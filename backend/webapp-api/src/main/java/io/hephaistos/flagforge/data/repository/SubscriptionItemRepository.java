package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.SubscriptionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionItemRepository extends JpaRepository<SubscriptionItemEntity, UUID> {

    /**
     * Find all subscription items for a subscription.
     */
    List<SubscriptionItemEntity> findBySubscription_Id(UUID subscriptionId);

    /**
     * Find subscription item by environment ID.
     */
    Optional<SubscriptionItemEntity> findByEnvironmentId(UUID environmentId);

    /**
     * Find subscription item by Stripe subscription item ID.
     */
    Optional<SubscriptionItemEntity> findByStripeSubscriptionItemId(
            String stripeSubscriptionItemId);

    /**
     * Delete subscription item by environment ID.
     */
    void deleteByEnvironmentId(UUID environmentId);

    /**
     * Check if an environment has a subscription item.
     */
    boolean existsByEnvironmentId(UUID environmentId);
}
