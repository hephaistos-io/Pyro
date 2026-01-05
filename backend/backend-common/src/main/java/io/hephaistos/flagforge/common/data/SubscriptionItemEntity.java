package io.hephaistos.flagforge.common.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a Stripe subscription item.
 * <p>
 * Maps an environment to a line item in the company's Stripe subscription. Each paid environment
 * has exactly one subscription item.
 */
@Entity
@Table(name = "subscription_item")
public class SubscriptionItemEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private CompanySubscriptionEntity subscription;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "stripe_subscription_item_id", nullable = false)
    private String stripeSubscriptionItemId;

    @Column(name = "stripe_price_id", nullable = false)
    private String stripePriceId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CompanySubscriptionEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(CompanySubscriptionEntity subscription) {
        this.subscription = subscription;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
    }

    public String getStripeSubscriptionItemId() {
        return stripeSubscriptionItemId;
    }

    public void setStripeSubscriptionItemId(String stripeSubscriptionItemId) {
        this.stripeSubscriptionItemId = stripeSubscriptionItemId;
    }

    public String getStripePriceId() {
        return stripePriceId;
    }

    public void setStripePriceId(String stripePriceId) {
        this.stripePriceId = stripePriceId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
