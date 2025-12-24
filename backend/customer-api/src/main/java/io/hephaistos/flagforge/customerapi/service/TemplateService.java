package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Service for retrieving and updating template values in the customer API.
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

    /**
     * Get merged USER template values for a specific user. Applies 3-layer merge:
     * <ol>
     * <li>Schema defaults from USER template</li>
     * <li>Environment-level defaults (identifier = "")</li>
     * <li>User-specific overrides</li>
     * </ol>
     *
     * @param applicationId The application ID (from API key)
     * @param environmentId The environment ID (from API key)
     * @param userId        The user identifier string
     * @return Merged template values for the user
     */
    MergedTemplateValuesResponse getMergedUserValues(UUID applicationId, UUID environmentId,
            String userId);

    /**
     * Set USER template overrides for a specific user. Creates or updates the user's override
     * values.
     *
     * @param applicationId The application ID (from API key)
     * @param environmentId The environment ID (from API key)
     * @param userId        The user identifier string
     * @param values        The override values to set
     */
    void setUserValues(UUID applicationId, UUID environmentId, String userId,
            Map<String, Object> values);
}
