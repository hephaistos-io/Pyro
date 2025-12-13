package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.data.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    List<ApiKeyEntity> findByApplicationId(UUID applicationId);

    boolean existsByKeyHash(String keyHash);
}
