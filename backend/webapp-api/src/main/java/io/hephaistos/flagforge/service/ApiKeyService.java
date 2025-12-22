package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.enums.KeyType;
import io.hephaistos.flagforge.controller.dto.ApiKeyResponse;

import java.util.UUID;

public interface ApiKeyService {

    void createApiKey(UUID applicationId, UUID environmentId, KeyType keyType);

    ApiKeyResponse getApiKeyByType(UUID applicationId, UUID environmentId, KeyType keyType);

    ApiKeyResponse regenerateKey(UUID applicationId, UUID environmentId, KeyType keyType);
}
