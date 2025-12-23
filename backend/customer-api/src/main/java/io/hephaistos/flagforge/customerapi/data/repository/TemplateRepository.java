package io.hephaistos.flagforge.customerapi.data.repository;

import io.hephaistos.flagforge.common.data.TemplateEntity;
import io.hephaistos.flagforge.common.enums.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TemplateEntity in customer-api. Read-only access to template schema definitions.
 */
@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID> {

    Optional<TemplateEntity> findByApplicationIdAndType(UUID applicationId, TemplateType type);
}
