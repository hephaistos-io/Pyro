package io.hephaistos.flagforge.common.enums;

/**
 * Payment status for billable resources like environments.
 * <p>
 * Used to track whether a resource has been paid for and is fully active.
 */
public enum PaymentStatus {
    /**
     * Payment has been confirmed. Resource is fully active.
     */
    PAID,

    /**
     * Resource is in the cart, awaiting checkout. API keys may be restricted until payment is
     * confirmed.
     */
    PENDING,

    /**
     * Payment failed or was never completed. Resource functionality may be limited.
     */
    UNPAID;

    /**
     * Returns true if this status allows full resource functionality.
     */
    public boolean isFullyActive() {
        return this == PAID;
    }

    /**
     * Returns true if this status blocks certain functionality (like API key generation).
     */
    public boolean isBlocked() {
        return this == PENDING || this == UNPAID;
    }
}
