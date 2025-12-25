package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.common.cache.CacheInvalidationEvent;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * No-op implementation of TemplateCacheService used when caching is disabled. All operations are
 * no-ops that bypass the cache.
 */
@Service
@ConditionalOnProperty(name = "flagforge.cache.enabled", havingValue = "false")
public class NoOpTemplateCacheService implements TemplateCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpTemplateCacheService.class);

    public NoOpTemplateCacheService() {
        LOGGER.info("Template cache is DISABLED - all requests will hit the database");
    }

    @Override
    public Optional<MergedTemplateValuesResponse> get(UUID appId, UUID envId, TemplateType type,
            String identifier) {
        // Always return empty - cache disabled
        return Optional.empty();
    }

    @Override
    public void put(UUID appId, UUID envId, TemplateType type, String identifier,
            MergedTemplateValuesResponse value) {
        // No-op - cache disabled
    }

    @Override
    public void invalidate(CacheInvalidationEvent event) {
        // No-op - cache disabled
    }
}
