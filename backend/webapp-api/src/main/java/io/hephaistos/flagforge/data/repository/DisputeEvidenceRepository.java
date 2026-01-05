package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.DisputeEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidenceEntity, UUID> {

    /**
     * Find dispute by Stripe dispute ID.
     */
    Optional<DisputeEvidenceEntity> findByStripeDisputeId(String stripeDisputeId);

    /**
     * Find dispute by Stripe charge ID.
     */
    Optional<DisputeEvidenceEntity> findByStripeChargeId(String stripeChargeId);

    /**
     * Find all disputes for a company.
     */
    List<DisputeEvidenceEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    /**
     * Find all open disputes (not yet resolved).
     */
    List<DisputeEvidenceEntity> findByStatusNotIn(List<String> closedStatuses);

    /**
     * Find disputes that haven't had evidence submitted.
     */
    List<DisputeEvidenceEntity> findByEvidenceSubmittedFalse();

    /**
     * Check if a dispute record exists.
     */
    boolean existsByStripeDisputeId(String stripeDisputeId);
}
