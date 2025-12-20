package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.ApiKeyEntity;
import io.hephaistos.flagforge.data.KeyType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiKeyResponse(UUID id, UUID environmentId, int rateLimitRequestsPerMinute,
                             KeyType keyType, String secretKey, OffsetDateTime expirationDate) {
    public static ApiKeyResponse fromEntity(ApiKeyEntity entity) {
        return new ApiKeyResponse(entity.getId(), entity.getEnvironmentId(),
                entity.getRateLimitRequestsPerMinute() != null ?
                        entity.getRateLimitRequestsPerMinute() : 1000, entity.getKeyType(),
                entity.getKey(), entity.getExpirationDate());
    }
}
