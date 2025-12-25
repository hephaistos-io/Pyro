package io.hephaistos.flagforge.service;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultRedisCleanupServiceTest {

    @Mock
    private StatefulRedisConnection<String, String> redisConnection;

    @Mock
    private RedisCommands<String, String> redisCommands;

    private DefaultRedisCleanupService cleanupService;

    private UUID environmentId;

    @BeforeEach
    void setUp() {
        when(redisConnection.sync()).thenReturn(redisCommands);
        cleanupService = new DefaultRedisCleanupService(redisConnection);
        environmentId = UUID.randomUUID();
    }

    private void setupEmptyScanMock() {
        @SuppressWarnings("unchecked")
        KeyScanCursor<String> mockCursor = mock(KeyScanCursor.class);
        when(mockCursor.getKeys()).thenReturn(Collections.emptyList());
        when(mockCursor.isFinished()).thenReturn(true);
        when(redisCommands.scan(any(ScanCursor.class), any(ScanArgs.class))).thenReturn(mockCursor);
    }

    private void setupScanMockWithKeys(List<String> keys) {
        @SuppressWarnings("unchecked")
        KeyScanCursor<String> cursorWithKeys = mock(KeyScanCursor.class);
        when(cursorWithKeys.getKeys()).thenReturn(keys);
        when(cursorWithKeys.isFinished()).thenReturn(true);

        when(redisCommands.scan(any(ScanCursor.class), any(ScanArgs.class))).thenReturn(
                cursorWithKeys);
    }

    // ========== Helper Methods ==========


    @Nested
    class CleanupEnvironmentKeysTests {

        @Test
        void deletesRateLimitBucketDirectly() {
            // Rate limit bucket has no wildcards, so del is called directly
            when(redisCommands.del(anyString())).thenReturn(1L);
            setupEmptyScanMock();

            cleanupService.cleanupEnvironmentKeys(environmentId);

            String expectedKey = "rate-limit:env:" + environmentId;
            verify(redisCommands).del(expectedKey);
        }

        @Test
        void scansForWildcardPatterns() {
            when(redisCommands.del(anyString())).thenReturn(1L);
            setupEmptyScanMock();

            cleanupService.cleanupEnvironmentKeys(environmentId);

            // Verify scan was called for all wildcard patterns (5 patterns)
            verify(redisCommands, times(5)).scan(any(ScanCursor.class), any(ScanArgs.class));
        }

        @Test
        void cleansUpAllPatterns() {
            when(redisCommands.del(anyString())).thenReturn(1L);
            setupEmptyScanMock();

            cleanupService.cleanupEnvironmentKeys(environmentId);

            // Verify direct delete for rate-limit
            verify(redisCommands).del("rate-limit:env:" + environmentId);

            // Verify scan was called for wildcard patterns (5 patterns with wildcards)
            verify(redisCommands, times(5)).scan(any(ScanCursor.class), any(ScanArgs.class));
        }

        @Test
        void handlesRedisErrorGracefully() {
            when(redisCommands.del(anyString())).thenThrow(
                    new RuntimeException("Redis connection failed"));

            // Should not throw exception - fail-open behavior
            cleanupService.cleanupEnvironmentKeys(environmentId);
        }

        @Test
        void handlesEmptyScanResultsGracefully() {
            when(redisCommands.del(anyString())).thenReturn(0L);
            setupEmptyScanMock();

            // Should not throw exception even when no keys found
            cleanupService.cleanupEnvironmentKeys(environmentId);

            // Verify del was called for rate-limit
            verify(redisCommands).del("rate-limit:env:" + environmentId);
        }

        @Test
        void deletesKeysFoundByScan() {
            // Set up scan to return keys for all patterns
            List<String> keysToDelete = List.of("usage:daily:" + environmentId + ":2024-01-01",
                    "usage:daily:" + environmentId + ":2024-01-02");
            setupScanMockWithKeys(keysToDelete);
            when(redisCommands.del(any(String[].class))).thenReturn(2L);

            cleanupService.cleanupEnvironmentKeys(environmentId);

            // Verify batch delete was called when keys were found
            verify(redisCommands, org.mockito.Mockito.atLeast(1)).del(any(String[].class));
        }

        @Test
        void handlesNullDeleteResponse() {
            when(redisCommands.del(anyString())).thenReturn(null);
            setupEmptyScanMock();

            // Should not throw exception
            cleanupService.cleanupEnvironmentKeys(environmentId);
        }
    }


    @Nested
    class PatternTests {

        @Test
        void rateLimitPatternIsExact() {
            when(redisCommands.del(anyString())).thenReturn(0L);
            setupEmptyScanMock();

            cleanupService.cleanupEnvironmentKeys(environmentId);

            // Rate limit should be direct delete (no wildcard)
            verify(redisCommands).del("rate-limit:env:" + environmentId);
        }

        @Test
        void usagePatternsUseWildcards() {
            when(redisCommands.del(anyString())).thenReturn(0L);
            setupEmptyScanMock();

            cleanupService.cleanupEnvironmentKeys(environmentId);

            // Verify scan was called 5 times (for 5 wildcard patterns)
            // usage:monthly, usage:daily, usage:peak, usage:rejected, usage:second
            verify(redisCommands, times(5)).scan(any(ScanCursor.class), any(ScanArgs.class));
        }
    }
}
