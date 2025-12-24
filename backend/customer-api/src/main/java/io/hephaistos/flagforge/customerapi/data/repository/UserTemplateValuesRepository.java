package io.hephaistos.flagforge.customerapi.data.repository;

import io.hephaistos.flagforge.common.data.UserTemplateValuesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserTemplateValuesEntity in customer-api. Provides access to per-user template
 * override values.
 */
@Repository
public interface UserTemplateValuesRepository
        extends JpaRepository<UserTemplateValuesEntity, UUID> {

    Optional<UserTemplateValuesEntity> findByApplicationIdAndEnvironmentIdAndUserId(
            UUID applicationId, UUID environmentId, UUID userId);
}
