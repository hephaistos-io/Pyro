package io.hephaistos.flagforge.common.enums;

/**
 * Represents the pricing tier with associated rate limits and pricing.
 *
 * <p>Tier Structure:
 * <ul>
 *   <li>FREE: 500k req/month, 5 req/sec, $0/month - First application and default environments</li>
 *   <li>BASIC: 2M req/month, 20 req/sec, $10/month - Additional applications and environments</li>
 *   <li>STANDARD: 10M req/month, 100 req/sec, $40/month</li>
 *   <li>PRO: 25M req/month, 500 req/sec, $100/month</li>
 *   <li>BUSINESS: 100M req/month, 2000 req/sec, $400/month</li>
 * </ul>
 *
 * <p>Tier Assignment:
 * <ul>
 *   <li>FREE tier is available for:
 *     <ul>
 *       <li>The first application created by a company</li>
 *       <li>The default environments (Development, Production) created with each application</li>
 *     </ul>
 *   </li>
 *   <li>BASIC tier (and higher) applies to:
 *     <ul>
 *       <li>Additional applications beyond the first one</li>
 *       <li>Additional custom environments created within an application</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public enum PricingTier {
    FREE(500_000L, 5, 0),
    BASIC(2_000_000L, 20, 10),
    STANDARD(10_000_000L, 100, 40),
    PRO(25_000_000L, 500, 100),
    BUSINESS(100_000_000L, 2000, 400);

    private final Long requestsPerMonth;
    private final Integer requestsPerSecond;
    private final Integer monthlyPriceUsd;

    PricingTier(Long requestsPerMonth, Integer requestsPerSecond, Integer monthlyPriceUsd) {
        this.requestsPerMonth = requestsPerMonth;
        this.requestsPerSecond = requestsPerSecond;
        this.monthlyPriceUsd = monthlyPriceUsd;
    }

    /**
     * Gets the maximum number of requests allowed per month for this tier.
     *
     * @return the monthly request limit
     */
    public Long getRequestsPerMonth() {
        return requestsPerMonth;
    }

    /**
     * Gets the maximum number of requests allowed per second for this tier.
     *
     * @return the per-second request limit
     */
    public Integer getRequestsPerSecond() {
        return requestsPerSecond;
    }

    /**
     * Returns true if this is the free tier.
     *
     * @return true if this tier is FREE, false otherwise
     */
    public boolean isFree() {
        return this == FREE;
    }

    /**
     * Returns true if this is a paid tier (not FREE).
     *
     * @return true if this tier is not FREE, false otherwise
     */
    public boolean isPaid() {
        return this != FREE;
    }

    /**
     * Gets the monthly price in USD for this tier.
     *
     * @return the monthly price in USD
     */
    public Integer getMonthlyPriceUsd() {
        return monthlyPriceUsd;
    }
}
