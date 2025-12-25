package io.hephaistos.flagforge.common.cache;

import io.hephaistos.flagforge.common.enums.TemplateType;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Event published to Redis pub/sub channel when template data changes. Used to invalidate cached
 * template responses in customer-api.
 *
 * @param type         Type of change that occurred
 * @param appId        Application UUID
 * @param envId        Environment UUID (null for SCHEMA_CHANGE which affects all environments)
 * @param templateType Whether this affects SYSTEM or USER template
 * @param identifier   For SYSTEM: the identifier; for USER: the userId; null for wildcard
 *                     invalidation
 */
public record CacheInvalidationEvent(CacheInvalidationType type, UUID appId, @Nullable UUID envId,
                                     TemplateType templateType, @Nullable String identifier) {

    /**
     * Redis pub/sub channel for cache invalidation messages.
     */
    public static final String CHANNEL = "template:invalidate";
}
