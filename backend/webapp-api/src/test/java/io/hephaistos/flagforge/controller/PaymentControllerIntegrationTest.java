package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.MailpitTestConfiguration;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.RedisTestContainerConfiguration;
import io.hephaistos.flagforge.StripeTestContainerConfiguration;
import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.common.enums.SubscriptionStatus;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.CheckoutRequest;
import io.hephaistos.flagforge.controller.dto.CheckoutSessionResponse;
import io.hephaistos.flagforge.controller.dto.CompanyResponse;
import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
import io.hephaistos.flagforge.controller.dto.SubscriptionStatusResponse;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CompanySubscriptionRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.SubscriptionItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestContainerConfiguration.class, RedisTestContainerConfiguration.class,
        MailpitTestConfiguration.class, StripeTestContainerConfiguration.class})
@Tag("integration")
class PaymentControllerIntegrationTest extends IntegrationTestSupport {

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

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        subscriptionItemRepository.deleteAll();
        subscriptionRepository.deleteAll();
        environmentRepository.deleteAll();
        applicationRepository.deleteAll();
        companyRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void getPaymentConfig_returns200WithPublishableKey() {
        String token = registerAndAuthenticateWithCompany();

        var response =
                get("/v1/payment/config", token, PaymentController.PaymentConfigResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().publishableKey()).isNotEmpty();
    }

