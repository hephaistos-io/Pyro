package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.EmailChangeTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeTokenEntity, UUID> {

    Optional<EmailChangeTokenEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE EmailChangeTokenEntity t SET t.usedAt = :now WHERE t.customerId = :customerId AND t.usedAt IS NULL")
    void invalidatePendingRequests(@Param("customerId") UUID customerId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM EmailChangeTokenEntity t WHERE t.expiresAt < :cutoff")
    void deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}
