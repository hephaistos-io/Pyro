package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.TemplateEntity;
import io.hephaistos.flagforge.common.enums.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TemplateEntity.
 *
 * IMPORTANT: Use findByIdFiltered() instead of findById() when you need Hibernate filters to
 * apply.
 */
@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID> {

    /**
     * Find a template by ID with Hibernate filters applied.
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.id = :id")
    Optional<TemplateEntity> findByIdFiltered(@Param("id") UUID id);

    /**
     * Check if a template exists by ID with Hibernate filters applied.
     */
    @Query("SELECT COUNT(t) > 0 FROM TemplateEntity t WHERE t.id = :id")
    boolean existsByIdFiltered(@Param("id") UUID id);

    /**
     * Find all templates for an application.
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.applicationId = :applicationId")
    List<TemplateEntity> findByApplicationId(@Param("applicationId") UUID applicationId);

    /**
     * Find a template by application and type.
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.applicationId = :applicationId AND t.type = :type")
    Optional<TemplateEntity> findByApplicationIdAndType(@Param("applicationId") UUID applicationId,
            @Param("type") TemplateType type);

    /**
     * Check if a template exists for the given application and type.
     */
    @Query("SELECT COUNT(t) > 0 FROM TemplateEntity t WHERE t.applicationId = :applicationId AND t.type = :type")
    boolean existsByApplicationIdAndType(@Param("applicationId") UUID applicationId,
            @Param("type") TemplateType type);
}
