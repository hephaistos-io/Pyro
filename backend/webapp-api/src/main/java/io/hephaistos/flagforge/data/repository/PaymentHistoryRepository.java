package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.PaymentHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistoryEntity, UUID> {

    /**
     * Find payment history for a company, ordered by creation date descending.
     */
    List<PaymentHistoryEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    /**
     * Find payment history for a company with pagination.
     */
    List<PaymentHistoryEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId,
            Pageable pageable);

    /**
     * Find payment by Stripe invoice ID.
     */
    Optional<PaymentHistoryEntity> findByStripeInvoiceId(String stripeInvoiceId);

    /**
     * Find payment by Stripe charge ID.
     */
    Optional<PaymentHistoryEntity> findByStripeChargeId(String stripeChargeId);

    /**
     * Check if a payment record exists for an invoice.
     */
    boolean existsByStripeInvoiceId(String stripeInvoiceId);
}
