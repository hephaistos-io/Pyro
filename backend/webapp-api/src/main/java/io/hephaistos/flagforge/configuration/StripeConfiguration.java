package io.hephaistos.flagforge.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Stripe payment integration.
 * <p>
 * Supports three modes:
 * <ul>
 *   <li>MOCK - Uses stripe-mock container for fast local development/testing</li>
 *   <li>SANDBOX - Uses real Stripe test API with CLI webhook forwarding</li>
 *   <li>PRODUCTION - Uses real Stripe live API</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties("flagforge.stripe")
public class StripeConfiguration {

    /**
     * Stripe API secret key. Use sk_test_* for test mode, sk_live_* for production.
     */
    private String apiKey = "sk_test_mock";
    /**
     * Stripe webhook endpoint secret for signature verification.
     */
    private String webhookSecret = "whsec_mock";
    /**
     * Stripe publishable key for frontend. Use pk_test_* for test mode.
     */
    private String publishableKey = "pk_test_mock";
    /**
     * Stripe integration mode. Defaults to MOCK for local development.
     */
    private StripeMode mode = StripeMode.MOCK;
    /**
     * Base URL for stripe-mock server. Only used when mode is MOCK.
     */
    private String mockBaseUrl = "http://localhost:12111";
    /**
     * Mapping of pricing tier names to Stripe Price IDs. Keys should match PricingTier enum values
     * (BASIC, STANDARD, PRO, BUSINESS).
     */
    private Map<String, String> priceIds = new HashMap<>();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public StripeMode getMode() {
        return mode;
    }

    public void setMode(StripeMode mode) {
        this.mode = mode;
    }

    /**
     * Check if running in mock mode (stripe-mock container).
     */
    public boolean isMock() {
        return mode == StripeMode.MOCK;
    }

    /**
     * Check if running in sandbox mode (real Stripe test API).
     */
    public boolean isSandbox() {
        return mode == StripeMode.SANDBOX;
    }

    /**
     * Check if running in production mode (real Stripe live API).
     */
    public boolean isProduction() {
        return mode == StripeMode.PRODUCTION;
    }

    /**
     * Backward compatibility alias for {@link #isMock()}.
     *
     * @deprecated Use {@link #isMock()} instead
     */
    @Deprecated
    public boolean isMockEnabled() {
        return isMock();
    }

    /**
     * Backward compatibility setter for mock mode.
     *
     * @deprecated Use {@link #setMode(StripeMode)} instead
     */
    @Deprecated
    public void setMockEnabled(boolean mockEnabled) {
        this.mode = mockEnabled ? StripeMode.MOCK : StripeMode.PRODUCTION;
    }

    public String getMockBaseUrl() {
        return mockBaseUrl;
    }

    public void setMockBaseUrl(String mockBaseUrl) {
        this.mockBaseUrl = mockBaseUrl;
    }

    public Map<String, String> getPriceIds() {
        return priceIds;
    }

    public void setPriceIds(Map<String, String> priceIds) {
        this.priceIds = priceIds;
    }

    /**
     * Get the Price ID for a specific pricing tier.
     *
     * @param tierName the tier name (e.g., "BASIC", "STANDARD", "PRO", "BUSINESS")
     * @return the Stripe Price ID, or null if not configured
     */
    public String getPriceIdForTier(String tierName) {
        return priceIds.get(tierName);
    }

    /**
     * Get the effective Stripe API base URL. Returns the mock URL if mock mode is enabled,
     * otherwise null (use Stripe default).
     */
    public String getEffectiveApiBase() {
        return isMock() ? mockBaseUrl : null;
    }

    /**
     * Stripe integration mode.
     */
    public enum StripeMode {
        /** Uses stripe-mock container for fast local testing */
        MOCK,
        /** Uses real Stripe test API (sk_test_*) with CLI webhook forwarding */
        SANDBOX,
        /** Uses real Stripe live API (sk_live_*) */
        PRODUCTION
    }
}
