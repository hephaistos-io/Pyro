package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Service for retrieving template values in the customer API.
 */
public interface TemplateService {

    /**
     * Get merged SYSTEM template values for the given application and environment. Starts with
     * schema defaults and optionally applies a single identifier override.
     *
     * @param applicationId The application ID (from API key)
     * @param environmentId The environment ID (from API key)
     * @param identifier    Optional identifier to apply override for
     * @return Merged template values with applied identifier (if found)
     */
    MergedTemplateValuesResponse getMergedSystemValues(UUID applicationId, UUID environmentId,
            @Nullable String identifier);
}
