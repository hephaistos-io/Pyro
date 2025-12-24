package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.UserTemplateValuesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for UserTemplateValuesEntity to access user template statistics.
 */
@Repository
public interface UserTemplateValuesRepository
        extends JpaRepository<UserTemplateValuesEntity, UUID> {

    long countByApplicationId(UUID applicationId);
}
