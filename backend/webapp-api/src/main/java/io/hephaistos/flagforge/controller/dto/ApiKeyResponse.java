package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.ApiKeyEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiKeyResponse(UUID id, String keyPrefix, String name, OffsetDateTime createdAt,
                             OffsetDateTime lastUsedAt, boolean isActive,
                             int rateLimitRequestsPerMinute) {
    public static ApiKeyResponse fromEntity(ApiKeyEntity entity) {
        return new ApiKeyResponse(entity.getId(), entity.getKeyPrefix(), entity.getName(),
                entity.getCreatedAt(), entity.getLastUsedAt(),
                Boolean.TRUE.equals(entity.getIsActive()),
                entity.getRateLimitRequestsPerMinute() != null ?
                        entity.getRateLimitRequestsPerMinute() :
                        1000);
    }
}
