package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.common.cache.CacheInvalidationEvent;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.customerapi.configuration.CacheProperties;
import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

/**
 * Redis-based implementation of TemplateCacheService. Uses JSON serialization for cache values and
 * SCAN for pattern-based invalidation.
 */
@Service
@ConditionalOnProperty(name = "flagforge.cache.enabled", havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class DefaultTemplateCacheService implements TemplateCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTemplateCacheService.class);
    private static final String CACHE_KEY_PREFIX = "template:cache:";

    private final RedisCommands<String, String> redisCommands;
    private final JsonMapper jsonMapper;
    private final CacheProperties cacheProperties;

    public DefaultTemplateCacheService(@Qualifier("cacheRedisConnection")
            StatefulRedisConnection<String, String> redisConnection, JsonMapper jsonMapper,
            CacheProperties cacheProperties) {
        this.redisCommands = redisConnection.sync();
        this.jsonMapper = jsonMapper;
        this.cacheProperties = cacheProperties;
        LOGGER.info("Template cache service initialized with TTL: {}s",
                cacheProperties.ttlSeconds());
    }

    @Override
    public Optional<MergedTemplateValuesResponse> get(UUID appId, UUID envId, TemplateType type,
            String identifier) {
        try {
            String key = buildKey(appId, envId, type, identifier);
            String json = redisCommands.get(key);

            if (json == null) {
                LOGGER.debug("Cache MISS: {} (envId={}, type={}, id={})", key, envId, type,
                        identifier);
                return Optional.empty();
            }

            LOGGER.debug("Cache HIT: {} (envId={}, type={}, id={})", key, envId, type, identifier);
            return Optional.of(jsonMapper.readValue(json, MergedTemplateValuesResponse.class));
        }
        catch (Exception e) {
            LOGGER.warn("Cache ERROR reading key: {} - falling back to database", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(UUID appId, UUID envId, TemplateType type, String identifier,
            MergedTemplateValuesResponse value) {
        try {
            String key = buildKey(appId, envId, type, identifier);
            String json = jsonMapper.writeValueAsString(value);

            // ALWAYS set TTL - critical for volatile-lru eviction policy
            redisCommands.setex(key, cacheProperties.ttlSeconds(), json);
            LOGGER.debug("Cache PUT: {} (ttl={}s)", key, cacheProperties.ttlSeconds());
        }
        catch (JacksonException e) {
            LOGGER.warn("Cache PUT failed - serialization error: {}", e.getMessage());
        }
        catch (Exception e) {
            LOGGER.warn("Cache PUT failed: {} - continuing without caching", e.getMessage());
        }
    }

    @Override
    public void invalidate(CacheInvalidationEvent event) {
        try {
            String pattern = buildInvalidationPattern(event);
            int deleted = deleteByPattern(pattern);
            LOGGER.info("Cache INVALIDATE: pattern={}, deleted={} keys, event={}", pattern, deleted,
                    event.type());
        }
        catch (Exception e) {
            LOGGER.warn("Cache INVALIDATE failed: {}", e.getMessage());
        }
    }

    /**
     * Build cache key for a template. Format: template:cache:{appId}:{envId}:{type}:{identifier}
     */
    private String buildKey(UUID appId, UUID envId, TemplateType type, String identifier) {
        return CACHE_KEY_PREFIX + appId + ":" + envId + ":" + type + ":" + (identifier != null ?
                identifier :
                "");
    }

    /**
     * Build invalidation pattern based on event type.
     */
    private String buildInvalidationPattern(CacheInvalidationEvent event) {
        String appId = event.appId().toString();

        return switch (event.type()) {
            case SCHEMA_CHANGE ->
                // Schema change affects all environments for this app and template type
                    CACHE_KEY_PREFIX + appId + ":*:" + event.templateType() + ":*";

            case OVERRIDE_CHANGE -> {
                String envId = event.envId() != null ? event.envId().toString() : "*";
                if (event.identifier() != null) {
                    // Specific override changed
                    yield CACHE_KEY_PREFIX + appId + ":" + envId + ":" + event.templateType() + ":" + event.identifier();
                }
                else if (event.templateType() == TemplateType.USER) {
                    // USER env defaults changed - affects all users in this environment
                    yield CACHE_KEY_PREFIX + appId + ":" + envId + ":USER:*";
                }
                else {
                    // SYSTEM with null identifier - invalidate all SYSTEM for this env
                    yield CACHE_KEY_PREFIX + appId + ":" + envId + ":SYSTEM:*";
                }
            }

            case USER_CHANGE -> {
                // Specific user override changed
                String envId = event.envId() != null ? event.envId().toString() : "*";
                String userId = event.identifier() != null ? event.identifier() : "*";
                yield CACHE_KEY_PREFIX + appId + ":" + envId + ":USER:" + userId;
            }
        };
    }

    /**
     * Delete all keys matching a pattern using SCAN (production-safe).
     */
    private int deleteByPattern(String pattern) {
        // If pattern contains no wildcards, delete directly
        if (!pattern.contains("*")) {
            Long deleted = redisCommands.del(pattern);
            return deleted != null ? deleted.intValue() : 0;
        }

        // Use SCAN for pattern matching (safe for production, non-blocking)
        int deleted = 0;
        ScanCursor cursor = ScanCursor.INITIAL;
        ScanArgs args = ScanArgs.Builder.matches(pattern).limit(100);

        do {
            KeyScanCursor<String> result = redisCommands.scan(cursor, args);
            if (!result.getKeys().isEmpty()) {
                String[] keys = result.getKeys().toArray(new String[0]);
                Long count = redisCommands.del(keys);
                deleted += count != null ? count.intValue() : 0;
            }
            cursor = result;
        } while (!cursor.isFinished());

        return deleted;
    }
}
