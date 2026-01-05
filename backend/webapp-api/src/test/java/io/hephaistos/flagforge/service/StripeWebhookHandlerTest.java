package io.hephaistos.flagforge.service;

import com.stripe.model.Dispute;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import io.hephaistos.flagforge.common.data.DisputeEvidenceEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.data.PaymentHistoryEntity;
import io.hephaistos.flagforge.common.enums.PaymentStatus;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.common.enums.SubscriptionStatus;
import io.hephaistos.flagforge.configuration.StripeConfiguration;
import io.hephaistos.flagforge.data.repository.CompanySubscriptionRepository;
import io.hephaistos.flagforge.data.repository.DisputeEvidenceRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.PaymentHistoryRepository;
import io.hephaistos.flagforge.data.repository.SubscriptionItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StripeWebhookHandler.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StripeWebhookHandlerTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID ENVIRONMENT_ID = UUID.randomUUID();
    private static final String STRIPE_CUSTOMER_ID = "cus_test123";
    private static final String STRIPE_SUBSCRIPTION_ID = "sub_test456";

    @Mock
    private CompanySubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionItemRepository subscriptionItemRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @Mock
    private DisputeEvidenceRepository disputeEvidenceRepository;

    @Mock
    private DisputeEvidenceService evidenceService;

    @Mock
    private StripeConfiguration config;

    private StripeWebhookHandler webhookHandler;

    @BeforeEach
    void setUp() {
        webhookHandler =
                new StripeWebhookHandler(subscriptionRepository, subscriptionItemRepository,
                        environmentRepository, paymentHistoryRepository, disputeEvidenceRepository,
                        evidenceService, config);
    }

    // ========== Checkout Completed Tests ==========

    @Test
    void handleCheckoutCompleted_updatesEnvironmentToPaid() {
        Event event = mockEventWithObject(createMockSession());
        EnvironmentEntity environment = createTestEnvironment();
        CompanySubscriptionEntity subscription = createTestSubscription();

        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));

        webhookHandler.handleCheckoutCompleted(event);

        ArgumentCaptor<EnvironmentEntity> envCaptor =
                ArgumentCaptor.forClass(EnvironmentEntity.class);
        verify(environmentRepository).save(envCaptor.capture());
        assertThat(envCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void handleCheckoutCompleted_activatesSubscription() {
        Event event = mockEventWithObject(createMockSession());
        EnvironmentEntity environment = createTestEnvironment();
        CompanySubscriptionEntity subscription = createTestSubscription();
        subscription.setStatus(SubscriptionStatus.INCOMPLETE);

        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));

        webhookHandler.handleCheckoutCompleted(event);

        ArgumentCaptor<CompanySubscriptionEntity> subCaptor =
                ArgumentCaptor.forClass(CompanySubscriptionEntity.class);
        verify(subscriptionRepository).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void handleCheckoutCompleted_handlesMultipleEnvironments() {
        UUID env1Id = UUID.randomUUID();
        UUID env2Id = UUID.randomUUID();

        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test");
        when(session.getSubscription()).thenReturn(STRIPE_SUBSCRIPTION_ID);
        when(session.getMetadata()).thenReturn(
                Map.of("company_id", COMPANY_ID.toString(), "environment_ids",
                        env1Id + "," + env2Id));

        Event event = mockEventWithObject(session);

        EnvironmentEntity env1 = new EnvironmentEntity();
        env1.setId(env1Id);
        env1.setPaymentStatus(PaymentStatus.PENDING);

        EnvironmentEntity env2 = new EnvironmentEntity();
        env2.setId(env2Id);
        env2.setPaymentStatus(PaymentStatus.PENDING);

        CompanySubscriptionEntity subscription = createTestSubscription();

        when(environmentRepository.findById(env1Id)).thenReturn(Optional.of(env1));
        when(environmentRepository.findById(env2Id)).thenReturn(Optional.of(env2));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));

        webhookHandler.handleCheckoutCompleted(event);

        verify(environmentRepository).findById(env1Id);
        verify(environmentRepository).findById(env2Id);
    }

    @Test
    void handleCheckoutCompleted_skipsWithMissingMetadata() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test");
        when(session.getMetadata()).thenReturn(Map.of());

        Event event = mockEventWithObject(session);

        webhookHandler.handleCheckoutCompleted(event);

        verify(environmentRepository, never()).findById(any());
        verify(subscriptionRepository, never()).findByCompanyId(any());
    }

    // ========== Subscription Event Tests ==========

    @Test
    void handleSubscriptionCreated_updatesLocalSubscription() {
        Subscription stripeSubscription = createMockStripeSubscription("active");
        Event event = mockEventWithObject(stripeSubscription);

        CompanySubscriptionEntity subscription = createTestSubscription();
        when(subscriptionRepository.findByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID)).thenReturn(
                Optional.of(subscription));

        webhookHandler.handleSubscriptionCreated(event);

        ArgumentCaptor<CompanySubscriptionEntity> captor =
                ArgumentCaptor.forClass(CompanySubscriptionEntity.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void handleSubscriptionUpdated_syncsStatus() {
        Subscription stripeSubscription = createMockStripeSubscription("past_due");
        Event event = mockEventWithObject(stripeSubscription);

        CompanySubscriptionEntity subscription = createTestSubscription();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID)).thenReturn(
                Optional.of(subscription));

        webhookHandler.handleSubscriptionUpdated(event);

        ArgumentCaptor<CompanySubscriptionEntity> captor =
                ArgumentCaptor.forClass(CompanySubscriptionEntity.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void handleSubscriptionDeleted_marksCanceled() {
        Subscription stripeSubscription = createMockStripeSubscription("canceled");
        Event event = mockEventWithObject(stripeSubscription);

        CompanySubscriptionEntity subscription = createTestSubscription();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID)).thenReturn(
                Optional.of(subscription));

        webhookHandler.handleSubscriptionDeleted(event);

        ArgumentCaptor<CompanySubscriptionEntity> captor =
                ArgumentCaptor.forClass(CompanySubscriptionEntity.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    void handleSubscriptionUpdated_syncsPeriodDates() {
        Subscription stripeSubscription = createMockStripeSubscription("active");
        long periodStart = Instant.now().getEpochSecond();
        long periodEnd = Instant.now().plusSeconds(2592000).getEpochSecond(); // +30 days
        when(stripeSubscription.getCurrentPeriodStart()).thenReturn(periodStart);
        when(stripeSubscription.getCurrentPeriodEnd()).thenReturn(periodEnd);
        when(stripeSubscription.getCancelAtPeriodEnd()).thenReturn(true);

        Event event = mockEventWithObject(stripeSubscription);

        CompanySubscriptionEntity subscription = createTestSubscription();
        when(subscriptionRepository.findByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID)).thenReturn(
                Optional.of(subscription));

        webhookHandler.handleSubscriptionUpdated(event);

        ArgumentCaptor<CompanySubscriptionEntity> captor =
                ArgumentCaptor.forClass(CompanySubscriptionEntity.class);
        verify(subscriptionRepository).save(captor.capture());

        CompanySubscriptionEntity saved = captor.getValue();
        assertThat(saved.getCurrentPeriodStart()).isNotNull();
        assertThat(saved.getCurrentPeriodEnd()).isNotNull();
        assertThat(saved.getCancelAtPeriodEnd()).isTrue();
    }

    // ========== Invoice Event Tests ==========

    @Test
    void handleInvoicePaid_createsPaymentRecord() {
        Invoice invoice = createMockInvoice();
        Event event = mockEventWithObject(invoice);

        CompanySubscriptionEntity subscription = createTestSubscription();
        when(paymentHistoryRepository.existsByStripeInvoiceId("in_test")).thenReturn(false);
        when(subscriptionRepository.findByStripeCustomerId(STRIPE_CUSTOMER_ID)).thenReturn(
                Optional.of(subscription));

        webhookHandler.handleInvoicePaid(event);

        ArgumentCaptor<PaymentHistoryEntity> captor =
                ArgumentCaptor.forClass(PaymentHistoryEntity.class);
        verify(paymentHistoryRepository).save(captor.capture());

        PaymentHistoryEntity payment = captor.getValue();
        assertThat(payment.getStripeInvoiceId()).isEqualTo("in_test");
        assertThat(payment.getStripeChargeId()).isEqualTo("ch_test");
        assertThat(payment.getAmountCents()).isEqualTo(5000);
        assertThat(payment.getStatus()).isEqualTo("paid");
    }

    @Test
    void handleInvoicePaid_skipsDuplicate() {
        Invoice invoice = createMockInvoice();
        Event event = mockEventWithObject(invoice);

        when(paymentHistoryRepository.existsByStripeInvoiceId("in_test")).thenReturn(true);

        webhookHandler.handleInvoicePaid(event);

        verify(subscriptionRepository, never()).findByStripeCustomerId(any());
        verify(paymentHistoryRepository, never()).save(any());
    }

    @Test
    void handlePaymentFailed_updatesStatusToPastDue() {
        Invoice invoice = createMockInvoice();
        Event event = mockEventWithObject(invoice);

        CompanySubscriptionEntity subscription = createTestSubscription();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByStripeCustomerId(STRIPE_CUSTOMER_ID)).thenReturn(
                Optional.of(subscription));

        webhookHandler.handlePaymentFailed(event);

        ArgumentCaptor<CompanySubscriptionEntity> captor =
                ArgumentCaptor.forClass(CompanySubscriptionEntity.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    // ========== Dispute Event Tests ==========

    @Test
    void handleDisputeCreated_createsDisputeRecord() {
        Dispute dispute = createMockDispute();
        Event event = mockEventWithObject(dispute);

        PaymentHistoryEntity payment = new PaymentHistoryEntity();
        payment.setCompanyId(COMPANY_ID);
        when(paymentHistoryRepository.findByStripeChargeId("ch_disputed")).thenReturn(
                Optional.of(payment));

        DisputeEvidenceService.DisputeEvidence evidence =
                new DisputeEvidenceService.DisputeEvidence("customer@test.com", "Test Customer",
                        "Product description", "Usage logs", "https://docs.example.com");
        when(evidenceService.collectEvidence(COMPANY_ID, "ch_disputed")).thenReturn(evidence);

        webhookHandler.handleDisputeCreated(event);

        // The handler saves twice: once after collecting evidence, once after submitting
        ArgumentCaptor<DisputeEvidenceEntity> captor =
                ArgumentCaptor.forClass(DisputeEvidenceEntity.class);
        verify(disputeEvidenceRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        DisputeEvidenceEntity saved = captor.getValue();
        assertThat(saved.getStripeDisputeId()).isEqualTo("dp_test");
        assertThat(saved.getStripeChargeId()).isEqualTo("ch_disputed");
        assertThat(saved.getAmountCents()).isEqualTo(5000);
        assertThat(saved.getReason()).isEqualTo("fraudulent");
    }

    @Test
    void handleDisputeCreated_submitsEvidence() {
        Dispute dispute = createMockDispute();
        Event event = mockEventWithObject(dispute);

        PaymentHistoryEntity payment = new PaymentHistoryEntity();
        payment.setCompanyId(COMPANY_ID);
        when(paymentHistoryRepository.findByStripeChargeId("ch_disputed")).thenReturn(
                Optional.of(payment));

        DisputeEvidenceService.DisputeEvidence evidence =
                new DisputeEvidenceService.DisputeEvidence("customer@test.com", "Test Customer",
                        "Product description", "Usage logs", "https://docs.example.com");
        when(evidenceService.collectEvidence(COMPANY_ID, "ch_disputed")).thenReturn(evidence);

        webhookHandler.handleDisputeCreated(event);

        verify(evidenceService).submitEvidence("dp_test", evidence);
    }

    @Test
    void handleDisputeCreated_handlesUnknownCharge() {
        Dispute dispute = createMockDispute();
        Event event = mockEventWithObject(dispute);

        when(paymentHistoryRepository.findByStripeChargeId("ch_disputed")).thenReturn(
                Optional.empty());

        webhookHandler.handleDisputeCreated(event);

        verify(evidenceService, never()).collectEvidence(any(), any());
        verify(disputeEvidenceRepository, never()).save(any());
    }

    @Test
    void handleDisputeUpdated_updatesStatus() {
        Dispute dispute = createMockDispute();
        when(dispute.getStatus()).thenReturn("under_review");
        Event event = mockEventWithObject(dispute);

        DisputeEvidenceEntity existingDispute = new DisputeEvidenceEntity();
        existingDispute.setStripeDisputeId("dp_test");
        existingDispute.setStatus("needs_response");
        when(disputeEvidenceRepository.findByStripeDisputeId("dp_test")).thenReturn(
                Optional.of(existingDispute));

        webhookHandler.handleDisputeUpdated(event);

        ArgumentCaptor<DisputeEvidenceEntity> captor =
                ArgumentCaptor.forClass(DisputeEvidenceEntity.class);
        verify(disputeEvidenceRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("under_review");
    }

    @Test
    void handleDisputeClosed_updatesStatus() {
        Dispute dispute = createMockDispute();
        when(dispute.getStatus()).thenReturn("won");
        Event event = mockEventWithObject(dispute);

        DisputeEvidenceEntity existingDispute = new DisputeEvidenceEntity();
        existingDispute.setStripeDisputeId("dp_test");
        existingDispute.setStatus("under_review");
        when(disputeEvidenceRepository.findByStripeDisputeId("dp_test")).thenReturn(
                Optional.of(existingDispute));

        webhookHandler.handleDisputeClosed(event);

        ArgumentCaptor<DisputeEvidenceEntity> captor =
                ArgumentCaptor.forClass(DisputeEvidenceEntity.class);
        verify(disputeEvidenceRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("won");
    }

    // ========== Helper Methods ==========

    @SuppressWarnings("unchecked")
    private <T extends com.stripe.model.StripeObject> Event mockEventWithObject(T object) {
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(object));

        return event;
    }

    private Session createMockSession() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test");
        when(session.getSubscription()).thenReturn(STRIPE_SUBSCRIPTION_ID);
        when(session.getMetadata()).thenReturn(
                Map.of("company_id", COMPANY_ID.toString(), "environment_ids",
                        ENVIRONMENT_ID.toString()));
        return session;
    }

    private Subscription createMockStripeSubscription(String status) {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn(STRIPE_SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn(status);
        return subscription;
    }

    private Invoice createMockInvoice() {
        Invoice invoice = mock(Invoice.class);
        when(invoice.getId()).thenReturn("in_test");
        when(invoice.getCustomer()).thenReturn(STRIPE_CUSTOMER_ID);
        when(invoice.getCharge()).thenReturn("ch_test");
        when(invoice.getAmountPaid()).thenReturn(5000L);
        when(invoice.getCurrency()).thenReturn("usd");
        return invoice;
    }

    private Dispute createMockDispute() {
        Dispute dispute = mock(Dispute.class);
        when(dispute.getId()).thenReturn("dp_test");
        when(dispute.getCharge()).thenReturn("ch_disputed");
        when(dispute.getAmount()).thenReturn(5000L);
        when(dispute.getReason()).thenReturn("fraudulent");
        when(dispute.getStatus()).thenReturn("needs_response");
        return dispute;
    }

    private CompanySubscriptionEntity createTestSubscription() {
        CompanySubscriptionEntity subscription = new CompanySubscriptionEntity();
        subscription.setId(UUID.randomUUID());
        subscription.setCompanyId(COMPANY_ID);
        subscription.setStripeCustomerId(STRIPE_CUSTOMER_ID);
        subscription.setStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }

    private EnvironmentEntity createTestEnvironment() {
        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setName("Production");
        environment.setTier(PricingTier.PRO);
        environment.setPaymentStatus(PaymentStatus.PENDING);
        return environment;
    }
}
