package io.hephaistos.flagforge.common.enums;

/**
 * Status of a Stripe subscription.
 * <p>
 * Maps to Stripe's subscription status values.
 *
 * @see <a href="https://docs.stripe.com/api/subscriptions/object#subscription_object-status">Stripe
 * Subscription Status</a>
 */
public enum SubscriptionStatus {
    /**
     * The subscription has been created but payment has not been confirmed.
     */
    INCOMPLETE,

    /**
     * The initial payment attempt failed and the subscription is no longer valid.
     */
    INCOMPLETE_EXPIRED,

    /**
     * The subscription is in a trial period.
     */
    TRIALING,

    /**
     * The subscription is active and payments are being collected.
     */
    ACTIVE,

    /**
     * The latest payment failed but the subscription is still active.
     */
    PAST_DUE,

    /**
     * The subscription has been canceled.
     */
    CANCELED,

    /**
     * The subscription is unpaid after exhausting all retry attempts.
     */
    UNPAID,

    /**
     * The subscription is paused (collection is paused).
     */
    PAUSED;

    /**
     * Converts a Stripe subscription status string to the corresponding enum value.
     *
     * @param stripeStatus the status string from Stripe API
     * @return the corresponding SubscriptionStatus
     * @throws IllegalArgumentException if the status is unknown
     */
    public static SubscriptionStatus fromStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "incomplete" -> INCOMPLETE;
            case "incomplete_expired" -> INCOMPLETE_EXPIRED;
            case "trialing" -> TRIALING;
            case "active" -> ACTIVE;
            case "past_due" -> PAST_DUE;
            case "canceled" -> CANCELED;
            case "unpaid" -> UNPAID;
            case "paused" -> PAUSED;
            default -> throw new IllegalArgumentException(
                    "Unknown Stripe subscription status: " + stripeStatus);
        };
    }

    /**
     * Returns true if this status indicates the subscription is in good standing.
     */
    public boolean isActive() {
        return this == ACTIVE || this == TRIALING;
    }

    /**
     * Returns true if this status indicates the subscription needs attention.
     */
    public boolean needsAction() {
        return this == INCOMPLETE || this == PAST_DUE || this == UNPAID;
    }
}
