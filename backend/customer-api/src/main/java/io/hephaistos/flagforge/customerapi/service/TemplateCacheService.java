package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.common.cache.CacheInvalidationEvent;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for caching template responses in Redis. Provides cache lookups, storage, and
 * invalidation.
 */
public interface TemplateCacheService {

    /**
     * Get a cached template response.
     *
     * @param appId      Application UUID
     * @param envId      Environment UUID
     * @param type       Template type (SYSTEM or USER)
     * @param identifier For SYSTEM: the identifier; for USER: the userId
     * @return Cached response if present, empty otherwise
     */
    Optional<MergedTemplateValuesResponse> get(UUID appId, UUID envId, TemplateType type,
            String identifier);

    /**
     * Cache a template response.
     *
     * @param appId      Application UUID
     * @param envId      Environment UUID
     * @param type       Template type (SYSTEM or USER)
     * @param identifier For SYSTEM: the identifier; for USER: the userId
     * @param value      The response to cache
     */
    void put(UUID appId, UUID envId, TemplateType type, String identifier,
            MergedTemplateValuesResponse value);

    /**
     * Invalidate cache entries based on an invalidation event.
     *
     * @param event The invalidation event describing what to invalidate
     */
    void invalidate(CacheInvalidationEvent event);
}
