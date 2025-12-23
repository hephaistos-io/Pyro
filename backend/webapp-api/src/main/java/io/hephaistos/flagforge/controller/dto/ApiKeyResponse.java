package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.ApiKeyEntity;
import io.hephaistos.flagforge.common.enums.KeyType;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for API key operations.
 * <p>
 * The secretKey field is only populated when the key is first created or regenerated. After that,
 * the key is stored as a hash and cannot be retrieved - users must regenerate to get a new key.
 *
 * @param id             the unique identifier of the API key
 * @param environmentId  the environment this key belongs to
 * @param keyType        the type of key (READ or WRITE)
 * @param secretKey      the plaintext secret key (null if not available, only shown on
 *                       create/regenerate)
 * @param expirationDate when this key expires
 */
public record ApiKeyResponse(UUID id, UUID environmentId, KeyType keyType,
                             @Nullable String secretKey,
                             OffsetDateTime expirationDate) {

    /**
     * Creates a response from an entity without exposing the secret key.
     * Use this for GET operations where the key hash is stored but not retrievable.
     */
    public static ApiKeyResponse fromEntity(ApiKeyEntity entity) {
        return new ApiKeyResponse(entity.getId(), entity.getEnvironmentId(), entity.getKeyType(),
                null, entity.getExpirationDate());
    }

    /**
     * Creates a response with the plaintext secret key. Use this only for create/regenerate
     * operations where the user needs to see the key.
     */
    public static ApiKeyResponse fromEntityWithSecret(ApiKeyEntity entity, String plaintextKey) {
        return new ApiKeyResponse(entity.getId(), entity.getEnvironmentId(), entity.getKeyType(),
                plaintextKey, entity.getExpirationDate());
    }
}
