package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CustomerEntity. Note: Queries are filtered by company via Hibernate @Filter when
 * the CompanyFilterAspect enables the filter in the service layer.
 *
 * IMPORTANT: Use findByIdFiltered() instead of findById() when you need Hibernate filters to apply.
 * The built-in findById() uses EntityManager.find() which bypasses Hibernate filters.
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByEmail(String email);

    @Query("SELECT DISTINCT c FROM CustomerEntity c LEFT JOIN FETCH c.accessibleApplications WHERE c.email = :email")
    Optional<CustomerEntity> findByEmailWithAccessibleApplications(@Param("email") String email);

    /**
     * Find a customer by ID with Hibernate filters applied. Use this instead of findById() when
     * filter enforcement is required.
     */
    @Query("SELECT DISTINCT c FROM CustomerEntity c LEFT JOIN FETCH c.accessibleApplications WHERE c.id = :id")
    Optional<CustomerEntity> findByIdFiltered(@Param("id") UUID id);

    /**
     * Find customers by company ID. Returns the first customer found (typically the admin).
     */
    @Query("SELECT c FROM CustomerEntity c WHERE c.companyId = :companyId ORDER BY c.id ASC")
    List<CustomerEntity> findByCompanyId(@Param("companyId") UUID companyId);

}
