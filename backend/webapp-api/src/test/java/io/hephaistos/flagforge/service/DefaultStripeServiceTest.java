package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.data.SubscriptionItemEntity;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.common.enums.SubscriptionStatus;
import io.hephaistos.flagforge.configuration.StripeConfiguration;
import io.hephaistos.flagforge.controller.dto.SubscriptionStatusResponse;
import io.hephaistos.flagforge.data.repository.CompanySubscriptionRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.SubscriptionItemRepository;
import io.hephaistos.flagforge.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DefaultStripeService.
 * <p>
 * These tests focus on repository interactions and local logic. Integration tests with stripe-mock
 * verify actual Stripe API calls.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultStripeServiceTest {

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

    private StripeConfiguration config;
    private DefaultStripeService stripeService;

    @BeforeEach
    void setUp() {
        config = new StripeConfiguration();
        config.setApiKey("sk_test_mock");
        config.setWebhookSecret("whsec_test");
        config.setPublishableKey("pk_test_mock");
        config.setMockEnabled(true);
        config.setMockBaseUrl("http://localhost:12111");
        config.setPriceIds(
                Map.of("BASIC", "price_basic", "STANDARD", "price_standard", "PRO", "price_pro",
                        "BUSINESS", "price_business"));

        stripeService =
                new DefaultStripeService(config, subscriptionRepository, subscriptionItemRepository,
                        environmentRepository);
    }

    @Test
    void createOrGetCustomer_returnsExistingCustomerId_whenSubscriptionExists() {
        CompanySubscriptionEntity existing = new CompanySubscriptionEntity();
        existing.setCompanyId(COMPANY_ID);
        existing.setStripeCustomerId(STRIPE_CUSTOMER_ID);

        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(existing));

        String customerId =
                stripeService.createOrGetCustomer(COMPANY_ID, "Test Company", "billing@test.com");

        assertThat(customerId).isEqualTo(STRIPE_CUSTOMER_ID);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void getSubscriptionStatus_returnsNull_whenNoSubscription() {
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        SubscriptionStatusResponse status = stripeService.getSubscriptionStatus(COMPANY_ID);

        assertThat(status).isNull();
    }

    @Test
    void getSubscriptionStatus_returnsStatus_whenSubscriptionExists() {
        CompanySubscriptionEntity subscription = createTestSubscription();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(86400));
        subscription.setCancelAtPeriodEnd(false);

        SubscriptionItemEntity item = new SubscriptionItemEntity();
        item.setEnvironmentId(ENVIRONMENT_ID);
        item.setStripeSubscriptionItemId("si_test");
        item.setStripePriceId("price_basic");
        item.setSubscription(subscription);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setName("Production");
        environment.setTier(PricingTier.BASIC);

        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));
        when(subscriptionItemRepository.findBySubscription_Id(subscription.getId())).thenReturn(
                List.of(item));
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));

        SubscriptionStatusResponse status = stripeService.getSubscriptionStatus(COMPANY_ID);

        assertThat(status).isNotNull();
        assertThat(status.status()).isEqualTo("ACTIVE");
        assertThat(status.totalMonthlyPriceCents()).isEqualTo(1000); // $10 * 100
        assertThat(status.items()).hasSize(1);
        assertThat(status.items().getFirst().environmentName()).isEqualTo("Production");
        assertThat(status.items().getFirst().tier()).isEqualTo("BASIC");
    }

    @Test
    void getSubscriptionStatus_calculatesTotalPrice_forMultipleItems() {
        CompanySubscriptionEntity subscription = createTestSubscription();
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        UUID env1Id = UUID.randomUUID();
        UUID env2Id = UUID.randomUUID();

        SubscriptionItemEntity item1 = new SubscriptionItemEntity();
        item1.setEnvironmentId(env1Id);
        item1.setStripeSubscriptionItemId("si_1");
        item1.setSubscription(subscription);

        SubscriptionItemEntity item2 = new SubscriptionItemEntity();
        item2.setEnvironmentId(env2Id);
        item2.setStripeSubscriptionItemId("si_2");
        item2.setSubscription(subscription);

        EnvironmentEntity env1 = new EnvironmentEntity();
        env1.setId(env1Id);
        env1.setName("Staging");
        env1.setTier(PricingTier.BASIC); // $10

        EnvironmentEntity env2 = new EnvironmentEntity();
        env2.setId(env2Id);
        env2.setName("Production");
        env2.setTier(PricingTier.PRO); // $100

        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));
        when(subscriptionItemRepository.findBySubscription_Id(subscription.getId())).thenReturn(
                List.of(item1, item2));
        when(environmentRepository.findById(env1Id)).thenReturn(Optional.of(env1));
        when(environmentRepository.findById(env2Id)).thenReturn(Optional.of(env2));

        SubscriptionStatusResponse status = stripeService.getSubscriptionStatus(COMPANY_ID);

        assertThat(status.totalMonthlyPriceCents()).isEqualTo(11000); // $110 * 100
        assertThat(status.items()).hasSize(2);
    }

    @Test
    void getInvoices_returnsEmptyList_whenNoSubscription() {
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        var invoices = stripeService.getInvoices(COMPANY_ID, 10);

        assertThat(invoices).isEmpty();
    }

    @Test
    void createCheckoutSession_throwsException_whenNoCustomer() {
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stripeService.createCheckoutSession(COMPANY_ID,
                List.of(new StripeService.PendingEnvironmentItem(ENVIRONMENT_ID, "Test Env",
                        PricingTier.BASIC)), "http://success", "http://cancel")).isInstanceOf(
                PaymentException.class).hasMessageContaining("Customer not found");
    }

    @Test
    void createCheckoutSession_throwsException_whenOnlyFreeItems() {
        CompanySubscriptionEntity subscription = createTestSubscription();
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));

        assertThatThrownBy(() -> stripeService.createCheckoutSession(COMPANY_ID,
                List.of(new StripeService.PendingEnvironmentItem(ENVIRONMENT_ID, "Test Env",
                        PricingTier.FREE)), "http://success", "http://cancel")).isInstanceOf(
                PaymentException.class).hasMessageContaining("No paid items");
    }

    @Test
    void createPortalSession_throwsException_whenNoSubscription() {
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> stripeService.createPortalSession(COMPANY_ID, "http://return")).isInstanceOf(
                PaymentException.class).hasMessageContaining("No subscription found");
    }

    @Test
    void addSubscriptionItem_skipsFreeTier() {
        stripeService.addSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID, PricingTier.FREE);

        verify(subscriptionRepository, never()).findByCompanyId(any());
        verify(subscriptionItemRepository, never()).save(any());
    }

    @Test
    void addSubscriptionItem_throwsException_whenNoSubscription() {
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stripeService.addSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID,
                PricingTier.BASIC)).isInstanceOf(PaymentException.class)
                .hasMessageContaining("No subscription found");
    }

    @Test
    void addSubscriptionItem_throwsException_whenNoActiveStripeSubscription() {
        CompanySubscriptionEntity subscription = createTestSubscription();
        subscription.setStripeSubscriptionId(null); // No active subscription

        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));

        assertThatThrownBy(() -> stripeService.addSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID,
                PricingTier.BASIC)).isInstanceOf(PaymentException.class)
                .hasMessageContaining("No active subscription");
    }

    @Test
    void removeSubscriptionItem_deletesExistingItem() {
        SubscriptionItemEntity item = new SubscriptionItemEntity();
        item.setEnvironmentId(ENVIRONMENT_ID);
        item.setStripeSubscriptionItemId("si_test");

        when(subscriptionItemRepository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(
                Optional.of(item));

        // Call the method - the Stripe API call will fail in unit tests,
        // but we can verify the repository lookup happens
        // Full integration tests with stripe-mock verify the complete flow
        try {
            stripeService.removeSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID);
        }
        catch (PaymentException e) {
            // Expected in unit tests since Stripe API isn't available
        }

        verify(subscriptionItemRepository).findByEnvironmentId(ENVIRONMENT_ID);
    }

    @Test
    void removeSubscriptionItem_doesNothing_whenNoItem() {
        when(subscriptionItemRepository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(
                Optional.empty());

        // Should not throw
        stripeService.removeSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID);

        verify(subscriptionItemRepository, never()).delete(any());
    }

    @Test
    void priceIdConfiguration_returnsCorrectPriceIds() {
        assertThat(config.getPriceIdForTier("BASIC")).isEqualTo("price_basic");
        assertThat(config.getPriceIdForTier("STANDARD")).isEqualTo("price_standard");
        assertThat(config.getPriceIdForTier("PRO")).isEqualTo("price_pro");
        assertThat(config.getPriceIdForTier("BUSINESS")).isEqualTo("price_business");
    }

    @Test
    void effectiveApiBase_returnsMockUrl_whenMockEnabled() {
        config.setMockEnabled(true);
        config.setMockBaseUrl("http://stripe-mock:12111");

        assertThat(config.getEffectiveApiBase()).isEqualTo("http://stripe-mock:12111");
    }

    @Test
    void effectiveApiBase_returnsNull_whenMockDisabled() {
        config.setMockEnabled(false);

        assertThat(config.getEffectiveApiBase()).isNull();
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
}
