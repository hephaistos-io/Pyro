package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.ApiKeyCreationResponse;
import io.hephaistos.flagforge.controller.dto.ApiKeyResponse;
import io.hephaistos.flagforge.data.KeyType;

import java.util.UUID;

public interface ApiKeyService {

    void createApiKey(UUID applicationId, UUID environmentId, KeyType keyType);

    ApiKeyResponse getApiKeyByType(UUID applicationId, UUID environmentId, KeyType keyType);

    ApiKeyCreationResponse regenerateKey(UUID applicationId, UUID apiKeyId);
}
