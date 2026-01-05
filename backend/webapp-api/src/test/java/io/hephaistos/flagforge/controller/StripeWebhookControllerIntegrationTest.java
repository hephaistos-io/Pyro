package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.MailpitTestConfiguration;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.RedisTestContainerConfiguration;
import io.hephaistos.flagforge.StripeTestContainerConfiguration;
import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.enums.PaymentStatus;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.common.enums.SubscriptionStatus;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.CompanyResponse;
import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CompanySubscriptionRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.PaymentHistoryRepository;
import io.hephaistos.flagforge.data.repository.SubscriptionItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestContainerConfiguration.class, RedisTestContainerConfiguration.class,
        MailpitTestConfiguration.class, StripeTestContainerConfiguration.class})
@Tag("integration")
class StripeWebhookControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private CompanySubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionItemRepository subscriptionItemRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        paymentHistoryRepository.deleteAll();
        subscriptionItemRepository.deleteAll();
        subscriptionRepository.deleteAll();
        environmentRepository.deleteAll();
        applicationRepository.deleteAll();
        companyRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void webhookEndpoint_returns400_withInvalidPayload() {
        var headers = createWebhookHeaders();
        var response = postWebhook("invalid json", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void webhookEndpoint_returns200_forUnhandledEventType() {
        String payload = createWebhookPayload("unhandled.event.type", "{}");
        var headers = createWebhookHeaders();

        var response = postWebhook(payload, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
    }

    @Test
    void checkoutCompleted_updatesEnvironmentToPaid() {
        // Setup: Create company, subscription, and environment
        String token = registerAndAuthenticateWithCompany();
        UUID companyId = getCompanyId(token);
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = createEnvironment(token, applicationId, "Staging");

        // Mark environment as PENDING (simulating pre-payment state)
        EnvironmentEntity env = environmentRepository.findById(environmentId).orElseThrow();
        env.setPaymentStatus(PaymentStatus.PENDING);
        environmentRepository.save(env);

        // Create subscription for the company
        CompanySubscriptionEntity subscription = new CompanySubscriptionEntity();
        subscription.setCompanyId(companyId);
        subscription.setStripeCustomerId("cus_test123");
        subscription.setStatus(SubscriptionStatus.INCOMPLETE);
        subscriptionRepository.save(subscription);

        // Create webhook payload with tier information
        String payload =
                createCheckoutCompletedPayload(companyId, environmentId, "BASIC", "sub_test456");
        var headers = createWebhookHeaders();

        var response = postWebhook(payload, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify environment is now PAID
        EnvironmentEntity updatedEnv = environmentRepository.findById(environmentId).orElseThrow();
        assertThat(updatedEnv.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        // Verify subscription is now ACTIVE
        CompanySubscriptionEntity updatedSub =
                subscriptionRepository.findByCompanyId(companyId).orElseThrow();
        assertThat(updatedSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updatedSub.getStripeSubscriptionId()).isEqualTo("sub_test456");

        // Verify subscription item was created
        var subscriptionItems =
                subscriptionItemRepository.findBySubscription_Id(updatedSub.getId());
        assertThat(subscriptionItems).hasSize(1);
        assertThat(subscriptionItems.getFirst().getEnvironmentId()).isEqualTo(environmentId);
    }

    @Test
    void checkoutCompleted_createsMultipleSubscriptionItems() {
        // Setup: Create company, subscription, and multiple environments
        String token = registerAndAuthenticateWithCompany();
        UUID companyId = getCompanyId(token);
        UUID applicationId = createApplication(token, "Test App");
        UUID env1Id = createEnvironment(token, applicationId, "Staging");
        UUID env2Id = createEnvironment(token, applicationId, "UAT");

        // Mark environments as PENDING
        environmentRepository.findById(env1Id).ifPresent(env -> {
            env.setPaymentStatus(PaymentStatus.PENDING);
            environmentRepository.save(env);
        });
        environmentRepository.findById(env2Id).ifPresent(env -> {
            env.setPaymentStatus(PaymentStatus.PENDING);
            environmentRepository.save(env);
        });

        // Create subscription for the company
        CompanySubscriptionEntity subscription = new CompanySubscriptionEntity();
        subscription.setCompanyId(companyId);
        subscription.setStripeCustomerId("cus_test123");
        subscription.setStatus(SubscriptionStatus.INCOMPLETE);
        subscriptionRepository.save(subscription);

        // Create webhook payload with multiple environments
        String payload =
                createCheckoutCompletedPayloadMultiple(companyId, new UUID[] {env1Id, env2Id},
                        new String[] {"BASIC", "PRO"}, "sub_test456");
        var headers = createWebhookHeaders();

        var response = postWebhook(payload, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify both environments are now PAID
        assertThat(environmentRepository.findById(env1Id).get().getPaymentStatus()).isEqualTo(
                PaymentStatus.PAID);
        assertThat(environmentRepository.findById(env2Id).get().getPaymentStatus()).isEqualTo(
                PaymentStatus.PAID);

        // Verify both subscription items were created
        CompanySubscriptionEntity updatedSub =
                subscriptionRepository.findByCompanyId(companyId).orElseThrow();
        var subscriptionItems =
                subscriptionItemRepository.findBySubscription_Id(updatedSub.getId());
        assertThat(subscriptionItems).hasSize(2);
    }

    @Test
    void invoicePaid_createsPaymentRecord() {
        // Setup: Create company with subscription
        String token = registerAndAuthenticateWithCompany();
        UUID companyId = getCompanyId(token);

        CompanySubscriptionEntity subscription = new CompanySubscriptionEntity();
        subscription.setCompanyId(companyId);
        subscription.setStripeCustomerId("cus_test123");
        subscription.setStripeSubscriptionId("sub_test456");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // Create webhook payload
        String payload = createInvoicePaidPayload("cus_test123", "in_test789", "ch_test000", 5000);
        var headers = createWebhookHeaders();

        var response = postWebhook(payload, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify payment record was created
        var payment = paymentHistoryRepository.findByStripeInvoiceId("in_test789");
        assertThat(payment).isPresent();
        assertThat(payment.get().getAmountCents()).isEqualTo(5000);
        assertThat(payment.get().getStatus()).isEqualTo("paid");
    }

    @Test
    void paymentFailed_updatesSubscriptionToPastDue() {
        // Setup: Create company with active subscription
        String token = registerAndAuthenticateWithCompany();
        UUID companyId = getCompanyId(token);

        CompanySubscriptionEntity subscription = new CompanySubscriptionEntity();
        subscription.setCompanyId(companyId);
        subscription.setStripeCustomerId("cus_test123");
        subscription.setStripeSubscriptionId("sub_test456");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // Create webhook payload
        String payload = createPaymentFailedPayload("cus_test123", "in_test789");
        var headers = createWebhookHeaders();

        var response = postWebhook(payload, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify subscription is now PAST_DUE
        CompanySubscriptionEntity updatedSub =
                subscriptionRepository.findByCompanyId(companyId).orElseThrow();
        assertThat(updatedSub.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void subscriptionDeleted_marksSubscriptionCanceled() {
        // Setup: Create company with active subscription
        String token = registerAndAuthenticateWithCompany();
        UUID companyId = getCompanyId(token);

        CompanySubscriptionEntity subscription = new CompanySubscriptionEntity();
        subscription.setCompanyId(companyId);
        subscription.setStripeCustomerId("cus_test123");
        subscription.setStripeSubscriptionId("sub_test456");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // Create webhook payload
        String payload = createSubscriptionDeletedPayload("sub_test456");
        var headers = createWebhookHeaders();

        var response = postWebhook(payload, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify subscription is now CANCELED
        CompanySubscriptionEntity updatedSub =
                subscriptionRepository.findByCompanyId(companyId).orElseThrow();
        assertThat(updatedSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    // ========== Helper Methods ==========

    private ResponseEntity<String> postWebhook(String payload, HttpHeaders headers) {
        return restTemplate.postForEntity(getBaseUrl() + "/webhooks/stripe",
                new HttpEntity<>(payload, headers), String.class);
    }

    private HttpHeaders createWebhookHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // In mock mode, signature verification is skipped
        headers.set("Stripe-Signature", "t=1234567890,v1=mock_signature");
        return headers;
    }

    private String createWebhookPayload(String eventType, String dataObject) {
        return String.format("""
                {
                    "id": "evt_test_%s",
                    "type": "%s",
                    "api_version": "2024-11-20.acacia",
                    "data": {
                        "object": %s
                    }
                }
                """, UUID.randomUUID().toString().substring(0, 8), eventType, dataObject);
    }

    private String createCheckoutCompletedPayload(UUID companyId, UUID environmentId,
            String tierName, String subscriptionId) {
        String sessionObject = String.format("""
                {
                    "id": "cs_test123",
                    "object": "checkout.session",
                    "subscription": "%s",
                    "metadata": {
                        "company_id": "%s",
                        "environment_ids": "%s",
                        "tier_names": "%s"
                    }
                }
                """, subscriptionId, companyId, environmentId, tierName);
        return createWebhookPayload("checkout.session.completed", sessionObject);
    }

    private String createCheckoutCompletedPayloadMultiple(UUID companyId, UUID[] environmentIds,
            String[] tierNames, String subscriptionId) {
        String envIdsStr = String.join(",",
                java.util.Arrays.stream(environmentIds).map(UUID::toString).toArray(String[]::new));
        String tierNamesStr = String.join(",", tierNames);
        String sessionObject = String.format("""
                {
                    "id": "cs_test123",
                    "object": "checkout.session",
                    "subscription": "%s",
                    "metadata": {
                        "company_id": "%s",
                        "environment_ids": "%s",
                        "tier_names": "%s"
                    }
                }
                """, subscriptionId, companyId, envIdsStr, tierNamesStr);
        return createWebhookPayload("checkout.session.completed", sessionObject);
    }

    private String createInvoicePaidPayload(String customerId, String invoiceId, String chargeId,
            int amountPaid) {
        String invoiceObject = String.format("""
                {
                    "id": "%s",
                    "object": "invoice",
                    "customer": "%s",
                    "charge": "%s",
                    "amount_paid": %d,
                    "currency": "usd"
                }
                """, invoiceId, customerId, chargeId, amountPaid);
        return createWebhookPayload("invoice.paid", invoiceObject);
    }

    private String createPaymentFailedPayload(String customerId, String invoiceId) {
        String invoiceObject = String.format("""
                {
                    "id": "%s",
                    "object": "invoice",
                    "customer": "%s"
                }
                """, invoiceId, customerId);
        return createWebhookPayload("invoice.payment_failed", invoiceObject);
    }

    private String createSubscriptionDeletedPayload(String subscriptionId) {
        String subscriptionObject = String.format("""
                {
                    "id": "%s",
                    "object": "subscription",
                    "status": "canceled"
                }
                """, subscriptionId);
        return createWebhookPayload("customer.subscription.deleted", subscriptionObject);
    }

    private UUID createApplication(String token, String name) {
        var response = post("/v1/applications", new ApplicationCreationRequest(name), token,
                ApplicationResponse.class);
        return response.getBody().id();
    }

    private UUID createEnvironment(String token, UUID applicationId, String name) {
        var response = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest(name, "Test environment", PricingTier.BASIC), token,
                EnvironmentResponse.class);
        return response.getBody().id();
    }

    private UUID getCompanyId(String token) {
        var response = get("/v1/company", token, CompanyResponse.class);
        return response.getBody().id();
    }
}
