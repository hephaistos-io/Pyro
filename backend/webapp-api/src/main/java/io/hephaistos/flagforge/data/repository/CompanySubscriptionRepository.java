package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanySubscriptionRepository
        extends JpaRepository<CompanySubscriptionEntity, UUID> {

    /**
     * Find subscription by company ID.
     */
    Optional<CompanySubscriptionEntity> findByCompanyId(UUID companyId);

    /**
     * Find subscription by Stripe customer ID.
     */
    Optional<CompanySubscriptionEntity> findByStripeCustomerId(String stripeCustomerId);

    /**
     * Find subscription by Stripe subscription ID.
     */
    Optional<CompanySubscriptionEntity> findByStripeSubscriptionId(String stripeSubscriptionId);

    /**
     * Check if a company already has a subscription.
     */
    boolean existsByCompanyId(UUID companyId);
}
