package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.CompanyEntity;
import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.data.SubscriptionItemEntity;
import io.hephaistos.flagforge.common.data.UsageDailyStatisticsEntity;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.common.enums.SubscriptionStatus;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CompanySubscriptionRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.SubscriptionItemRepository;
import io.hephaistos.flagforge.data.repository.UsageDailyStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DefaultDisputeEvidenceService.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultDisputeEvidenceServiceTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID ENVIRONMENT_ID = UUID.randomUUID();
    private static final String CHARGE_ID = "ch_test123";

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanySubscriptionRepository subscriptionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SubscriptionItemRepository subscriptionItemRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private UsageDailyStatisticsRepository usageRepository;

    private DefaultDisputeEvidenceService evidenceService;

    @BeforeEach
    void setUp() {
        evidenceService =
                new DefaultDisputeEvidenceService(companyRepository, subscriptionRepository,
                        customerRepository, subscriptionItemRepository, environmentRepository,
                        usageRepository);
    }

    @Test
    void collectEvidence_returnsCustomerInfo_whenCustomerExists() {
        CustomerEntity customer = createTestCustomer();
        CompanyEntity company = createTestCompany();

        when(customerRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(customer));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        DisputeEvidenceService.DisputeEvidence evidence =
                evidenceService.collectEvidence(COMPANY_ID, CHARGE_ID);

        assertThat(evidence.customerEmail()).isEqualTo("john@test.com");
        assertThat(evidence.customerName()).isEqualTo("John Doe");
    }

    @Test
    void collectEvidence_returnsEmptyCustomerInfo_whenNoCustomers() {
        CompanyEntity company = createTestCompany();

        when(customerRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        DisputeEvidenceService.DisputeEvidence evidence =
                evidenceService.collectEvidence(COMPANY_ID, CHARGE_ID);

        assertThat(evidence.customerEmail()).isEmpty();
        assertThat(evidence.customerName()).isEmpty();
    }

    @Test
    void collectEvidence_includesProductDescription_withSubscribedEnvironments() {
        CustomerEntity customer = createTestCustomer();
        CompanyEntity company = createTestCompany();
        CompanySubscriptionEntity subscription = createTestSubscription();
        SubscriptionItemEntity item = createTestSubscriptionItem(subscription);
        EnvironmentEntity environment = createTestEnvironment();

        when(customerRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(customer));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));
        when(subscriptionItemRepository.findBySubscription_Id(SUBSCRIPTION_ID)).thenReturn(
                List.of(item));
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(usageRepository.findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(
                eq(ENVIRONMENT_ID), any(LocalDate.class))).thenReturn(List.of());

        DisputeEvidenceService.DisputeEvidence evidence =
                evidenceService.collectEvidence(COMPANY_ID, CHARGE_ID);

        assertThat(evidence.productDescription()).contains("Test Company");
        assertThat(evidence.productDescription()).contains("Production");
        assertThat(evidence.productDescription()).contains("PRO tier");
    }

    @Test
    void collectEvidence_includesUsageLogs_whenDataAvailable() {
        CustomerEntity customer = createTestCustomer();
        CompanyEntity company = createTestCompany();
        CompanySubscriptionEntity subscription = createTestSubscription();
        SubscriptionItemEntity item = createTestSubscriptionItem(subscription);
        EnvironmentEntity environment = createTestEnvironment();
        UsageDailyStatisticsEntity usage = createTestUsageStats();

        when(customerRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(customer));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));
        when(subscriptionItemRepository.findBySubscription_Id(SUBSCRIPTION_ID)).thenReturn(
                List.of(item));
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(usageRepository.findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(
                eq(ENVIRONMENT_ID), any(LocalDate.class))).thenReturn(List.of(usage));

        DisputeEvidenceService.DisputeEvidence evidence =
                evidenceService.collectEvidence(COMPANY_ID, CHARGE_ID);

        assertThat(evidence.usageLogs()).contains("SERVICE USAGE LOGS");
        assertThat(evidence.usageLogs()).contains("Production");
        assertThat(evidence.usageLogs()).contains("1,000 requests");
    }

    @Test
    void collectEvidence_returnsDefaultDescription_whenNoSubscription() {
        CustomerEntity customer = createTestCustomer();
        CompanyEntity company = createTestCompany();

        when(customerRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(customer));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        DisputeEvidenceService.DisputeEvidence evidence =
                evidenceService.collectEvidence(COMPANY_ID, CHARGE_ID);

        assertThat(evidence.productDescription()).contains("FlagForge feature flag management");
        assertThat(evidence.productDescription()).contains("Test Company");
    }

    @Test
    void collectEvidence_returnsNoUsageMessage_whenNoActiveEnvironments() {
        CustomerEntity customer = createTestCustomer();
        CompanyEntity company = createTestCompany();
        CompanySubscriptionEntity subscription = createTestSubscription();

        when(customerRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(customer));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));
        when(subscriptionItemRepository.findBySubscription_Id(SUBSCRIPTION_ID)).thenReturn(
                List.of());

        DisputeEvidenceService.DisputeEvidence evidence =
                evidenceService.collectEvidence(COMPANY_ID, CHARGE_ID);

        assertThat(evidence.usageLogs()).isEqualTo("No active environments found.");
    }

    @Test
    void collectEvidence_includesServiceDocumentationUrl() {
        CustomerEntity customer = createTestCustomer();
        CompanyEntity company = createTestCompany();

        when(customerRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(customer));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        DisputeEvidenceService.DisputeEvidence evidence =
                evidenceService.collectEvidence(COMPANY_ID, CHARGE_ID);

        assertThat(evidence.serviceDocumentationUrl()).isEqualTo(
                "https://docs.flagforge.io/terms-of-service");
    }

    @Test
    void collectEvidence_handlesMultipleEnvironments() {
        CustomerEntity customer = createTestCustomer();
        CompanyEntity company = createTestCompany();
        CompanySubscriptionEntity subscription = createTestSubscription();

        UUID env1Id = UUID.randomUUID();
        UUID env2Id = UUID.randomUUID();

        SubscriptionItemEntity item1 = new SubscriptionItemEntity();
        item1.setSubscription(subscription);
        item1.setEnvironmentId(env1Id);

        SubscriptionItemEntity item2 = new SubscriptionItemEntity();
        item2.setSubscription(subscription);
        item2.setEnvironmentId(env2Id);

        EnvironmentEntity env1 = new EnvironmentEntity();
        env1.setId(env1Id);
        env1.setName("Staging");
        env1.setTier(PricingTier.BASIC);

        EnvironmentEntity env2 = new EnvironmentEntity();
        env2.setId(env2Id);
        env2.setName("Production");
        env2.setTier(PricingTier.PRO);

        when(customerRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(customer));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID)).thenReturn(
                Optional.of(subscription));
        when(subscriptionItemRepository.findBySubscription_Id(SUBSCRIPTION_ID)).thenReturn(
                List.of(item1, item2));
        when(environmentRepository.findById(env1Id)).thenReturn(Optional.of(env1));
        when(environmentRepository.findById(env2Id)).thenReturn(Optional.of(env2));
        when(usageRepository.findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(eq(env1Id),
                any(LocalDate.class))).thenReturn(List.of());
        when(usageRepository.findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(eq(env2Id),
                any(LocalDate.class))).thenReturn(List.of());

        DisputeEvidenceService.DisputeEvidence evidence =
                evidenceService.collectEvidence(COMPANY_ID, CHARGE_ID);

        assertThat(evidence.productDescription()).contains("Staging");
        assertThat(evidence.productDescription()).contains("Production");
        assertThat(evidence.productDescription()).contains("BASIC tier");
        assertThat(evidence.productDescription()).contains("PRO tier");
    }

    private CustomerEntity createTestCustomer() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(UUID.randomUUID());
        customer.setEmail("john@test.com");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setCompanyId(COMPANY_ID);
        return customer;
    }

    private CompanyEntity createTestCompany() {
        CompanyEntity company = new CompanyEntity();
        company.setId(COMPANY_ID);
        company.setName("Test Company");
        return company;
    }

    private CompanySubscriptionEntity createTestSubscription() {
        CompanySubscriptionEntity subscription = new CompanySubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setCompanyId(COMPANY_ID);
        subscription.setStripeCustomerId("cus_test123");
        subscription.setStripeSubscriptionId("sub_test456");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }

    private SubscriptionItemEntity createTestSubscriptionItem(
            CompanySubscriptionEntity subscription) {
        SubscriptionItemEntity item = new SubscriptionItemEntity();
        item.setId(UUID.randomUUID());
        item.setSubscription(subscription);
        item.setEnvironmentId(ENVIRONMENT_ID);
        item.setStripeSubscriptionItemId("si_test");
        item.setStripePriceId("price_pro");
        return item;
    }

    private EnvironmentEntity createTestEnvironment() {
        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setName("Production");
        environment.setTier(PricingTier.PRO);
        return environment;
    }

    private UsageDailyStatisticsEntity createTestUsageStats() {
        UsageDailyStatisticsEntity usage = new UsageDailyStatisticsEntity();
        usage.setId(UUID.randomUUID());
        usage.setEnvironmentId(ENVIRONMENT_ID);
        usage.setDate(LocalDate.now().minusDays(1));
        usage.setTotalRequests(1000);
        usage.setPeakRequestsPerSecond(50);
        usage.setAvgRequestsPerSecond(BigDecimal.valueOf(10.5));
        return usage;
    }
}