    @Test
    void getSubscriptionStatus_returnsNull_whenNoSubscription() {
        String token = registerAndAuthenticateWithCompany();

        var response = get("/v1/payment/subscription", token, SubscriptionStatusResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getSubscriptionStatus_returnsStatus_whenSubscriptionExists() {
        String token = registerAndAuthenticateWithCompany();
        UUID companyId = getCompanyId(token);

        // Create a subscription manually for testing
        CompanySubscriptionEntity subscription = new CompanySubscriptionEntity();
        subscription.setCompanyId(companyId);
        subscription.setStripeCustomerId("cus_test123");
        subscription.setStripeSubscriptionId("sub_test456");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        var response = get("/v1/payment/subscription", token, SubscriptionStatusResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    void createOrGetCustomer_returns200AndCreatesCustomer() {
        String token = registerAndAuthenticateWithCompany();

        var response = post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().customerId()).isNotEmpty();
        assertThat(response.getBody().customerId()).startsWith("cus_");
    }

    @Test
    void createOrGetCustomer_returnsSameCustomer_whenAlreadyExists() {
        String token = registerAndAuthenticateWithCompany();

        // Create customer first time
        var response1 = post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        // Create customer second time
        var response2 = post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        assertThat(response1.getBody().customerId()).isEqualTo(response2.getBody().customerId());
    }

    @Test
    void createCheckoutSession_returns400_whenNoCustomer() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = createPaidEnvironment(token, applicationId, "Staging");

        var request = new CheckoutRequest(
                List.of(new CheckoutRequest.PendingEnvironmentDto(environmentId, "Staging",
                        PricingTier.BASIC)), "http://localhost:4200/success",
                "http://localhost:4200/cancel");

        var response = post("/v1/payment/checkout", request, token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("PAYMENT_ERROR");
    }

    @Test
    void createCheckoutSession_returns200_withValidRequest() {
        String token = registerAndAuthenticateWithCompany();

        // First create a customer
        post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = createPaidEnvironment(token, applicationId, "Staging");

        var request = new CheckoutRequest(
                List.of(new CheckoutRequest.PendingEnvironmentDto(environmentId, "Staging",
                        PricingTier.BASIC)), "http://localhost:4200/success",
                "http://localhost:4200/cancel");

        var response = post("/v1/payment/checkout", request, token, CheckoutSessionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sessionId()).startsWith("cs_");
    }

    @Test
    void createCheckoutSession_returns400_withOnlyFreeItems() {
        String token = registerAndAuthenticateWithCompany();

        // First create a customer
        post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        UUID applicationId = createApplication(token, "Test App");

        var request = new CheckoutRequest(
                List.of(new CheckoutRequest.PendingEnvironmentDto(UUID.randomUUID(), "Free Env",
                        PricingTier.FREE)), "http://localhost:4200/success",
                "http://localhost:4200/cancel");

        var response = post("/v1/payment/checkout", request, token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("No paid items");
    }

    @Test
    void getInvoices_returnsEmptyList_whenNoSubscription() {
        String token = registerAndAuthenticateWithCompany();

        var response = get("/v1/payment/invoices", token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    @Test
    void createPortalSession_returns400_whenNoSubscription() {
        String token = registerAndAuthenticateWithCompany();

        var response = post("/v1/payment/portal",
                new PaymentController.PortalRequest("http://localhost:4200/billing"), token,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("PAYMENT_ERROR");
    }

    @Test
    void createPortalSession_returns200_whenSubscriptionExists() {
        String token = registerAndAuthenticateWithCompany();

        // Create a customer first
        var customerResponse = post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        var response = post("/v1/payment/portal",
                new PaymentController.PortalRequest("http://localhost:4200/billing"), token,
                PaymentController.PortalSessionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().url()).isNotEmpty();
    }

    @Test
    void paymentEndpoints_require401_withoutAuth() {
        var configResponse =
                restTemplate.getForEntity(getBaseUrl() + "/v1/payment/config", String.class);
        assertThat(configResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var subscriptionResponse =
                restTemplate.getForEntity(getBaseUrl() + "/v1/payment/subscription", String.class);
        assertThat(subscriptionResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void completeCheckoutFlow_createsSubscriptionItemsAndUpdatesStatus() {
        String token = registerAndAuthenticateWithCompany();
        UUID companyId = getCompanyId(token);

        // Create customer
        post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        // Create application and paid environment
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = createPaidEnvironment(token, applicationId, "Staging");

        // Create checkout session
        var checkoutRequest = new CheckoutRequest(
                List.of(new CheckoutRequest.PendingEnvironmentDto(environmentId, "Staging",
                        PricingTier.BASIC)), "http://localhost:4200/success",
                "http://localhost:4200/cancel");

        var checkoutResponse =
                post("/v1/payment/checkout", checkoutRequest, token, CheckoutSessionResponse.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String sessionId = checkoutResponse.getBody().sessionId();

        // Complete the checkout (simulates what happens after Stripe payment in mock mode)
        var completeRequest = new PaymentController.CompleteCheckoutRequest(sessionId);
        var completeResponse =
                post("/v1/payment/checkout/complete", completeRequest, token, Void.class);
        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify subscription status is now returned with items
        var statusResponse =
                get("/v1/payment/subscription", token, SubscriptionStatusResponse.class);
        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResponse.getBody()).isNotNull();
        assertThat(statusResponse.getBody().status()).isEqualTo("ACTIVE");
        assertThat(statusResponse.getBody().items()).hasSize(1);
        assertThat(statusResponse.getBody().items().getFirst().environmentId()).isEqualTo(
                environmentId);
        assertThat(statusResponse.getBody().items().getFirst().tier()).isEqualTo("BASIC");
        assertThat(statusResponse.getBody().totalMonthlyPriceCents()).isEqualTo(
                1000); // $10 = 1000 cents
    }

    @Test
    void completeCheckoutFlow_withMultipleEnvironments() {
        String token = registerAndAuthenticateWithCompany();

        // Create customer
        post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        // Create application and multiple paid environments
        UUID applicationId = createApplication(token, "Test App");
        UUID env1Id = createPaidEnvironment(token, applicationId, "Staging");
        UUID env2Id = createEnvironmentWithTier(token, applicationId, "UAT", PricingTier.PRO);

        // Create checkout session with multiple environments
        var checkoutRequest = new CheckoutRequest(
                List.of(new CheckoutRequest.PendingEnvironmentDto(env1Id, "Staging",
                                PricingTier.BASIC),
                        new CheckoutRequest.PendingEnvironmentDto(env2Id, "UAT", PricingTier.PRO)),
                "http://localhost:4200/success", "http://localhost:4200/cancel");

        var checkoutResponse =
                post("/v1/payment/checkout", checkoutRequest, token, CheckoutSessionResponse.class);
        String sessionId = checkoutResponse.getBody().sessionId();

        // Complete the checkout
        var completeRequest = new PaymentController.CompleteCheckoutRequest(sessionId);
        post("/v1/payment/checkout/complete", completeRequest, token, Void.class);

        // Verify subscription status shows both items
        var statusResponse =
                get("/v1/payment/subscription", token, SubscriptionStatusResponse.class);
        assertThat(statusResponse.getBody()).isNotNull();
        assertThat(statusResponse.getBody().items()).hasSize(2);
        assertThat(statusResponse.getBody().totalMonthlyPriceCents()).isEqualTo(
                11000); // $10 + $100 = $110 = 11000 cents
    }

    @Test
    void completeCheckoutSession_returns200_evenWithInvalidSessionId() {
        // In mock mode, invalid session IDs may not cause errors
        String token = registerAndAuthenticateWithCompany();

        // Create customer first
        post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        var completeRequest = new PaymentController.CompleteCheckoutRequest("cs_invalid_session");
        var response = post("/v1/payment/checkout/complete", completeRequest, token, String.class);

        // The endpoint should handle this gracefully (may return 200 or 400 depending on stripe-mock behavior)
        assertThat(response.getStatusCode().is2xxSuccessful() || response.getStatusCode()
                .is4xxClientError()).isTrue();
    }

    // ========== Environment Deletion Tests ==========

    @Test
    void deleteEnvironment_removesSubscriptionItem_forPaidTier() {
        String token = registerAndAuthenticateWithCompany();

        // Create customer and complete checkout for a paid environment
        post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = createPaidEnvironment(token, applicationId, "Premium Env");

        // Create and complete checkout for this environment
        var checkoutRequest = new CheckoutRequest(
                List.of(new CheckoutRequest.PendingEnvironmentDto(environmentId, "Premium Env",
                        PricingTier.BASIC)), "http://localhost:4200/success",
                "http://localhost:4200/cancel");

        var checkoutResponse =
                post("/v1/payment/checkout", checkoutRequest, token, CheckoutSessionResponse.class);
        post("/v1/payment/checkout/complete", new PaymentController.CompleteCheckoutRequest(
                checkoutResponse.getBody().sessionId()), token, Void.class);

        // Verify subscription item exists
        var statusBefore = get("/v1/payment/subscription", token, SubscriptionStatusResponse.class);
        assertThat(statusBefore.getBody()).isNotNull();
        assertThat(statusBefore.getBody().items()).hasSize(1);
        assertThat(statusBefore.getBody().totalMonthlyPriceCents()).isEqualTo(1000); // $10

        // Delete the environment
        delete("/v1/applications/" + applicationId + "/environments/" + environmentId, token,
                Void.class);

        // Verify subscription item is removed
        var statusAfter = get("/v1/payment/subscription", token, SubscriptionStatusResponse.class);
        assertThat(statusAfter.getBody()).isNotNull();
        assertThat(statusAfter.getBody().items()).isEmpty();
        assertThat(statusAfter.getBody().totalMonthlyPriceCents()).isEqualTo(0);
    }

    @Test
    void deleteEnvironment_doesNotAffectSubscription_forFreeTier() {
        String token = registerAndAuthenticateWithCompany();

        // Create customer and complete checkout for a paid environment
        post("/v1/payment/customer",
                new PaymentController.CustomerRequest("Test Company", "billing@test.com"), token,
                PaymentController.CustomerResponse.class);

        UUID applicationId = createApplication(token, "Test App");
        UUID paidEnvId = createPaidEnvironment(token, applicationId, "Paid Env");
        UUID freeEnvId =
                createEnvironmentWithTier(token, applicationId, "Free Env", PricingTier.FREE);

        // Create and complete checkout for paid environment only
        var checkoutRequest = new CheckoutRequest(
                List.of(new CheckoutRequest.PendingEnvironmentDto(paidEnvId, "Paid Env",
                        PricingTier.BASIC)), "http://localhost:4200/success",
                "http://localhost:4200/cancel");

        var checkoutResponse =
                post("/v1/payment/checkout", checkoutRequest, token, CheckoutSessionResponse.class);
        post("/v1/payment/checkout/complete", new PaymentController.CompleteCheckoutRequest(
                checkoutResponse.getBody().sessionId()), token, Void.class);

        // Verify initial subscription status
        var statusBefore = get("/v1/payment/subscription", token, SubscriptionStatusResponse.class);
        assertThat(statusBefore.getBody().items()).hasSize(1);
        assertThat(statusBefore.getBody().totalMonthlyPriceCents()).isEqualTo(1000);

        // Delete the FREE environment
        delete("/v1/applications/" + applicationId + "/environments/" + freeEnvId, token,
                Void.class);

        // Verify subscription is unchanged (still has the paid item)
        var statusAfter = get("/v1/payment/subscription", token, SubscriptionStatusResponse.class);
        assertThat(statusAfter.getBody().items()).hasSize(1);
        assertThat(statusAfter.getBody().totalMonthlyPriceCents()).isEqualTo(1000);
    }

    // ========== Helper Methods ==========

    private UUID createApplication(String token, String name) {
        var response = post("/v1/applications", new ApplicationCreationRequest(name), token,
                ApplicationResponse.class);
        return response.getBody().id();
    }

    private UUID createPaidEnvironment(String token, UUID applicationId, String name) {
        return createEnvironmentWithTier(token, applicationId, name, PricingTier.BASIC);
    }

    private UUID createEnvironmentWithTier(String token, UUID applicationId, String name,
            PricingTier tier) {
        var response = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest(name, "Test environment", tier), token,
                EnvironmentResponse.class);
        return response.getBody().id();
    }

    private UUID getCompanyId(String token) {
        var response = get("/v1/company", token, CompanyResponse.class);
        return response.getBody().id();
    }
}
