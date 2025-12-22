package io.hephaistos.flagforge.customerapi.data.repository;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for application entities in the customer-api. Used to retrieve company information for
 * API key authentication.
 */
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {
    // Uses inherited findById() method to retrieve applications
}
