package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.data.ApplicationEntity;
import io.hephaistos.flagforge.data.CustomerEntity;
import io.hephaistos.flagforge.data.PricingTier;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    private DefaultApplicationService applicationService;
    private UUID testCompanyId;
    private UUID testCustomerId;

    @BeforeEach
    void setUp() {
        applicationService =
                new DefaultApplicationService(applicationRepository, environmentService,
                        templateService, customerRepository);
        testCompanyId = UUID.randomUUID();
        testCustomerId = UUID.randomUUID();
        setupSecurityContext(testCompanyId, testCustomerId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createFirstApplicationForCompanyHasFreePricingTier() {
        var request = new ApplicationCreationRequest("Test App");
        var customer = new CustomerEntity();
        customer.setId(testCustomerId);
        when(applicationRepository.existsByNameAndCompanyId("Test App", testCompanyId)).thenReturn(
                false);
        when(applicationRepository.countByCompanyId(testCompanyId)).thenReturn(0L);
        when(applicationRepository.save(any(ApplicationEntity.class))).thenAnswer(invocation -> {
            ApplicationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(customer));

        var response = applicationService.createApplication(request);

        assertThat(response.name()).isEqualTo("Test App");
        assertThat(response.companyId()).isEqualTo(testCompanyId);
        assertThat(response.pricingTier()).isEqualTo(PricingTier.FREE);
        verify(environmentService).createDefaultEnvironment(response.id());
        verify(templateService).createDefaultTemplates(any(ApplicationEntity.class));

        // Verify the entity was saved with FREE tier
        ArgumentCaptor<ApplicationEntity> captor = ArgumentCaptor.forClass(ApplicationEntity.class);
        verify(applicationRepository).save(captor.capture());
        assertThat(captor.getValue().getPricingTier()).isEqualTo(PricingTier.FREE);
    }

    @Test
    void createSubsequentApplicationForCompanyHasPaidPricingTier() {
        var request = new ApplicationCreationRequest("Second App");
        var customer = new CustomerEntity();
        customer.setId(testCustomerId);
        when(applicationRepository.existsByNameAndCompanyId("Second App",
                testCompanyId)).thenReturn(false);
        when(applicationRepository.countByCompanyId(testCompanyId)).thenReturn(1L);
        when(applicationRepository.save(any(ApplicationEntity.class))).thenAnswer(invocation -> {
            ApplicationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(customer));

        var response = applicationService.createApplication(request);

        assertThat(response.name()).isEqualTo("Second App");
        assertThat(response.companyId()).isEqualTo(testCompanyId);
        assertThat(response.pricingTier()).isEqualTo(PricingTier.PAID);
        verify(environmentService).createDefaultEnvironment(response.id());
        verify(templateService).createDefaultTemplates(any(ApplicationEntity.class));

        // Verify the entity was saved with PAID tier
        ArgumentCaptor<ApplicationEntity> captor = ArgumentCaptor.forClass(ApplicationEntity.class);
        verify(applicationRepository).save(captor.capture());
        assertThat(captor.getValue().getPricingTier()).isEqualTo(PricingTier.PAID);
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
    void createApplicationUpdatesSecurityContextAccessibleApplicationIds() {
        var request = new ApplicationCreationRequest("Test App");
        var customer = new CustomerEntity();
        customer.setId(testCustomerId);
        UUID newAppId = UUID.randomUUID();

        when(applicationRepository.existsByNameAndCompanyId("Test App", testCompanyId)).thenReturn(
                false);
        when(applicationRepository.countByCompanyId(testCompanyId)).thenReturn(0L);
        when(applicationRepository.save(any(ApplicationEntity.class))).thenAnswer(invocation -> {
            ApplicationEntity entity = invocation.getArgument(0);
            entity.setId(newAppId);
            return entity;
        });
        when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(customer));

        // Set up initial accessible app IDs
        var context = (FlagForgeSecurityContext) SecurityContextHolder.getContext();
        UUID existingAppId = UUID.randomUUID();
        context.setAccessibleApplicationIds(Set.of(existingAppId));

        applicationService.createApplication(request);

        // Verify the security context was updated with the new app ID
        Set<UUID> accessibleAppIds = context.getAccessibleApplicationIds();
        assertThat(accessibleAppIds).contains(existingAppId, newAppId);
    }

    @Test
    void createApplicationGrantsCreatorAccessToApplication() {
        var request = new ApplicationCreationRequest("Test App");
        var customer = new CustomerEntity();
        customer.setId(testCustomerId);
        customer.setAccessibleApplications(new HashSet<>());

        when(applicationRepository.existsByNameAndCompanyId("Test App", testCompanyId)).thenReturn(
                false);
        when(applicationRepository.countByCompanyId(testCompanyId)).thenReturn(0L);
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
