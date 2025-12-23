package io.hephaistos.flagforge.customerapi.data.repository;

import io.hephaistos.flagforge.common.data.TemplateValuesEntity;
import io.hephaistos.flagforge.common.enums.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TemplateValuesEntity in customer-api. Read-only access to template override
 * values.
 */
@Repository
public interface TemplateValuesRepository extends JpaRepository<TemplateValuesEntity, UUID> {

    Optional<TemplateValuesEntity> findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
            UUID applicationId, UUID environmentId, TemplateType type, String identifier);
}
