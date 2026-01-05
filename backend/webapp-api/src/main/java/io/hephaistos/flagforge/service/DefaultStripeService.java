package io.hephaistos.flagforge.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.SubscriptionItemCreateParams;
import com.stripe.param.SubscriptionItemUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import io.hephaistos.flagforge.common.data.SubscriptionItemEntity;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production/Sandbox implementation of StripeService using the Stripe Java SDK.
 * <p>
 * This service is activated when mode is SANDBOX or PRODUCTION. Webhook events handle checkout
 * completion and subscription updates.
 */
@Service
@Transactional
@ConditionalOnExpression("'${flagforge.stripe.mode:mock}' != 'mock'")
public class DefaultStripeService extends AbstractStripeService {

    public DefaultStripeService(StripeConfiguration config,
            CompanySubscriptionRepository subscriptionRepository,
            SubscriptionItemRepository subscriptionItemRepository,
            EnvironmentRepository environmentRepository) {
        super(config, subscriptionRepository, subscriptionItemRepository, environmentRepository);
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = config.getApiKey();
        if (config.isSandbox()) {
            logger.info("Stripe configured in SANDBOX mode (test API)");
        }
        else {
            logger.info("Stripe configured in PRODUCTION mode");
        }
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

            logger.info("Created Stripe customer {} for company {}", customer.getId(), companyId);
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

        // Filter to only paid items and build line items
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

            logger.info("Created checkout session {} for company {}", session.getId(), companyId);

            return new CheckoutSessionResponse(session.getId(), session.getUrl());

        }
        catch (StripeException e) {
            throw new PaymentException("Failed to create checkout session", e);
        }
    }

    @Override
    public String createPortalSession(UUID companyId, String returnUrl) {
        CompanySubscriptionEntity subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new PaymentException("No subscription found for this company"));

        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(subscription.getStripeCustomerId())
                            .setReturnUrl(returnUrl)
                            .build();

            com.stripe.model.billingportal.Session session =
                    com.stripe.model.billingportal.Session.create(params);

            logger.info("Created portal session for company {}", companyId);
            return session.getUrl();

        }
        catch (StripeException e) {
            throw new PaymentException("Failed to create customer portal session", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(UUID companyId, int limit) {
        CompanySubscriptionEntity subscription =
                subscriptionRepository.findByCompanyId(companyId).orElse(null);

        if (subscription == null) {
            return List.of();
        }

        try {
            InvoiceListParams params = InvoiceListParams.builder()
                    .setCustomer(subscription.getStripeCustomerId())
                    .setLimit((long) limit)
                    .build();

            InvoiceCollection invoices = Invoice.list(params);

            return invoices.getData().stream().map(this::mapToInvoiceResponse).toList();

        }
        catch (StripeException e) {
            logger.error("Failed to fetch invoices for company {}", companyId, e);
            return List.of();
        }
    }

    private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
        return new InvoiceResponse(invoice.getId(),
                invoice.getAmountDue() != null ? invoice.getAmountDue().intValue() : 0,
                invoice.getCurrency(), invoice.getStatus(), invoice.getInvoicePdf(),
                invoice.getHostedInvoiceUrl(), invoice.getPeriodStart() != null ?
                Instant.ofEpochSecond(invoice.getPeriodStart()) :
                null, invoice.getPeriodEnd() != null ?
                Instant.ofEpochSecond(invoice.getPeriodEnd()) :
                null, invoice.getStatusTransitions() != null && invoice.getStatusTransitions()
                .getPaidAt() != null ?
                Instant.ofEpochSecond(invoice.getStatusTransitions().getPaidAt()) :
                null,
                invoice.getCreated() != null ? Instant.ofEpochSecond(invoice.getCreated()) : null);
    }

    @Override
    public void addSubscriptionItem(UUID companyId, UUID environmentId, PricingTier tier) {
        if (tier.isFree()) {
            logger.debug("Skipping subscription item creation for free tier environment {}",
                    environmentId);
            return;
        }

        CompanySubscriptionEntity subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new PaymentException("No subscription found for this company"));

        if (subscription.getStripeSubscriptionId() == null) {
            throw new PaymentException("No active subscription. Please complete checkout first.");
        }

        try {
            SubscriptionItemCreateParams params = SubscriptionItemCreateParams.builder()
                    .setSubscription(subscription.getStripeSubscriptionId())
                    .setPrice(config.getPriceIdForTier(tier.name()))
                    .setQuantity(1L)
                    .putMetadata("environment_id", environmentId.toString())
                    .build();

            SubscriptionItem stripeItem = SubscriptionItem.create(params);

            SubscriptionItemEntity itemEntity = new SubscriptionItemEntity();
            itemEntity.setSubscription(subscription);
            itemEntity.setEnvironmentId(environmentId);
            itemEntity.setStripeSubscriptionItemId(stripeItem.getId());
            itemEntity.setStripePriceId(config.getPriceIdForTier(tier.name()));
            subscriptionItemRepository.save(itemEntity);

            logger.info("Added subscription item {} for environment {}", stripeItem.getId(),
                    environmentId);

        }
        catch (StripeException e) {
            throw new PaymentException("Failed to add subscription item", e);
        }
    }

    @Override
    public void updateSubscriptionItem(UUID companyId, UUID environmentId, PricingTier newTier) {
        SubscriptionItemEntity item =
                subscriptionItemRepository.findByEnvironmentId(environmentId).orElse(null);

        if (newTier.isFree()) {
            // Downgrading to free tier - remove the subscription item
            if (item != null) {
                removeSubscriptionItem(companyId, environmentId);
            }
            return;
        }

        if (item == null) {
            // Upgrading from free tier - add new subscription item
            addSubscriptionItem(companyId, environmentId, newTier);
            return;
        }

        // Changing between paid tiers
        try {
            SubscriptionItemUpdateParams params = SubscriptionItemUpdateParams.builder()
                    .setPrice(config.getPriceIdForTier(newTier.name()))
                    .build();

            SubscriptionItem.retrieve(item.getStripeSubscriptionItemId()).update(params);

            item.setStripePriceId(config.getPriceIdForTier(newTier.name()));
            subscriptionItemRepository.save(item);

            logger.info("Updated subscription item for environment {} to tier {}", environmentId,
                    newTier.name());

        }
        catch (StripeException e) {
            throw new PaymentException("Failed to update subscription item", e);
        }
    }

    @Override
    public void removeSubscriptionItem(UUID companyId, UUID environmentId) {
        subscriptionItemRepository.findByEnvironmentId(environmentId).ifPresent(item -> {
            try {
                SubscriptionItem.retrieve(item.getStripeSubscriptionItemId()).delete();
                subscriptionItemRepository.delete(item);
                logger.info("Removed subscription item for environment {}", environmentId);

            }
            catch (StripeException e) {
                throw new PaymentException("Failed to remove subscription item", e);
            }
        });
    }

    @Override
    public void completeCheckoutSession(String sessionId) {
        // In production, webhooks handle checkout completion
        logger.debug(
                "Ignoring completeCheckoutSession - webhooks handle completion in production mode");
    }
}
