package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.ApiKeyEntity;
import io.hephaistos.flagforge.common.enums.KeyType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for API key operations.
 *
 * @param id             the unique identifier of the API key
 * @param environmentId  the environment this key belongs to
 * @param keyType        the type of key (READ or WRITE)
 * @param secretKey      the plaintext secret key
 * @param expirationDate when this key expires
 */
public record ApiKeyResponse(UUID id, UUID environmentId, KeyType keyType, String secretKey,
                             OffsetDateTime expirationDate) {

    /**
     * Creates a response from an entity, including the secret key.
     */
    public static ApiKeyResponse fromEntity(ApiKeyEntity entity) {
        return new ApiKeyResponse(entity.getId(), entity.getEnvironmentId(), entity.getKeyType(),
                entity.getKey(), entity.getExpirationDate());
    }
}
