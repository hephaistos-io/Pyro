package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.TemplateValuesEntity;
import io.hephaistos.flagforge.common.enums.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TemplateValuesEntity.
 */
@Repository
public interface TemplateValuesRepository extends JpaRepository<TemplateValuesEntity, UUID> {

    /**
     * Find all overrides for a specific application, environment, and type.
     */
    @Query("SELECT tv FROM TemplateValuesEntity tv " + "WHERE tv.applicationId = :applicationId " + "AND tv.environmentId = :environmentId " + "AND tv.type = :type")
    List<TemplateValuesEntity> findByApplicationIdAndEnvironmentIdAndType(
            @Param("applicationId") UUID applicationId, @Param("environmentId") UUID environmentId,
            @Param("type") TemplateType type);

    /**
     * Find overrides for specific identifiers (for merge query).
     */
    @Query("SELECT tv FROM TemplateValuesEntity tv " + "WHERE tv.applicationId = :applicationId " + "AND tv.environmentId = :environmentId " + "AND tv.type = :type " + "AND tv.identifier IN :identifiers")
    List<TemplateValuesEntity> findByApplicationIdAndEnvironmentIdAndTypeAndIdentifierIn(
            @Param("applicationId") UUID applicationId, @Param("environmentId") UUID environmentId,
            @Param("type") TemplateType type, @Param("identifiers") List<String> identifiers);

    /**
     * Find a specific override by all key fields.
     */
    @Query("SELECT tv FROM TemplateValuesEntity tv " + "WHERE tv.applicationId = :applicationId " + "AND tv.environmentId = :environmentId " + "AND tv.type = :type " + "AND tv.identifier = :identifier")
    Optional<TemplateValuesEntity> findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
            @Param("applicationId") UUID applicationId, @Param("environmentId") UUID environmentId,
            @Param("type") TemplateType type, @Param("identifier") String identifier);

    /**
     * Check if an override exists.
     */
    @Query("SELECT COUNT(tv) > 0 FROM TemplateValuesEntity tv " + "WHERE tv.applicationId = :applicationId " + "AND tv.environmentId = :environmentId " + "AND tv.type = :type " + "AND tv.identifier = :identifier")
    boolean existsByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
            @Param("applicationId") UUID applicationId, @Param("environmentId") UUID environmentId,
            @Param("type") TemplateType type, @Param("identifier") String identifier);

    /**
     * Find all overrides for an application and environment.
     */
    @Query("SELECT tv FROM TemplateValuesEntity tv WHERE tv.applicationId = :applicationId AND tv.environmentId = :environmentId")
    List<TemplateValuesEntity> findByApplicationIdAndEnvironmentId(
            @Param("applicationId") UUID applicationId, @Param("environmentId") UUID environmentId);
}
