package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.data.CompanyInviteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyInviteRepository extends JpaRepository<CompanyInviteEntity, UUID> {

    @Query("SELECT i FROM CompanyInviteEntity i LEFT JOIN FETCH i.preAssignedApplications WHERE i.token = :token")
    Optional<CompanyInviteEntity> findByToken(@Param("token") String token);

    @Query("SELECT i FROM CompanyInviteEntity i WHERE i.usedAt IS NULL")
    List<CompanyInviteEntity> findPending();
}
