package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.data.ApiKeyEntity;
import io.hephaistos.flagforge.data.KeyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    Optional<ApiKeyEntity> findByApplicationIdAndEnvironmentIdAndKeyType(UUID applicationId,
            UUID environmentId, KeyType keyType);

    boolean existsByKeyHash(String keyHash);
}
