package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.RegistrationVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationVerificationTokenRepository
        extends JpaRepository<RegistrationVerificationTokenEntity, UUID> {

    Optional<RegistrationVerificationTokenEntity> findByToken(String token);

    Optional<RegistrationVerificationTokenEntity> findByCustomerId(UUID customerId);

    @Modifying
    @Query("DELETE FROM RegistrationVerificationTokenEntity t WHERE t.expiresAt < :cutoff")
    void deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}
