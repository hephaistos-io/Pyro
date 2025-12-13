package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.ApiKeyCreationResponse;
import io.hephaistos.flagforge.controller.dto.ApiKeyResponse;

import java.util.List;
import java.util.UUID;

public interface ApiKeyService {

    ApiKeyCreationResponse createApiKey(UUID applicationId, String name);

    List<ApiKeyResponse> getApiKeysForApplication(UUID applicationId);

    void revokeApiKey(UUID applicationId, UUID apiKeyId);
}
