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
import org.mockito.ArgumentCaptor;
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
 * Unit tests for MockStripeService.
 * <p>
 * Tests the mock-specific behavior for local development and testing.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class MockStripeServiceTest {

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
    private MockStripeService mockStripeService;

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

        mockStripeService =
                new MockStripeService(config, subscriptionRepository, subscriptionItemRepository,
                        environmentRepository);
    }

    @Test
    void getSubscriptionStatus_returnsNull_whenNoSubscription() {
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        SubscriptionStatusResponse status = mockStripeService.getSubscriptionStatus(COMPANY_ID);

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

        SubscriptionStatusResponse status = mockStripeService.getSubscriptionStatus(COMPANY_ID);

        assertThat(status).isNotNull();
        assertThat(status.status()).isEqualTo("ACTIVE");
        assertThat(status.totalMonthlyPriceCents()).isEqualTo(1000); // $10 * 100
        assertThat(status.items()).hasSize(1);
        assertThat(status.items().getFirst().environmentName()).isEqualTo("Production");
    }

    @Test
    void createPortalSession_returnsReturnUrl_inMockMode() {
        CompanySubscriptionEntity subscription = createTestSubscription();
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));

        String returnUrl = "http://localhost:4200/billing";

        String result = mockStripeService.createPortalSession(COMPANY_ID, returnUrl);

        // In mock mode, portal session just returns the return URL
        assertThat(result).isEqualTo(returnUrl);
    }

    @Test
    void createPortalSession_throwsException_whenNoCustomer() {
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mockStripeService.createPortalSession(COMPANY_ID,
                "http://localhost")).isInstanceOf(PaymentException.class)
                .hasMessageContaining("No customer found");
    }

    @Test
    void getInvoices_returnsEmptyList_inMockMode() {
        var invoices = mockStripeService.getInvoices(COMPANY_ID, 10);

        // In mock mode, invoices are always empty
        assertThat(invoices).isEmpty();
    }

    @Test
    void addSubscriptionItem_skipsFreeTier() {
        mockStripeService.addSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID, PricingTier.FREE);

        verify(subscriptionRepository, never()).findByCompanyId(any());
        verify(subscriptionItemRepository, never()).save(any());
    }

    @Test
    void addSubscriptionItem_createsItemWithMockId() {
        CompanySubscriptionEntity subscription = createTestSubscription();

        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));

        mockStripeService.addSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID, PricingTier.BASIC);

        ArgumentCaptor<SubscriptionItemEntity> captor =
                ArgumentCaptor.forClass(SubscriptionItemEntity.class);
        verify(subscriptionItemRepository).save(captor.capture());

        SubscriptionItemEntity savedItem = captor.getValue();
        assertThat(savedItem.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(savedItem.getStripeSubscriptionItemId()).startsWith("si_mock_");
        assertThat(savedItem.getStripePriceId()).isEqualTo("price_basic");
    }

    @Test
    void addSubscriptionItem_throwsException_whenNoSubscription() {
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mockStripeService.addSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID,
                PricingTier.BASIC)).isInstanceOf(PaymentException.class)
                .hasMessageContaining("No subscription found");
    }

    @Test
    void addSubscriptionItem_throwsException_whenNoActiveStripeSubscription() {
        CompanySubscriptionEntity subscription = createTestSubscription();
        subscription.setStripeSubscriptionId(null); // No active subscription

        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));

        assertThatThrownBy(() -> mockStripeService.addSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID,
                PricingTier.BASIC)).isInstanceOf(PaymentException.class)
                .hasMessageContaining("No active subscription");
    }

    @Test
    void updateSubscriptionItem_updatesExistingItem() {
        CompanySubscriptionEntity subscription = createTestSubscription();
        SubscriptionItemEntity existingItem = new SubscriptionItemEntity();
        existingItem.setEnvironmentId(ENVIRONMENT_ID);
        existingItem.setStripeSubscriptionItemId("si_mock_existing");
        existingItem.setStripePriceId("price_basic");
        existingItem.setSubscription(subscription);

        when(subscriptionItemRepository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(
                Optional.of(existingItem));

        mockStripeService.updateSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID, PricingTier.PRO);

        ArgumentCaptor<SubscriptionItemEntity> captor =
                ArgumentCaptor.forClass(SubscriptionItemEntity.class);
        verify(subscriptionItemRepository).save(captor.capture());

        SubscriptionItemEntity savedItem = captor.getValue();
        assertThat(savedItem.getStripePriceId()).isEqualTo("price_pro");
    }

    @Test
    void updateSubscriptionItem_removesItem_whenDowngradingToFree() {
        SubscriptionItemEntity existingItem = new SubscriptionItemEntity();
        existingItem.setEnvironmentId(ENVIRONMENT_ID);
        existingItem.setStripeSubscriptionItemId("si_mock_existing");

        when(subscriptionItemRepository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(
                Optional.of(existingItem));

        mockStripeService.updateSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID, PricingTier.FREE);

        verify(subscriptionItemRepository).delete(existingItem);
    }

    @Test
    void removeSubscriptionItem_deletesExistingItem() {
        SubscriptionItemEntity item = new SubscriptionItemEntity();
        item.setEnvironmentId(ENVIRONMENT_ID);
        item.setStripeSubscriptionItemId("si_mock_test");

        when(subscriptionItemRepository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(
                Optional.of(item));

        mockStripeService.removeSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID);

        verify(subscriptionItemRepository).delete(item);
    }

    @Test
    void removeSubscriptionItem_doesNothing_whenNoItem() {
        when(subscriptionItemRepository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(
                Optional.empty());

        // Should not throw
        mockStripeService.removeSubscriptionItem(COMPANY_ID, ENVIRONMENT_ID);

        verify(subscriptionItemRepository, never()).delete(any());
    }

    @Test
    void createCheckoutSession_throwsException_whenNoCustomer() {
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mockStripeService.createCheckoutSession(COMPANY_ID,
                List.of(new StripeService.PendingEnvironmentItem(ENVIRONMENT_ID, "Test Env",
                        PricingTier.BASIC)), "http://success", "http://cancel")).isInstanceOf(
                PaymentException.class).hasMessageContaining("Customer not found");
    }

    @Test
    void createCheckoutSession_throwsException_whenOnlyFreeItems() {
        CompanySubscriptionEntity subscription = createTestSubscription();
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));

        assertThatThrownBy(() -> mockStripeService.createCheckoutSession(COMPANY_ID,
                List.of(new StripeService.PendingEnvironmentItem(ENVIRONMENT_ID, "Test Env",
                        PricingTier.FREE)), "http://success", "http://cancel")).isInstanceOf(
                PaymentException.class).hasMessageContaining("No paid items");
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
