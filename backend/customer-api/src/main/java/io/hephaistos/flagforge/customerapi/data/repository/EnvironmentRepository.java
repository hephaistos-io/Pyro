package io.hephaistos.flagforge.customerapi.data.repository;

import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for environment entities in the customer-api. Used to retrieve environment
 * configuration including rate limits.
 */
public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, UUID> {
    // Uses inherited findById() method to retrieve environments
}
