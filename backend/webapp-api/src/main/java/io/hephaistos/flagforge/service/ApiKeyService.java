package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.enums.KeyType;
import io.hephaistos.flagforge.controller.dto.ApiKeyResponse;

import java.util.UUID;

public interface ApiKeyService {

    /**
     * Creates a new API key for the given application and environment. The key is hashed before
     * storage and cannot be retrieved later.
     *
     * @return the API key response including the plaintext secret key (only time it's visible)
     */
    ApiKeyResponse createApiKey(UUID applicationId, UUID environmentId, KeyType keyType);

    /**
     * Gets API key metadata by type. The secret key is NOT returned as it is stored hashed. To get
     * a new usable key, use {@link #regenerateKey}.
     */
    ApiKeyResponse getApiKeyByType(UUID applicationId, UUID environmentId, KeyType keyType);

    /**
     * Regenerates an API key, creating a new key and setting the old one to expire in 1 week.
     *
     * @return the API key response including the new plaintext secret key (only time it's visible)
     */
    ApiKeyResponse regenerateKey(UUID applicationId, UUID environmentId, KeyType keyType);
}
