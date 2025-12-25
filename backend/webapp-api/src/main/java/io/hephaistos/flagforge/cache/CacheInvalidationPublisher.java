package io.hephaistos.flagforge.cache;

import io.hephaistos.flagforge.common.cache.CacheInvalidationEvent;
import io.hephaistos.flagforge.common.cache.CacheInvalidationType;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * Publishes cache invalidation events to Redis pub/sub channel. Used by webapp-api to notify
 * customer-api instances when template data changes.
 */
@Service
@ConditionalOnProperty(name = "flagforge.redis.enabled", havingValue = "true",
        matchIfMissing = true)
public class CacheInvalidationPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheInvalidationPublisher.class);

    private final StatefulRedisConnection<String, String> redisConnection;
    private final JsonMapper jsonMapper;

    public CacheInvalidationPublisher(StatefulRedisConnection<String, String> redisConnection,
            JsonMapper jsonMapper) {
        this.redisConnection = redisConnection;
        this.jsonMapper = jsonMapper;
        LOGGER.info("Cache invalidation publisher initialized");
    }

    /**
     * Publish a schema change event (affects all environments for the app + template type).
     */
    public void publishSchemaChange(UUID appId, TemplateType type) {
        var event =
                new CacheInvalidationEvent(CacheInvalidationType.SCHEMA_CHANGE, appId, null, type,
                        null);
        publish(event);
    }

    /**
     * Publish an override change event (affects specific identifier in environment).
     */
    public void publishOverrideChange(UUID appId, UUID envId, TemplateType type,
            String identifier) {
        var event = new CacheInvalidationEvent(CacheInvalidationType.OVERRIDE_CHANGE, appId, envId,
                type, identifier);
        publish(event);
    }

    /**
     * Publish events for environment deletion (invalidates all caches for that environment).
     */
    public void publishEnvironmentDeleted(UUID appId, UUID envId) {
        // Invalidate both SYSTEM and USER caches for this environment
        var systemEvent =
                new CacheInvalidationEvent(CacheInvalidationType.OVERRIDE_CHANGE, appId, envId,
                        TemplateType.SYSTEM, null);
        var userEvent =
                new CacheInvalidationEvent(CacheInvalidationType.OVERRIDE_CHANGE, appId, envId,
                        TemplateType.USER, null);
        publish(systemEvent);
        publish(userEvent);
    }

    private void publish(CacheInvalidationEvent event) {
        try {
            String message = jsonMapper.writeValueAsString(event);
            Long subscribers =
                    redisConnection.sync().publish(CacheInvalidationEvent.CHANNEL, message);
            LOGGER.debug("Published cache invalidation: {} to {} subscribers", event, subscribers);
        }
        catch (JacksonException e) {
            LOGGER.warn("Failed to serialize cache invalidation event: {}", e.getMessage());
        }
        catch (Exception e) {
            LOGGER.warn("Failed to publish cache invalidation: {}", e.getMessage());
            // Fail-open: cache will eventually expire via TTL
        }
    }
}
