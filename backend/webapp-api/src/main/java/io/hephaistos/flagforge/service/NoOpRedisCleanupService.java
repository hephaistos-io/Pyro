package io.hephaistos.flagforge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * No-op implementation of RedisCleanupService used when Redis is disabled. All cleanup operations
 * are no-ops since there's nothing to clean up.
 */
@Service
@ConditionalOnProperty(name = "flagforge.redis.enabled", havingValue = "false")
public class NoOpRedisCleanupService implements RedisCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpRedisCleanupService.class);

    public NoOpRedisCleanupService() {
        LOGGER.info("Redis cleanup service is DISABLED - no cleanup will be performed");
    }

    @Override
    public void cleanupEnvironmentKeys(UUID environmentId) {
        // No-op - Redis disabled
    }
}
