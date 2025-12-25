package io.hephaistos.flagforge.cache;

import io.hephaistos.flagforge.common.enums.TemplateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * No-op implementation of CacheInvalidationPublisher used when Redis is disabled. All publish
 * operations are no-ops.
 */
@Service
@ConditionalOnProperty(name = "flagforge.redis.enabled", havingValue = "false")
public class NoOpCacheInvalidationPublisher extends CacheInvalidationPublisher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(NoOpCacheInvalidationPublisher.class);

    public NoOpCacheInvalidationPublisher() {
        super(null, null);
        LOGGER.info(
                "Cache invalidation publisher is DISABLED - no invalidation events will be sent");
    }

    @Override
    public void publishSchemaChange(UUID appId, TemplateType type) {
        // No-op - Redis disabled
    }

    @Override
    public void publishOverrideChange(UUID appId, UUID envId, TemplateType type,
            String identifier) {
        // No-op - Redis disabled
    }

    @Override
    public void publishEnvironmentDeleted(UUID appId, UUID envId) {
        // No-op - Redis disabled
    }
}
