package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.data.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByEmail(String email);

    @Query("SELECT DISTINCT c FROM CustomerEntity c LEFT JOIN FETCH c.accessibleApplications WHERE c.email = :email")
    Optional<CustomerEntity> findByEmailWithAccessibleApplications(@Param("email") String email);

}
