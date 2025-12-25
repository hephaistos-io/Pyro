package io.hephaistos.flagforge.cache;

import io.hephaistos.flagforge.common.cache.CacheInvalidationEvent;
import io.hephaistos.flagforge.common.cache.CacheInvalidationType;
import io.hephaistos.flagforge.common.enums.TemplateType;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CacheInvalidationPublisherTest {

    @Mock
    private StatefulRedisConnection<String, String> redisConnection;

    @Mock
    private RedisCommands<String, String> redisCommands;

    private JsonMapper jsonMapper;
    private CacheInvalidationPublisher publisher;

    private UUID appId;
    private UUID envId;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        when(redisConnection.sync()).thenReturn(redisCommands);
        publisher = new CacheInvalidationPublisher(redisConnection, jsonMapper);
        appId = UUID.randomUUID();
        envId = UUID.randomUUID();
    }

    @Nested
    class PublishSchemaChangeTests {

        @Test
        void publishesSchemaChangeEventForSystemType() throws JacksonException {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishSchemaChange(appId, TemplateType.SYSTEM);

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisCommands).publish(eq(CacheInvalidationEvent.CHANNEL),
                    messageCaptor.capture());

            CacheInvalidationEvent event =
                    jsonMapper.readValue(messageCaptor.getValue(), CacheInvalidationEvent.class);
            assertThat(event.type()).isEqualTo(CacheInvalidationType.SCHEMA_CHANGE);
            assertThat(event.appId()).isEqualTo(appId);
            assertThat(event.envId()).isNull();
            assertThat(event.templateType()).isEqualTo(TemplateType.SYSTEM);
            assertThat(event.identifier()).isNull();
        }

        @Test
        void publishesSchemaChangeEventForUserType() throws JacksonException {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishSchemaChange(appId, TemplateType.USER);

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisCommands).publish(eq(CacheInvalidationEvent.CHANNEL),
                    messageCaptor.capture());

            CacheInvalidationEvent event =
                    jsonMapper.readValue(messageCaptor.getValue(), CacheInvalidationEvent.class);
            assertThat(event.templateType()).isEqualTo(TemplateType.USER);
        }

        @Test
        void publishesToCorrectChannel() {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishSchemaChange(appId, TemplateType.SYSTEM);

            verify(redisCommands).publish(eq("template:invalidate"), anyString());
        }

        @Test
        void handlesRedisErrorGracefully() {
            when(redisCommands.publish(anyString(), anyString())).thenThrow(
                    new RuntimeException("Redis connection failed"));

            // Should not throw exception - fail-open behavior
            publisher.publishSchemaChange(appId, TemplateType.SYSTEM);
        }
    }


    @Nested
    class PublishOverrideChangeTests {

        @Test
        void publishesOverrideChangeEventWithIdentifier() throws JacksonException {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishOverrideChange(appId, envId, TemplateType.SYSTEM, "region-eu");

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisCommands).publish(eq(CacheInvalidationEvent.CHANNEL),
                    messageCaptor.capture());

            CacheInvalidationEvent event =
                    jsonMapper.readValue(messageCaptor.getValue(), CacheInvalidationEvent.class);
            assertThat(event.type()).isEqualTo(CacheInvalidationType.OVERRIDE_CHANGE);
            assertThat(event.appId()).isEqualTo(appId);
            assertThat(event.envId()).isEqualTo(envId);
            assertThat(event.templateType()).isEqualTo(TemplateType.SYSTEM);
            assertThat(event.identifier()).isEqualTo("region-eu");
        }

        @Test
        void publishesOverrideChangeEventForUserTypeWithUserId() throws JacksonException {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishOverrideChange(appId, envId, TemplateType.USER, "user-abc123");

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisCommands).publish(eq(CacheInvalidationEvent.CHANNEL),
                    messageCaptor.capture());

            CacheInvalidationEvent event =
                    jsonMapper.readValue(messageCaptor.getValue(), CacheInvalidationEvent.class);
            assertThat(event.type()).isEqualTo(CacheInvalidationType.OVERRIDE_CHANGE);
            assertThat(event.templateType()).isEqualTo(TemplateType.USER);
            assertThat(event.identifier()).isEqualTo("user-abc123");
        }

        @Test
        void publishesOverrideChangeEventWithNullIdentifier() throws JacksonException {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishOverrideChange(appId, envId, TemplateType.USER, null);

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisCommands).publish(eq(CacheInvalidationEvent.CHANNEL),
                    messageCaptor.capture());

            CacheInvalidationEvent event =
                    jsonMapper.readValue(messageCaptor.getValue(), CacheInvalidationEvent.class);
            assertThat(event.identifier()).isNull();
        }

        @Test
        void handlesRedisErrorGracefully() {
            when(redisCommands.publish(anyString(), anyString())).thenThrow(
                    new RuntimeException("Redis connection failed"));

            // Should not throw exception - fail-open behavior
            publisher.publishOverrideChange(appId, envId, TemplateType.SYSTEM, "test");
        }
    }


    @Nested
    class PublishEnvironmentDeletedTests {

        @Test
        void publishesBothSystemAndUserEvents() {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishEnvironmentDeleted(appId, envId);

            // Should publish two events - one for SYSTEM and one for USER
            verify(redisCommands, times(2)).publish(eq(CacheInvalidationEvent.CHANNEL),
                    anyString());
        }

        @Test
        void publishesSystemEventForEnvironmentDeletion() throws JacksonException {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishEnvironmentDeleted(appId, envId);

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisCommands, times(2)).publish(eq(CacheInvalidationEvent.CHANNEL),
                    messageCaptor.capture());

            // First event should be SYSTEM
            CacheInvalidationEvent systemEvent =
                    jsonMapper.readValue(messageCaptor.getAllValues().get(0),
                            CacheInvalidationEvent.class);
            assertThat(systemEvent.type()).isEqualTo(CacheInvalidationType.OVERRIDE_CHANGE);
            assertThat(systemEvent.appId()).isEqualTo(appId);
            assertThat(systemEvent.envId()).isEqualTo(envId);
            assertThat(systemEvent.templateType()).isEqualTo(TemplateType.SYSTEM);
            assertThat(systemEvent.identifier()).isNull();
        }

        @Test
        void publishesUserEventForEnvironmentDeletion() throws JacksonException {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishEnvironmentDeleted(appId, envId);

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisCommands, times(2)).publish(eq(CacheInvalidationEvent.CHANNEL),
                    messageCaptor.capture());

            // Second event should be USER
            CacheInvalidationEvent userEvent =
                    jsonMapper.readValue(messageCaptor.getAllValues().get(1),
                            CacheInvalidationEvent.class);
            assertThat(userEvent.type()).isEqualTo(CacheInvalidationType.OVERRIDE_CHANGE);
            assertThat(userEvent.appId()).isEqualTo(appId);
            assertThat(userEvent.envId()).isEqualTo(envId);
            assertThat(userEvent.templateType()).isEqualTo(TemplateType.USER);
            assertThat(userEvent.identifier()).isNull();
        }

        @Test
        void handlesRedisErrorGracefullyForFirstEvent() {
            when(redisCommands.publish(anyString(), anyString())).thenThrow(
                    new RuntimeException("Redis connection failed"));

            // Should not throw exception - fail-open behavior
            // Note: Second event won't be published due to exception in first
            publisher.publishEnvironmentDeleted(appId, envId);
        }

        @Test
        void handlesRedisErrorGracefullyForSecondEvent() {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L)  // First succeeds
                    .thenThrow(new RuntimeException("Redis connection failed"));  // Second fails

            // Should not throw exception - fail-open behavior
            publisher.publishEnvironmentDeleted(appId, envId);

            // Verify first event was still published
            verify(redisCommands, times(2)).publish(eq(CacheInvalidationEvent.CHANNEL),
                    anyString());
        }
    }


    @Nested
    class MessageSerializationTests {

        @Test
        void serializesEventToValidJson() {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishSchemaChange(appId, TemplateType.SYSTEM);

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisCommands).publish(anyString(), messageCaptor.capture());

            String json = messageCaptor.getValue();
            assertThat(json).contains("\"type\":");
            assertThat(json).contains("\"appId\":");
            assertThat(json).contains("\"templateType\":");
        }

        @Test
        void includesCorrectAppIdInJson() {
            when(redisCommands.publish(anyString(), anyString())).thenReturn(1L);

            publisher.publishSchemaChange(appId, TemplateType.SYSTEM);

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisCommands).publish(anyString(), messageCaptor.capture());

            assertThat(messageCaptor.getValue()).contains(appId.toString());
        }
    }
}
