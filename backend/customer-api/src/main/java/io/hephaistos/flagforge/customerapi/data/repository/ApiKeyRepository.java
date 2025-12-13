package io.hephaistos.flagforge.customerapi.data.repository;

import io.hephaistos.flagforge.customerapi.data.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    Optional<ApiKeyEntity> findByKeyHash(String keyHash);

    @Modifying
    @Query("UPDATE ApiKeyEntity a SET a.lastUsedAt = :lastUsedAt WHERE a.id = :id")
    void updateLastUsedAt(UUID id, OffsetDateTime lastUsedAt);
}
