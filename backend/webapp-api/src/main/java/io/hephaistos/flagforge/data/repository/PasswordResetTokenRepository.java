package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByToken(String token);

    @Query("SELECT COUNT(t) FROM PasswordResetTokenEntity t WHERE t.customerId = :customerId AND t.createdAt > :since")
    long countRecentRequests(@Param("customerId") UUID customerId, @Param("since") Instant since);

    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiresAt < :cutoff")
    void deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}
