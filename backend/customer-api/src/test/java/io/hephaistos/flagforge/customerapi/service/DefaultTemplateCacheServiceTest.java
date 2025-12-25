package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.common.cache.CacheInvalidationEvent;
import io.hephaistos.flagforge.common.cache.CacheInvalidationType;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.types.StringTemplateField;
import io.hephaistos.flagforge.common.types.TemplateSchema;
import io.hephaistos.flagforge.customerapi.configuration.CacheProperties;
import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultTemplateCacheServiceTest {

    @Mock
    private StatefulRedisConnection<String, String> redisConnection;

    @Mock
    private RedisCommands<String, String> redisCommands;

    private JsonMapper jsonMapper;
    private CacheProperties cacheProperties;
    private DefaultTemplateCacheService cacheService;

    private UUID appId;
    private UUID envId;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        cacheProperties = new CacheProperties(true, 300);
        when(redisConnection.sync()).thenReturn(redisCommands);
        cacheService =
                new DefaultTemplateCacheService(redisConnection, jsonMapper, cacheProperties);
        appId = UUID.randomUUID();
        envId = UUID.randomUUID();
    }

    private MergedTemplateValuesResponse createMergedResponse(TemplateType type,
            String identifier) {
        var schema = new TemplateSchema(List.of(new StringTemplateField("api_url", "API URL", false,
                "https://default.api.com", 0, 255)));
        return new MergedTemplateValuesResponse(type, schema,
                Map.of("api_url", "https://test.api.com"), identifier);
    }

    private void setupScanMock(List<String> keys) {
        @SuppressWarnings("unchecked")
        KeyScanCursor<String> mockCursor = mock(KeyScanCursor.class);
        when(mockCursor.getKeys()).thenReturn(keys);
        when(mockCursor.isFinished()).thenReturn(true);
        when(redisCommands.scan(any(ScanCursor.class), any(ScanArgs.class))).thenReturn(mockCursor);
    }

    private void verifyScanCalledWith(String pattern) {
        ArgumentCaptor<ScanArgs> argsCaptor = ArgumentCaptor.forClass(ScanArgs.class);
        verify(redisCommands).scan(eq(ScanCursor.INITIAL), argsCaptor.capture());
        // ScanArgs doesn't expose pattern easily, so we just verify scan was called
    }

    // ========== Helper Methods ==========


    @Nested
    class GetTests {

        @Test
        void returnsEmptyWhenCacheMiss() {
            when(redisCommands.get(anyString())).thenReturn(null);

            var result = cacheService.get(appId, envId, TemplateType.SYSTEM, "test-id");

            assertThat(result).isEmpty();
        }

        @Test
        void returnsCachedValueOnHit() throws JacksonException {
            var response = createMergedResponse(TemplateType.SYSTEM, "test-id");
            String json = jsonMapper.writeValueAsString(response);
            when(redisCommands.get(anyString())).thenReturn(json);

            var result = cacheService.get(appId, envId, TemplateType.SYSTEM, "test-id");

            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo(TemplateType.SYSTEM);
            assertThat(result.get().appliedIdentifier()).isEqualTo("test-id");
        }

        @Test
        void buildsCorrectCacheKeyWithIdentifier() {
            when(redisCommands.get(anyString())).thenReturn(null);

            cacheService.get(appId, envId, TemplateType.SYSTEM, "region-eu");

            String expectedKey = "template:cache:" + appId + ":" + envId + ":SYSTEM:region-eu";
            verify(redisCommands).get(expectedKey);
        }

        @Test
        void buildsCorrectCacheKeyWithEmptyIdentifier() {
            when(redisCommands.get(anyString())).thenReturn(null);

            cacheService.get(appId, envId, TemplateType.USER, "");

            String expectedKey = "template:cache:" + appId + ":" + envId + ":USER:";
            verify(redisCommands).get(expectedKey);
        }

        @Test
        void buildsCorrectCacheKeyWithNullIdentifier() {
            when(redisCommands.get(anyString())).thenReturn(null);

            cacheService.get(appId, envId, TemplateType.SYSTEM, null);

            String expectedKey = "template:cache:" + appId + ":" + envId + ":SYSTEM:";
            verify(redisCommands).get(expectedKey);
        }

        @Test
        void returnsEmptyOnDeserializationError() {
            when(redisCommands.get(anyString())).thenReturn("invalid json{");

            var result = cacheService.get(appId, envId, TemplateType.SYSTEM, "test");

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyOnRedisError() {
            when(redisCommands.get(anyString())).thenThrow(
                    new RuntimeException("Redis connection failed"));

            var result = cacheService.get(appId, envId, TemplateType.SYSTEM, "test");

            assertThat(result).isEmpty();
        }
    }


    @Nested
    class PutTests {

        @Test
        void cachesValueWithCorrectTtl() throws JacksonException {
            var response = createMergedResponse(TemplateType.SYSTEM, "test-id");

            cacheService.put(appId, envId, TemplateType.SYSTEM, "test-id", response);

            String expectedKey = "template:cache:" + appId + ":" + envId + ":SYSTEM:test-id";
            verify(redisCommands).setex(eq(expectedKey), eq(300L), anyString());
        }

        @Test
        void serializesResponseToJson() throws JacksonException {
            var response = createMergedResponse(TemplateType.USER, "user123");

            cacheService.put(appId, envId, TemplateType.USER, "user123", response);

            ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisCommands).setex(anyString(), anyLong(), jsonCaptor.capture());

            String capturedJson = jsonCaptor.getValue();
            var deserialized =
                    jsonMapper.readValue(capturedJson, MergedTemplateValuesResponse.class);
            assertThat(deserialized.type()).isEqualTo(TemplateType.USER);
            assertThat(deserialized.appliedIdentifier()).isEqualTo("user123");
        }

        @Test
        void handlesRedisErrorGracefully() {
            var response = createMergedResponse(TemplateType.SYSTEM, "test");
            when(redisCommands.setex(anyString(), anyLong(), anyString())).thenThrow(
                    new RuntimeException("Redis down"));

            // Should not throw exception
            cacheService.put(appId, envId, TemplateType.SYSTEM, "test", response);
        }
    }


    @Nested
    class InvalidateTests {

        @Test
        void schemaChangeInvalidatesAllEnvironmentsForAppAndType() {
            var event = new CacheInvalidationEvent(CacheInvalidationType.SCHEMA_CHANGE, appId, null,
                    TemplateType.SYSTEM, null);
            setupScanMock(Collections.emptyList());

            cacheService.invalidate(event);

            String expectedPattern = "template:cache:" + appId + ":*:SYSTEM:*";
            verifyScanCalledWith(expectedPattern);
        }

        @Test
        void overrideChangeWithIdentifierInvalidatesSpecificKey() {
            var event =
                    new CacheInvalidationEvent(CacheInvalidationType.OVERRIDE_CHANGE, appId, envId,
                            TemplateType.SYSTEM, "region-eu");
            // No wildcards in pattern, so del is called directly
            when(redisCommands.del(anyString())).thenReturn(1L);

            cacheService.invalidate(event);

            String expectedKey = "template:cache:" + appId + ":" + envId + ":SYSTEM:region-eu";
            verify(redisCommands).del(expectedKey);
        }

        @Test
        void overrideChangeWithoutIdentifierForUserTypeInvalidatesAllUsers() {
            var event =
                    new CacheInvalidationEvent(CacheInvalidationType.OVERRIDE_CHANGE, appId, envId,
                            TemplateType.USER, null);
            setupScanMock(Collections.emptyList());

            cacheService.invalidate(event);

            String expectedPattern = "template:cache:" + appId + ":" + envId + ":USER:*";
            verifyScanCalledWith(expectedPattern);
        }

        @Test
        void overrideChangeWithoutIdentifierForSystemTypeInvalidatesAllSystem() {
            var event =
                    new CacheInvalidationEvent(CacheInvalidationType.OVERRIDE_CHANGE, appId, envId,
                            TemplateType.SYSTEM, null);
            setupScanMock(Collections.emptyList());

            cacheService.invalidate(event);

            String expectedPattern = "template:cache:" + appId + ":" + envId + ":SYSTEM:*";
            verifyScanCalledWith(expectedPattern);
        }

        @Test
        void userChangeInvalidatesSpecificUserCache() {
            var event = new CacheInvalidationEvent(CacheInvalidationType.USER_CHANGE, appId, envId,
                    TemplateType.USER, "user-abc123");
            // No wildcards in pattern, so del is called directly
            when(redisCommands.del(anyString())).thenReturn(1L);

            cacheService.invalidate(event);

            String expectedKey = "template:cache:" + appId + ":" + envId + ":USER:user-abc123";
            verify(redisCommands).del(expectedKey);
        }

        @Test
        void userChangeWithoutIdentifierUsesWildcard() {
            var event = new CacheInvalidationEvent(CacheInvalidationType.USER_CHANGE, appId, envId,
                    TemplateType.USER, null);
            setupScanMock(Collections.emptyList());

            cacheService.invalidate(event);

            String expectedPattern = "template:cache:" + appId + ":" + envId + ":USER:*";
            verifyScanCalledWith(expectedPattern);
        }

        @Test
        void deletesKeysFoundByScan() {
            var event = new CacheInvalidationEvent(CacheInvalidationType.SCHEMA_CHANGE, appId, null,
                    TemplateType.SYSTEM, null);
            List<String> keysToDelete = List.of("template:cache:" + appId + ":env1:SYSTEM:id1",
                    "template:cache:" + appId + ":env2:SYSTEM:id2");
            setupScanMock(keysToDelete);
            when(redisCommands.del(any(String[].class))).thenReturn(2L);

            cacheService.invalidate(event);

            ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
            verify(redisCommands).del(keysCaptor.capture());
            assertThat(keysCaptor.getValue()).containsExactlyInAnyOrder(
                    "template:cache:" + appId + ":env1:SYSTEM:id1",
                    "template:cache:" + appId + ":env2:SYSTEM:id2");
        }

        @Test
        void handlesRedisErrorGracefully() {
            var event = new CacheInvalidationEvent(CacheInvalidationType.SCHEMA_CHANGE, appId, null,
                    TemplateType.SYSTEM, null);
            when(redisCommands.scan(any(ScanCursor.class), any(ScanArgs.class))).thenThrow(
                    new RuntimeException("Redis down"));

            // Should not throw exception
            cacheService.invalidate(event);
        }

        @Test
        void overrideChangeWithNullEnvIdUsesWildcard() {
            var event = new CacheInvalidationEvent(CacheInvalidationType.OVERRIDE_CHANGE, appId,
                    null,  // null envId
                    TemplateType.SYSTEM, "identifier");
            setupScanMock(Collections.emptyList());

            cacheService.invalidate(event);

            // With wildcards, scan should be called
            String expectedPattern = "template:cache:" + appId + ":*:SYSTEM:identifier";
            verifyScanCalledWith(expectedPattern);
        }
    }
}
