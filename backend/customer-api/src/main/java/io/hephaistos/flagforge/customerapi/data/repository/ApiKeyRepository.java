package io.hephaistos.flagforge.customerapi.data.repository;

import io.hephaistos.flagforge.common.data.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for API key entities in the customer-api. Used for API key authentication and
 * validation.
 */
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    /**
     * Finds an API key by its hashed key value.
     * The caller must hash the plaintext key using {@link io.hephaistos.flagforge.common.util.ApiKeyHasher}
     * before calling this method.
     *
     * @param keyHash the SHA-256 hash of the API key
     * @return the API key entity if found
     */
    Optional<ApiKeyEntity> findByKey(String keyHash);
}
