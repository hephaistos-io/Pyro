package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.controller.dto.CheckoutSessionResponse;
import io.hephaistos.flagforge.controller.dto.InvoiceResponse;
import io.hephaistos.flagforge.controller.dto.SubscriptionStatusResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service for Stripe payment integration.
 * <p>
 * Handles customer management, checkout sessions, subscriptions, and customer portal. Uses
 * stripe-mock in development/test environments.
 *
 * @see <a href="https://docs.stripe.com/api">Stripe API Documentation</a>
 */
public interface StripeService {

    /**
     * Creates or retrieves a Stripe customer for the given company. If the company already has a
     * Stripe customer, returns the existing customer ID.
     *
     * @param companyId   the company ID
     * @param companyName the company name
     * @param email       the billing contact email
     * @return the Stripe customer ID
     */
    String createOrGetCustomer(UUID companyId, String companyName, String email);

    /**
     * Creates a Stripe Checkout session for purchasing environments. The session allows customers
     * to enter payment details on Stripe's hosted page.
     *
     * @param companyId  the company ID
     * @param items      list of pending environments with their tiers
     * @param successUrl URL to redirect to on successful payment
     * @param cancelUrl  URL to redirect to on cancelled payment
     * @return checkout session details including the URL
     */
    CheckoutSessionResponse createCheckoutSession(UUID companyId,
            List<PendingEnvironmentItem> items, String successUrl, String cancelUrl);

    /**
     * Creates a Stripe Customer Portal session for self-service subscription management. Customers
     * can view invoices, update payment methods, and cancel subscriptions.
     *
     * @param companyId the company ID
     * @param returnUrl URL to return to after leaving the portal
     * @return the portal session URL
     */
    String createPortalSession(UUID companyId, String returnUrl);

    /**
     * Gets the current subscription status for a company.
     *
     * @param companyId the company ID
     * @return subscription status details, or null if no subscription exists
     */
    SubscriptionStatusResponse getSubscriptionStatus(UUID companyId);

    /**
     * Gets invoice history for a company.
     *
     * @param companyId the company ID
     * @param limit     maximum number of invoices to return
     * @return list of invoices
     */
    List<InvoiceResponse> getInvoices(UUID companyId, int limit);

    /**
     * Adds a new subscription item for an environment. Called after successful checkout to link the
     * environment to the subscription.
     *
     * @param companyId     the company ID
     * @param environmentId the environment ID
     * @param tier          the pricing tier
     */
    void addSubscriptionItem(UUID companyId, UUID environmentId, PricingTier tier);

    /**
     * Updates an environment's subscription item to a new tier.
     *
     * @param companyId     the company ID
     * @param environmentId the environment ID
     * @param newTier       the new pricing tier
     */
    void updateSubscriptionItem(UUID companyId, UUID environmentId, PricingTier newTier);

    /**
     * Removes a subscription item for an environment (when environment is deleted).
     *
     * @param companyId     the company ID
     * @param environmentId the environment ID
     */
    void removeSubscriptionItem(UUID companyId, UUID environmentId);

    /**
     * Completes a checkout session by processing the payment. In mock mode, this simulates the
     * webhook that Stripe would normally send. In production mode, this is a no-op as webhooks
     * handle completion.
     *
     * @param sessionId the Stripe checkout session ID
     */
    void completeCheckoutSession(String sessionId);

    /**
     * Represents a pending environment item for checkout.
     */
    record PendingEnvironmentItem(UUID environmentId, String environmentName, PricingTier tier) {
    }
}
