package io.hephaistos.flagforge.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Dispute;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.checkout.Session;
import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import io.hephaistos.flagforge.common.data.DisputeEvidenceEntity;
import io.hephaistos.flagforge.common.data.PaymentHistoryEntity;
import io.hephaistos.flagforge.common.data.SubscriptionItemEntity;
import io.hephaistos.flagforge.common.enums.PaymentStatus;
import io.hephaistos.flagforge.common.enums.SubscriptionStatus;
import io.hephaistos.flagforge.configuration.StripeConfiguration;
import io.hephaistos.flagforge.data.repository.CompanySubscriptionRepository;
import io.hephaistos.flagforge.data.repository.DisputeEvidenceRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.PaymentHistoryRepository;
import io.hephaistos.flagforge.data.repository.SubscriptionItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles Stripe webhook events.
 * <p>
 * Processes events asynchronously to avoid blocking Stripe's webhook delivery. All database
 * operations are transactional to ensure consistency.
 */
@Service
@Transactional
public class StripeWebhookHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StripeWebhookHandler.class);

    private final CompanySubscriptionRepository subscriptionRepository;
    private final SubscriptionItemRepository subscriptionItemRepository;
    private final EnvironmentRepository environmentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final DisputeEvidenceRepository disputeEvidenceRepository;
    private final DisputeEvidenceService evidenceService;
    private final StripeConfiguration config;

    public StripeWebhookHandler(CompanySubscriptionRepository subscriptionRepository,
            SubscriptionItemRepository subscriptionItemRepository,
            EnvironmentRepository environmentRepository,
            PaymentHistoryRepository paymentHistoryRepository,
            DisputeEvidenceRepository disputeEvidenceRepository,
            DisputeEvidenceService evidenceService, StripeConfiguration config) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionItemRepository = subscriptionItemRepository;
        this.environmentRepository = environmentRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.disputeEvidenceRepository = disputeEvidenceRepository;
        this.evidenceService = evidenceService;
        this.config = config;
    }

    /**
     * Handles checkout.session.completed event. Updates environments to PAID status, links
     * subscription, and creates subscription items.
     */
    public void handleCheckoutCompleted(Event event) {
        Session session = deserializeObject(event, Session.class);
        if (session == null) {
            LOGGER.warn("Could not deserialize checkout session from event {}", event.getId());
            return;
        }

        String companyIdStr = session.getMetadata().get("company_id");
        String environmentIdsStr = session.getMetadata().get("environment_ids");
        String tierNamesStr = session.getMetadata().get("tier_names");

        if (companyIdStr == null || environmentIdsStr == null) {
            LOGGER.warn("Missing metadata in checkout session {}", session.getId());
            return;
        }

        UUID companyId = UUID.fromString(companyIdStr);

        // Parse environment IDs and tier names (in matching order)
        List<UUID> environmentIds = Arrays.stream(environmentIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();

        List<String> tierNames = tierNamesStr != null ?
                Arrays.stream(tierNamesStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList() :
                List.of();

        // Update environments to PAID status
        environmentIds.forEach(envId -> {
            environmentRepository.findById(envId).ifPresent(env -> {
                env.setPaymentStatus(PaymentStatus.PAID);
                environmentRepository.save(env);
                LOGGER.info("Environment {} marked as PAID", envId);
            });
        });

        // Update subscription record and create subscription items
        String stripeSubscriptionId = session.getSubscription();
        subscriptionRepository.findByCompanyId(companyId).ifPresent(sub -> {
            sub.setStripeSubscriptionId(stripeSubscriptionId);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.touch();
            subscriptionRepository.save(sub);
            LOGGER.info("Subscription activated for company {}", companyId);

            // Create subscription item records
            createSubscriptionItems(sub, stripeSubscriptionId, environmentIds, tierNames);
        });

        LOGGER.info("Checkout completed for session {}, company {}", session.getId(), companyId);
    }

    /**
     * Creates SubscriptionItemEntity records by fetching subscription items from Stripe. In mock
     * mode, uses metadata directly since stripe-mock doesn't track session items.
     */
    private void createSubscriptionItems(CompanySubscriptionEntity subscription,
            String stripeSubscriptionId, List<UUID> environmentIds, List<String> tierNames) {

        if (stripeSubscriptionId == null || environmentIds.isEmpty()) {
            LOGGER.debug("No subscription items to create");
            return;
        }

        // In mock mode, stripe-mock doesn't track session items correctly,
        // so use metadata directly
        if (config.isMock()) {
            LOGGER.info("Mock mode enabled, creating subscription items from metadata");
            createSubscriptionItemsFromMetadata(subscription, environmentIds, tierNames);
            return;
        }

        try {
            // Fetch the subscription from Stripe to get the actual item IDs
            Subscription stripeSubscription = Subscription.retrieve(stripeSubscriptionId);
            SubscriptionItemCollection items = stripeSubscription.getItems();

            if (items == null || items.getData() == null) {
                LOGGER.info("No Stripe items available, creating from metadata");
                createSubscriptionItemsFromMetadata(subscription, environmentIds, tierNames);
                return;
            }

            List<SubscriptionItem> stripeItems = items.getData();

            // Match Stripe items to environments by order (both lists are in same order)
            int itemCount = Math.min(stripeItems.size(), environmentIds.size());
            for (int i = 0; i < itemCount; i++) {
                SubscriptionItem stripeItem = stripeItems.get(i);
                UUID environmentId = environmentIds.get(i);

                // Check if already exists
                if (subscriptionItemRepository.findByEnvironmentId(environmentId).isPresent()) {
                    LOGGER.debug("Subscription item already exists for environment {}",
                            environmentId);
                    continue;
                }

                SubscriptionItemEntity itemEntity = new SubscriptionItemEntity();
                itemEntity.setSubscription(subscription);
                itemEntity.setEnvironmentId(environmentId);
                itemEntity.setStripeSubscriptionItemId(stripeItem.getId());
                itemEntity.setStripePriceId(stripeItem.getPrice().getId());
                subscriptionItemRepository.save(itemEntity);

                LOGGER.info("Created subscription item for environment {} with Stripe item {}",
                        environmentId, stripeItem.getId());
            }

        }
        catch (StripeException e) {
            LOGGER.error("Failed to fetch subscription items from Stripe, falling back to metadata",
                    e);
            createSubscriptionItemsFromMetadata(subscription, environmentIds, tierNames);
        }
    }

    /**
     * Creates subscription items using metadata when Stripe API is unavailable (mock mode).
     */
    private void createSubscriptionItemsFromMetadata(CompanySubscriptionEntity subscription,
            List<UUID> environmentIds, List<String> tierNames) {

        for (int i = 0; i < environmentIds.size(); i++) {
            UUID environmentId = environmentIds.get(i);
            String tierName = i < tierNames.size() ? tierNames.get(i) : "UNKNOWN";

            // Check if already exists
            if (subscriptionItemRepository.findByEnvironmentId(environmentId).isPresent()) {
                LOGGER.debug("Subscription item already exists for environment {}", environmentId);
                continue;
            }

            SubscriptionItemEntity itemEntity = new SubscriptionItemEntity();
            itemEntity.setSubscription(subscription);
            itemEntity.setEnvironmentId(environmentId);
            // Generate mock IDs for local development
            itemEntity.setStripeSubscriptionItemId(
                    "si_mock_" + environmentId.toString().substring(0, 8));
            itemEntity.setStripePriceId(config.getPriceIdForTier(tierName));
            subscriptionItemRepository.save(itemEntity);

            LOGGER.info("Created subscription item (from metadata) for environment {} tier {}",
                    environmentId, tierName);
        }
    }

    /**
     * Handles customer.subscription.created event.
     */
    public void handleSubscriptionCreated(Event event) {
        Subscription subscription = deserializeObject(event, Subscription.class);
        if (subscription == null)
            return;

        updateSubscriptionFromStripe(subscription);
        LOGGER.info("Subscription created: {}", subscription.getId());
    }

    /**
     * Handles customer.subscription.updated event.
     */
    public void handleSubscriptionUpdated(Event event) {
        Subscription subscription = deserializeObject(event, Subscription.class);
        if (subscription == null)
            return;

        updateSubscriptionFromStripe(subscription);
        LOGGER.info("Subscription updated: {}", subscription.getId());
    }

    /**
     * Handles customer.subscription.deleted event.
     */
    public void handleSubscriptionDeleted(Event event) {
        Subscription subscription = deserializeObject(event, Subscription.class);
        if (subscription == null)
            return;

        subscriptionRepository.findByStripeSubscriptionId(subscription.getId()).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.CANCELED);
            sub.touch();
            subscriptionRepository.save(sub);
            LOGGER.info("Subscription cancelled: {}", subscription.getId());
        });
    }

    private void updateSubscriptionFromStripe(Subscription subscription) {
        subscriptionRepository.findByStripeSubscriptionId(subscription.getId()).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.fromStripeStatus(subscription.getStatus()));
            if (subscription.getCurrentPeriodStart() != null) {
                sub.setCurrentPeriodStart(
                        Instant.ofEpochSecond(subscription.getCurrentPeriodStart()));
            }
            if (subscription.getCurrentPeriodEnd() != null) {
                sub.setCurrentPeriodEnd(Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()));
            }
            sub.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
            sub.touch();
            subscriptionRepository.save(sub);
        });
    }

    /**
     * Handles invoice.paid event. Records payment in history.
     */
    public void handleInvoicePaid(Event event) {
        Invoice invoice = deserializeObject(event, Invoice.class);
        if (invoice == null)
            return;

        // Skip if we already recorded this invoice
        if (paymentHistoryRepository.existsByStripeInvoiceId(invoice.getId())) {
            LOGGER.debug("Invoice {} already recorded", invoice.getId());
            return;
        }

        // Find company from customer
        String customerId = invoice.getCustomer();
        Optional<CompanySubscriptionEntity> subscriptionOpt =
                subscriptionRepository.findByStripeCustomerId(customerId);

        if (subscriptionOpt.isEmpty()) {
            LOGGER.warn("No subscription found for customer {}", customerId);
            return;
        }

        CompanySubscriptionEntity subscription = subscriptionOpt.get();

        PaymentHistoryEntity payment = new PaymentHistoryEntity();
        payment.setCompanyId(subscription.getCompanyId());
        payment.setStripeInvoiceId(invoice.getId());
        payment.setStripeChargeId(invoice.getCharge());
        payment.setAmountCents(
                invoice.getAmountPaid() != null ? invoice.getAmountPaid().intValue() : 0);
        payment.setCurrency(invoice.getCurrency());
        payment.setStatus("paid");
        payment.setInvoicePdfUrl(invoice.getInvoicePdf());
        payment.setHostedInvoiceUrl(invoice.getHostedInvoiceUrl());
        if (invoice.getPeriodStart() != null) {
            payment.setPeriodStart(Instant.ofEpochSecond(invoice.getPeriodStart()));
        }
        if (invoice.getPeriodEnd() != null) {
            payment.setPeriodEnd(Instant.ofEpochSecond(invoice.getPeriodEnd()));
        }
        payment.setPaidAt(Instant.now());

        paymentHistoryRepository.save(payment);
        LOGGER.info("Recorded payment for invoice {}", invoice.getId());
    }

    /**
     * Handles invoice.payment_failed event. Updates subscription status and notifies customer.
     */
    public void handlePaymentFailed(Event event) {
        Invoice invoice = deserializeObject(event, Invoice.class);
        if (invoice == null)
            return;

        String customerId = invoice.getCustomer();
        subscriptionRepository.findByStripeCustomerId(customerId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.PAST_DUE);
            sub.touch();
            subscriptionRepository.save(sub);
            LOGGER.warn("Payment failed for invoice {}, company marked as PAST_DUE",
                    invoice.getId());

            // TODO: Send notification email to customer about failed payment
        });
    }

    /**
     * Handles charge.dispute.created event. Automatically collects and submits evidence.
     */
    public void handleDisputeCreated(Event event) {
        Dispute dispute = deserializeObject(event, Dispute.class);
        if (dispute == null)
            return;

        String chargeId = dispute.getCharge();
        UUID companyId = findCompanyFromCharge(chargeId);

        if (companyId == null) {
            LOGGER.warn("Could not find company for disputed charge {}", chargeId);
            return;
        }

        // Create dispute record
        DisputeEvidenceEntity disputeEntity = new DisputeEvidenceEntity();
        disputeEntity.setCompanyId(companyId);
        disputeEntity.setStripeDisputeId(dispute.getId());
        disputeEntity.setStripeChargeId(chargeId);
        disputeEntity.setAmountCents(
                dispute.getAmount() != null ? dispute.getAmount().intValue() : 0);
        disputeEntity.setReason(dispute.getReason());
        disputeEntity.setStatus(dispute.getStatus());

        // Collect evidence
        try {
            DisputeEvidenceService.DisputeEvidence evidence =
                    evidenceService.collectEvidence(companyId, chargeId);

            disputeEntity.setCustomerEmail(evidence.customerEmail());
            disputeEntity.setCustomerName(evidence.customerName());
            disputeEntity.setProductDescription(evidence.productDescription());
            disputeEntity.setUsageLogs(evidence.usageLogs());
            disputeEntity.setServiceDocumentationUrl(evidence.serviceDocumentationUrl());

            disputeEvidenceRepository.save(disputeEntity);

            // Auto-submit evidence
            evidenceService.submitEvidence(dispute.getId(), evidence);
            disputeEntity.setEvidenceSubmitted(true);
            disputeEntity.touch();
            disputeEvidenceRepository.save(disputeEntity);

            LOGGER.info("Evidence collected and submitted for dispute {}", dispute.getId());

        }
        catch (Exception e) {
            LOGGER.error("Failed to collect/submit evidence for dispute {}", dispute.getId(), e);
            disputeEvidenceRepository.save(disputeEntity);
        }
    }

    /**
     * Handles charge.dispute.updated event.
     */
    public void handleDisputeUpdated(Event event) {
        Dispute dispute = deserializeObject(event, Dispute.class);
        if (dispute == null)
            return;

        disputeEvidenceRepository.findByStripeDisputeId(dispute.getId()).ifPresent(entity -> {
            entity.setStatus(dispute.getStatus());
            entity.touch();
            disputeEvidenceRepository.save(entity);
            LOGGER.info("Dispute {} updated to status {}", dispute.getId(), dispute.getStatus());
        });
    }

    /**
     * Handles charge.dispute.closed event.
     */
    public void handleDisputeClosed(Event event) {
        Dispute dispute = deserializeObject(event, Dispute.class);
        if (dispute == null)
            return;

        disputeEvidenceRepository.findByStripeDisputeId(dispute.getId()).ifPresent(entity -> {
            entity.setStatus(dispute.getStatus());
            entity.touch();
            disputeEvidenceRepository.save(entity);
            LOGGER.info("Dispute {} closed with status {}", dispute.getId(), dispute.getStatus());
        });
    }

    /**
     * Finds the company ID from a charge ID by looking up payment history.
     */
    private UUID findCompanyFromCharge(String chargeId) {
        return paymentHistoryRepository.findByStripeChargeId(chargeId)
                .map(PaymentHistoryEntity::getCompanyId)
                .orElse(null);
    }

    /**
     * Helper to deserialize event data objects.
     */
    @SuppressWarnings("unchecked")
    private <T extends StripeObject> T deserializeObject(Event event, Class<T> clazz) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            if (clazz.isInstance(obj)) {
                return (T) obj;
            }
        }
        return null;
    }
}
