package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.data.ApiKeyEntity;
import io.hephaistos.flagforge.data.KeyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    @Query("SELECT a FROM ApiKeyEntity a WHERE a.applicationId = :applicationId " + "AND a.environmentId = :environmentId AND a.keyType = :keyType " + "AND a.expirationDate > :now ORDER BY a.expirationDate DESC LIMIT 1")
    Optional<ApiKeyEntity> findActiveByApplicationIdAndEnvironmentIdAndKeyType(UUID applicationId,
            UUID environmentId, KeyType keyType, OffsetDateTime now);

    boolean existsByKeyHash(String keyHash);
}
