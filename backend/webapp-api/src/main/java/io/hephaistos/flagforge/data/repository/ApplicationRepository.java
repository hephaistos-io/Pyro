package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.data.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ApplicationEntity. Note: Queries are filtered by company via Hibernate @Filter
 * when the CompanyFilterAspect enables the filter in the service layer.
 *
 * IMPORTANT: Use findByIdFiltered() instead of findById() when you need Hibernate filters to apply.
 * The built-in findById() uses EntityManager.find() which bypasses Hibernate filters.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {

    List<ApplicationEntity> findByCompanyId(UUID companyId);

    boolean existsByNameAndCompanyId(String name, UUID companyId);

    long countByCompanyId(UUID companyId);

    /**
     * Find an application by ID with Hibernate filters applied. Use this instead of findById() when
     * filter enforcement is required.
     */
    @Query("SELECT a FROM ApplicationEntity a WHERE a.id = :id")
    Optional<ApplicationEntity> findByIdFiltered(@Param("id") UUID id);

    /**
     * Check if an application exists by ID with Hibernate filters applied. Use this instead of
     * existsById() when filter enforcement is required.
     */
    @Query("SELECT COUNT(a) > 0 FROM ApplicationEntity a WHERE a.id = :id")
    boolean existsByIdFiltered(@Param("id") UUID id);
}
