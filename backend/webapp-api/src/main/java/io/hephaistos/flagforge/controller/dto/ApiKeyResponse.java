package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.ApiKeyEntity;
import io.hephaistos.flagforge.common.enums.KeyType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiKeyResponse(UUID id, UUID environmentId, KeyType keyType, String secretKey,
                             OffsetDateTime expirationDate) {
    public static ApiKeyResponse fromEntity(ApiKeyEntity entity) {
        return new ApiKeyResponse(entity.getId(), entity.getEnvironmentId(), entity.getKeyType(),
                entity.getKey(), entity.getExpirationDate());
    }
}
