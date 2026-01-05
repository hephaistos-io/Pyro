package io.hephaistos.flagforge.service;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Cleans up Redis keys when environments are deleted. Uses SCAN for pattern matching
 * (production-safe, non-blocking). Fails open: cleanup errors don't block environment deletion.
 */
@Service
@ConditionalOnProperty(name = "flagforge.redis.enabled", havingValue = "true")
public class DefaultRedisCleanupService implements RedisCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRedisCleanupService.class);
    private static final int SCAN_BATCH_SIZE = 100;

    private final StatefulRedisConnection<String, String> redisConnection;

    public DefaultRedisCleanupService(StatefulRedisConnection<String, String> redisConnection) {
        this.redisConnection = redisConnection;
        LOGGER.info("Redis cleanup service initialized");
    }

    @Override
    public void cleanupEnvironmentKeys(UUID environmentId) {
        try {
            var commands = redisConnection.sync();
            String envId = environmentId.toString();

            // All key patterns associated with an environment
            List<String> patterns =
                    List.of("rate-limit:env:" + envId, "usage:monthly:" + envId + ":*",
                            "usage:daily:" + envId + ":*", "usage:peak:" + envId + ":*",
                            "usage:rejected:" + envId + ":*", "usage:second:" + envId + ":*");

            int totalDeleted = 0;
            for (String pattern : patterns) {
                int deleted = deleteByPattern(commands, pattern);
                totalDeleted += deleted;
            }

            LOGGER.info("Cleaned up {} Redis keys for deleted environment: {}", totalDeleted,
                    envId);
        }
        catch (Exception e) {
            // Fail-open: log warning but don't block environment deletion
            LOGGER.warn("Failed to cleanup Redis keys for environment {}: {}", environmentId,
                    e.getMessage());
        }
    }

    /**
     * Delete keys matching a pattern using SCAN (non-blocking, production-safe). Handles both exact
     * keys and wildcard patterns.
     *
     * @param commands Redis commands interface
     * @param pattern  Key pattern (may include wildcards)
     * @return Number of keys deleted
     */
    private int deleteByPattern(RedisCommands<String, String> commands, String pattern) {
        // For patterns without wildcards, try direct delete first
        if (!pattern.contains("*")) {
            Long deleted = commands.del(pattern);
            return deleted != null ? deleted.intValue() : 0;
        }

        // For wildcard patterns, use SCAN
        int deleted = 0;
        ScanCursor cursor = ScanCursor.INITIAL;
        ScanArgs args = ScanArgs.Builder.matches(pattern).limit(SCAN_BATCH_SIZE);

        do {
            KeyScanCursor<String> result = commands.scan(cursor, args);
            if (!result.getKeys().isEmpty()) {
                Long count = commands.del(result.getKeys().toArray(new String[0]));
                if (count != null) {
                    deleted += count.intValue();
                }
            }
            cursor = result;
        } while (!cursor.isFinished());

        return deleted;
    }
}
