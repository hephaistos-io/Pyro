package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.UserTemplateValuesRepository;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.common.security.FlagForgeSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private TemplateService templateService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserTemplateValuesRepository userTemplateValuesRepository;

    @Mock
    private UsageTrackingService usageTrackingService;

    @Mock
    private AuditInfoService auditInfoService;

    private DefaultApplicationService applicationService;
    private UUID testCompanyId;
    private UUID testCustomerId;

    @BeforeEach
    void setUp() {
        applicationService =
                new DefaultApplicationService(applicationRepository, environmentService,
                        templateService, customerRepository, userTemplateValuesRepository,
                        usageTrackingService, auditInfoService);
        testCompanyId = UUID.randomUUID();
        testCustomerId = UUID.randomUUID();
        setupSecurityContext(testCompanyId, testCustomerId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createApplicationThrowsDuplicateResourceException() {
        var request = new ApplicationCreationRequest("Existing App");
        when(applicationRepository.existsByNameAndCompanyId("Existing App",
                testCompanyId)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(request)).isInstanceOf(
                DuplicateResourceException.class).hasMessageContaining("Existing App");
    }

    @Test
    void createApplicationThrowsNoCompanyAssignedException() {
        setupSecurityContextWithoutCompany();
        var request = new ApplicationCreationRequest("Test App");

        assertThatThrownBy(() -> applicationService.createApplication(request)).isInstanceOf(
                NoCompanyAssignedException.class);
    }

    @Test
    void createApplicationGrantsCreatorAccessToApplication() {
        var request = new ApplicationCreationRequest("Test App");
        var customer = new CustomerEntity();
        customer.setId(testCustomerId);
        customer.setAccessibleApplications(new HashSet<>());

        when(applicationRepository.existsByNameAndCompanyId("Test App", testCompanyId)).thenReturn(
                false);
        when(applicationRepository.save(any(ApplicationEntity.class))).thenAnswer(invocation -> {
            ApplicationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(customer));

        applicationService.createApplication(request);

        // Verify the customer was granted access to the application
        assertThat(customer.getAccessibleApplications()).hasSize(1);
    }

    @Test
    void getApplicationStatisticsReturnsZeroWhenNoUsersAndNoHits() {
        UUID applicationId = UUID.randomUUID();
        var application = new ApplicationEntity();
        application.setId(applicationId);

        when(applicationRepository.findByIdFiltered(applicationId)).thenReturn(
                Optional.of(application));
        when(userTemplateValuesRepository.countByApplicationId(applicationId)).thenReturn(0L);

        var result = applicationService.getApplicationStatistics(applicationId);

        assertThat(result.totalUsers()).isEqualTo(0);
        assertThat(result.hitsThisMonth()).isEqualTo(0);
    }

    @Test
    void getApplicationStatisticsReturnsCorrectUserCount() {
        UUID applicationId = UUID.randomUUID();
        var application = new ApplicationEntity();
        application.setId(applicationId);

        when(applicationRepository.findByIdFiltered(applicationId)).thenReturn(
                Optional.of(application));
        when(userTemplateValuesRepository.countByApplicationId(applicationId)).thenReturn(42L);

        var result = applicationService.getApplicationStatistics(applicationId);

        assertThat(result.totalUsers()).isEqualTo(42);
        assertThat(result.hitsThisMonth()).isEqualTo(0);
    }

    @Test
    void getApplicationStatisticsSumsHitsAcrossEnvironments() {
        UUID applicationId = UUID.randomUUID();
        UUID env1Id = UUID.randomUUID();
        UUID env2Id = UUID.randomUUID();

        var env1 = new EnvironmentEntity();
        env1.setId(env1Id);
        var env2 = new EnvironmentEntity();
        env2.setId(env2Id);

        var application = new ApplicationEntity();
        application.setId(applicationId);
        application.getEnvironments().addAll(List.of(env1, env2));

        when(applicationRepository.findByIdFiltered(applicationId)).thenReturn(
                Optional.of(application));
        when(userTemplateValuesRepository.countByApplicationId(applicationId)).thenReturn(10L);
        when(usageTrackingService.getMonthlyUsage(env1Id)).thenReturn(100L);
        when(usageTrackingService.getMonthlyUsage(env2Id)).thenReturn(200L);

        var result = applicationService.getApplicationStatistics(applicationId);

        assertThat(result.totalUsers()).isEqualTo(10);
        assertThat(result.hitsThisMonth()).isEqualTo(300);
    }

    @Test
    void getApplicationStatisticsThrowsNotFoundForInvalidApp() {
        UUID applicationId = UUID.randomUUID();

        when(applicationRepository.findByIdFiltered(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> applicationService.getApplicationStatistics(applicationId)).isInstanceOf(
                NotFoundException.class).hasMessageContaining(applicationId.toString());
    }

    private void setupSecurityContext(UUID companyId, UUID customerId) {
        var context = new FlagForgeSecurityContext();
        context.setCustomerName("test@example.com");
        context.setCustomerId(customerId);
        context.setCompanyId(companyId.toString());
        SecurityContextHolder.setContext(context);
    }

    private void setupSecurityContextWithoutCompany() {
        var context = new FlagForgeSecurityContext();
        context.setCustomerName("test@example.com");
        context.setCustomerId(testCustomerId);
        SecurityContextHolder.setContext(context);
    }
}
