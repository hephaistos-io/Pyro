package io.hephaistos.flagforge.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import io.hephaistos.flagforge.common.data.SubscriptionItemEntity;
import io.hephaistos.flagforge.common.enums.PaymentStatus;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.common.enums.SubscriptionStatus;
import io.hephaistos.flagforge.configuration.StripeConfiguration;
import io.hephaistos.flagforge.controller.dto.CheckoutSessionResponse;
import io.hephaistos.flagforge.controller.dto.InvoiceResponse;
import io.hephaistos.flagforge.data.repository.CompanySubscriptionRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.SubscriptionItemRepository;
import io.hephaistos.flagforge.exception.PaymentException;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Mock implementation of StripeService for local development and testing.
 * <p>
 * Uses stripe-mock server for API calls and provides simulated webhook behavior through the
 * completeCheckoutSession method.
 */
@Service
@Transactional
@ConditionalOnProperty(name = "flagforge.stripe.mode", havingValue = "mock", matchIfMissing = true)
public class MockStripeService extends AbstractStripeService {

    /**
     * Local cache for pending checkout sessions. stripe-mock doesn't persist session metadata, so
     * we store it locally.
     */
    private final Map<String, PendingCheckoutData> pendingCheckouts = new ConcurrentHashMap<>();


    public MockStripeService(StripeConfiguration config,
            CompanySubscriptionRepository subscriptionRepository,
            SubscriptionItemRepository subscriptionItemRepository,
            EnvironmentRepository environmentRepository) {
        super(config, subscriptionRepository, subscriptionItemRepository, environmentRepository);
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = config.getApiKey();
        Stripe.overrideApiBase(config.getMockBaseUrl());
        logger.info("Stripe configured in MOCK mode with base URL: {}", config.getMockBaseUrl());
    }

    @Override
    public String createOrGetCustomer(UUID companyId, String companyName, String email) {
        return subscriptionRepository.findByCompanyId(companyId)
                .map(CompanySubscriptionEntity::getStripeCustomerId)
                .orElseGet(() -> createCustomer(companyId, companyName, email));
    }

    private String createCustomer(UUID companyId, String companyName, String email) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setName(companyName)
                    .setEmail(email)
                    .putMetadata("company_id", companyId.toString())
                    .build();

            Customer customer = Customer.create(params);

            CompanySubscriptionEntity subscription = new CompanySubscriptionEntity();
            subscription.setCompanyId(companyId);
            subscription.setStripeCustomerId(customer.getId());
            subscription.setStatus(SubscriptionStatus.INCOMPLETE);
            subscriptionRepository.save(subscription);

