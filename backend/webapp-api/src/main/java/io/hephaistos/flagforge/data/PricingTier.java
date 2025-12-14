package io.hephaistos.flagforge.data;

/**
 * Represents the pricing tier. FREE tier is available for: - The first application created by a
 * company - The default environment created with each application
 *
 * PAID tier applies to: - Additional applications beyond the first one - Additional environments
 * created within an application
 */
public enum PricingTier {
    FREE,
    PAID
}
