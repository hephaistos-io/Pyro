package io.hephaistos.flagforge.customerapi.cache;

import io.hephaistos.flagforge.common.cache.CacheInvalidationEvent;
import io.hephaistos.flagforge.customerapi.service.TemplateCacheService;
import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Subscribes to Redis pub/sub channel for cache invalidation messages. When a message is received,
 * it delegates to TemplateCacheService to invalidate matching cache entries.
 */
@Component
@ConditionalOnProperty(name = "flagforge.cache.enabled", havingValue = "true",
        matchIfMissing = true)
public class CacheInvalidationSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheInvalidationSubscriber.class);

    private final RedisClient redisClient;
    private final TemplateCacheService cacheService;
    private final JsonMapper jsonMapper;

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    public CacheInvalidationSubscriber(RedisClient redisClient, TemplateCacheService cacheService,
            JsonMapper jsonMapper) {
        this.redisClient = redisClient;
        this.cacheService = cacheService;
        this.jsonMapper = jsonMapper;
    }

    @PostConstruct
    public void subscribe() {
        try {
            pubSubConnection = redisClient.connectPubSub();

            pubSubConnection.addListener(new RedisPubSubAdapter<>() {
                @Override
                public void message(String channel, String message) {
                    handleMessage(channel, message);
                }
            });

            pubSubConnection.sync().subscribe(CacheInvalidationEvent.CHANNEL);
            LOGGER.info("Subscribed to cache invalidation channel: {}",
                    CacheInvalidationEvent.CHANNEL);
        }
        catch (Exception e) {
            LOGGER.error("Failed to subscribe to cache invalidation channel: {}", e.getMessage());
            // Fail-open: cache invalidation via pub/sub won't work, but TTL-based expiry
            // will eventually clear stale entries
        }
    }

    @PreDestroy
    public void unsubscribe() {
        if (pubSubConnection != null) {
            try {
                pubSubConnection.sync().unsubscribe(CacheInvalidationEvent.CHANNEL);
                pubSubConnection.close();
                LOGGER.info("Unsubscribed from cache invalidation channel");
            }
            catch (Exception e) {
                LOGGER.warn("Error during unsubscribe: {}", e.getMessage());
            }
        }
    }

    private void handleMessage(String channel, String message) {
        try {
            CacheInvalidationEvent event =
                    jsonMapper.readValue(message, CacheInvalidationEvent.class);
            LOGGER.debug("Received cache invalidation: channel={}, event={}", channel, event);
            cacheService.invalidate(event);
        }
        catch (Exception e) {
            LOGGER.warn("Failed to process cache invalidation message: {} - {}", message,
                    e.getMessage());
        }
    }
}