            logger.info("[MOCK] Created Stripe customer {} for company {}", customer.getId(),
                    companyId);
            return customer.getId();

        }
        catch (StripeException e) {
            throw new PaymentException("Failed to create Stripe customer", e);
        }
    }

    @Override
    public CheckoutSessionResponse createCheckoutSession(UUID companyId,
            List<PendingEnvironmentItem> items, String successUrl, String cancelUrl) {

        CompanySubscriptionEntity subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new PaymentException(
                        "Customer not found. Please set up billing first."));

        // Filter to only paid items
        List<PendingEnvironmentItem> paidItems =
                items.stream().filter(item -> item.tier().isPaid()).toList();

        if (paidItems.isEmpty()) {
            throw new PaymentException(
                    "No paid items in checkout. Only paid tiers require payment.");
        }

        List<SessionCreateParams.LineItem> lineItems = paidItems.stream()
                .map(item -> SessionCreateParams.LineItem.builder()
                        .setPrice(config.getPriceIdForTier(item.tier().name()))
                        .setQuantity(1L)
                        .build())
                .toList();

        // Store only paid environment IDs in order matching line items
        String environmentIds = paidItems.stream()
                .map(item -> item.environmentId().toString())
                .collect(Collectors.joining(","));

        // Store tier names in same order for mapping
        String tierNames =
                paidItems.stream().map(item -> item.tier().name()).collect(Collectors.joining(","));

        try {
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setCustomer(subscription.getStripeCustomerId())
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .putMetadata("company_id", companyId.toString())
                    .putMetadata("environment_ids", environmentIds)
                    .putMetadata("tier_names", tierNames);

            lineItems.forEach(paramsBuilder::addLineItem);

            Session session = Session.create(paramsBuilder.build());

            // Store pending checkout data locally (stripe-mock doesn't persist metadata)
            List<UUID> envIds =
                    paidItems.stream().map(PendingEnvironmentItem::environmentId).toList();
            List<String> tiers = paidItems.stream().map(item -> item.tier().name()).toList();
            pendingCheckouts.put(session.getId(),
                    new PendingCheckoutData(companyId, envIds, tiers));

            logger.info("[MOCK] Created checkout session {} for company {} with {} items",
                    session.getId(), companyId, paidItems.size());

            // In mock mode, redirect directly to success URL (stripe-mock doesn't have a real checkout UI)
            String redirectUrl = successUrl + "?session_id=" + session.getId();

            return new CheckoutSessionResponse(session.getId(), redirectUrl);

        }
        catch (StripeException e) {
            throw new PaymentException("Failed to create checkout session", e);
        }
    }

    @Override
    public String createPortalSession(UUID companyId, String returnUrl) {
        // Validate that a customer exists (same validation as production)
        subscriptionRepository.findByCompanyId(companyId)
                .filter(sub -> sub.getStripeCustomerId() != null)
                .orElseThrow(() -> new PaymentException("No customer found for this company"));

        // In mock mode, just return the return URL (stripe-mock doesn't have a portal UI)
        logger.info("[MOCK] Portal session requested for company {}, returning to {}", companyId,
                returnUrl);
        return returnUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(UUID companyId, int limit) {
        // In mock mode, return empty list (stripe-mock doesn't maintain invoice state)
        logger.debug("[MOCK] Invoice list requested for company {}, returning empty list",
                companyId);
        return List.of();
    }

    @Override
    public void addSubscriptionItem(UUID companyId, UUID environmentId, PricingTier tier) {
        if (tier.isFree()) {
            logger.debug("[MOCK] Skipping subscription item creation for free tier environment {}",
                    environmentId);
            return;
        }

        CompanySubscriptionEntity subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new PaymentException("No subscription found for this company"));

        if (subscription.getStripeSubscriptionId() == null) {
            throw new PaymentException("No active subscription. Please complete checkout first.");
        }

        // Create a mock subscription item directly in the database
        SubscriptionItemEntity itemEntity = new SubscriptionItemEntity();
        itemEntity.setSubscription(subscription);
        itemEntity.setEnvironmentId(environmentId);
        itemEntity.setStripeSubscriptionItemId(
                "si_mock_" + environmentId.toString().substring(0, 8));
        itemEntity.setStripePriceId(config.getPriceIdForTier(tier.name()));
        subscriptionItemRepository.save(itemEntity);

        logger.info("[MOCK] Added subscription item for environment {}", environmentId);
    }

    @Override
    public void updateSubscriptionItem(UUID companyId, UUID environmentId, PricingTier newTier) {
        SubscriptionItemEntity item =
                subscriptionItemRepository.findByEnvironmentId(environmentId).orElse(null);

        if (newTier.isFree()) {
            if (item != null) {
                removeSubscriptionItem(companyId, environmentId);
            }
            return;
        }

        if (item == null) {
            addSubscriptionItem(companyId, environmentId, newTier);
            return;
        }

        // Update the price ID
        item.setStripePriceId(config.getPriceIdForTier(newTier.name()));
        subscriptionItemRepository.save(item);

        logger.info("[MOCK] Updated subscription item for environment {} to tier {}", environmentId,
                newTier.name());
    }

    @Override
    public void removeSubscriptionItem(UUID companyId, UUID environmentId) {
        subscriptionItemRepository.findByEnvironmentId(environmentId).ifPresent(item -> {
            subscriptionItemRepository.delete(item);
            logger.info("[MOCK] Removed subscription item for environment {}", environmentId);
        });
    }

    @Override
    public void completeCheckoutSession(String sessionId) {
        // Retrieve from local cache (stripe-mock doesn't persist metadata)
        PendingCheckoutData checkoutData = pendingCheckouts.remove(sessionId);

        if (checkoutData == null) {
            logger.warn("[MOCK] No pending checkout found for session {}", sessionId);
            return;
        }

        UUID companyId = checkoutData.companyId();
        List<UUID> environmentIds = checkoutData.environmentIds();
        List<String> tierNames = checkoutData.tierNames();

        // Update environments to PAID status
        for (UUID envId : environmentIds) {
            environmentRepository.findById(envId).ifPresent(env -> {
                env.setPaymentStatus(PaymentStatus.PAID);
                environmentRepository.save(env);
                logger.info("[MOCK] Environment {} marked as PAID", envId);
            });
        }

        // Update subscription record
        subscriptionRepository.findByCompanyId(companyId).ifPresent(sub -> {
            sub.setStripeSubscriptionId("sub_mock_" + companyId.toString().substring(0, 8));
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.touch();
            subscriptionRepository.save(sub);
            logger.info("[MOCK] Subscription activated for company {}", companyId);

            // Create subscription item records
            createSubscriptionItemsFromMetadata(sub, environmentIds, tierNames);
        });

        logger.info("[MOCK] Checkout completed for session {}, company {}", sessionId, companyId);
    }

    private void createSubscriptionItemsFromMetadata(CompanySubscriptionEntity subscription,
            List<UUID> environmentIds, List<String> tierNames) {

        for (int i = 0; i < environmentIds.size(); i++) {
            UUID environmentId = environmentIds.get(i);
            String tierName = i < tierNames.size() ? tierNames.get(i) : "UNKNOWN";

            // Check if already exists
            if (subscriptionItemRepository.findByEnvironmentId(environmentId).isPresent()) {
                logger.debug("[MOCK] Subscription item already exists for environment {}",
                        environmentId);
                continue;
            }

            SubscriptionItemEntity itemEntity = new SubscriptionItemEntity();
            itemEntity.setSubscription(subscription);
            itemEntity.setEnvironmentId(environmentId);
            itemEntity.setStripeSubscriptionItemId(
                    "si_mock_" + environmentId.toString().substring(0, 8));
            itemEntity.setStripePriceId(config.getPriceIdForTier(tierName));
            subscriptionItemRepository.save(itemEntity);

            logger.info("[MOCK] Created subscription item for environment {} tier {}",
                    environmentId, tierName);
        }
    }


    /**
     * Data stored when creating a checkout session, retrieved when completing it.
     */
    public record PendingCheckoutData(UUID companyId, List<UUID> environmentIds,
                                      List<String> tierNames) {
    }
}
