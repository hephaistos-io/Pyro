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
     * Finds an API key by its plaintext value.
     *
     * @param key the plaintext API key (64 hex characters)
     * @return the API key entity if found
     */
    Optional<ApiKeyEntity> findByKey(String key);
}
