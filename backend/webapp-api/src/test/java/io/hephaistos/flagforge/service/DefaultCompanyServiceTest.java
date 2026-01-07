package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CompanyEntity;
import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.controller.dto.CompanyCreationRequest;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.exception.CompanyAlreadyAssignedException;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.common.security.FlagForgeSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultCompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private FlagForgeSecurityContext securityContext;

    private DefaultCompanyService companyService;

    private MockedStatic<FlagForgeSecurityContext> securityContextMock;

    @BeforeEach
    void setUp() {
        companyService = new DefaultCompanyService(companyRepository, customerRepository,
                applicationRepository);
        securityContextMock = Mockito.mockStatic(FlagForgeSecurityContext.class);
        securityContextMock.when(FlagForgeSecurityContext::getCurrent).thenReturn(securityContext);
    }

    @AfterEach
    void tearDown() {
        securityContextMock.close();
    }

    // ========== Get Company For Current Customer Tests ==========

    @Test
    void getCompanyForCurrentCustomerReturnsCompanyWhenExists() {
        UUID companyId = UUID.randomUUID();
        var company = createCompanyEntity(companyId, "Test Company");

        when(securityContext.getCompanyId()).thenReturn(Optional.of(companyId));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        var result = companyService.getCompanyForCurrentCustomer();

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(companyId);
        assertThat(result.get().getName()).isEqualTo("Test Company");
    }

    @Test
    void getCompanyForCurrentCustomerReturnsEmptyWhenNoCompanyAssigned() {
        when(securityContext.getCompanyId()).thenReturn(Optional.empty());

        var result = companyService.getCompanyForCurrentCustomer();

        assertThat(result).isEmpty();
        verify(companyRepository, never()).findById(any());
    }

    @Test
    void getCompanyForCurrentCustomerReturnsEmptyWhenCompanyNotFound() {
        UUID companyId = UUID.randomUUID();

        when(securityContext.getCompanyId()).thenReturn(Optional.of(companyId));
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        var result = companyService.getCompanyForCurrentCustomer();

        assertThat(result).isEmpty();
    }

    // ========== Get Company Tests ==========

    @Test
    void getCompanyReturnsCompanyWhenExists() {
        UUID companyId = UUID.randomUUID();
        var company = createCompanyEntity(companyId, "My Company");

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        var result = companyService.getCompany(companyId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(companyId);
    }

    @Test
    void getCompanyReturnsEmptyWhenNotFound() {
        UUID companyId = UUID.randomUUID();

        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        var result = companyService.getCompany(companyId);

        assertThat(result).isEmpty();
    }

    // ========== Create Company For Current Customer Tests ==========

    @Test
    void createCompanyForCurrentCustomerCreatesCompanySuccessfully() {
        UUID customerId = UUID.randomUUID();
        String customerEmail = "test@example.com";
        var request = new CompanyCreationRequest("New Company");
        var customer = createCustomerEntity(customerId, customerEmail);

        when(securityContext.getCompanyId()).thenReturn(Optional.empty());
        when(securityContext.getCustomerName()).thenReturn(customerEmail);
        when(customerRepository.findByEmail(customerEmail)).thenReturn(Optional.of(customer));
        when(companyRepository.save(any(CompanyEntity.class))).thenAnswer(invocation -> {
            CompanyEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        var response = companyService.createCompanyForCurrentCustomer(request);

        assertThat(response.name()).isEqualTo("New Company");
        verify(companyRepository).save(any(CompanyEntity.class));
    }

    @Test
    void createCompanyForCurrentCustomerSetsCompanyOnCustomer() {
        UUID customerId = UUID.randomUUID();
        String customerEmail = "test@example.com";
        var request = new CompanyCreationRequest("New Company");
        var customer = createCustomerEntity(customerId, customerEmail);

        when(securityContext.getCompanyId()).thenReturn(Optional.empty());
        when(securityContext.getCustomerName()).thenReturn(customerEmail);
        when(customerRepository.findByEmail(customerEmail)).thenReturn(Optional.of(customer));
        when(companyRepository.save(any(CompanyEntity.class))).thenAnswer(invocation -> {
            CompanyEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        companyService.createCompanyForCurrentCustomer(request);

        assertThat(customer.getCompanyId()).isPresent();
    }

    @Test
    void createCompanyForCurrentCustomerThrowsWhenAlreadyHasCompany() {
        UUID existingCompanyId = UUID.randomUUID();
        var request = new CompanyCreationRequest("New Company");

        when(securityContext.getCompanyId()).thenReturn(Optional.of(existingCompanyId));
        when(securityContext.getCustomerName()).thenReturn("test@example.com");

        assertThatThrownBy(
                () -> companyService.createCompanyForCurrentCustomer(request)).isInstanceOf(
                        CompanyAlreadyAssignedException.class)
                .hasMessageContaining("already has one assigned");

        verify(companyRepository, never()).save(any());
    }

    @Test
    void createCompanyForCurrentCustomerThrowsWhenCustomerNotFound() {
        String customerEmail = "notfound@example.com";
        var request = new CompanyCreationRequest("New Company");

        when(securityContext.getCompanyId()).thenReturn(Optional.empty());
        when(securityContext.getCustomerName()).thenReturn(customerEmail);
        when(customerRepository.findByEmail(customerEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> companyService.createCompanyForCurrentCustomer(request)).isInstanceOf(
                UsernameNotFoundException.class);

        verify(companyRepository, never()).save(any());
    }

    // ========== Get Company Statistics Tests ==========

    @Test
    void getCompanyStatisticsReturnsStatisticsSuccessfully() {
        UUID companyId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        var application = createApplicationEntityWithEnvironments(appId, companyId);

        when(securityContext.getCompanyId()).thenReturn(Optional.of(companyId));
        when(applicationRepository.findByCompanyId(companyId)).thenReturn(List.of(application));

        var response = companyService.getCompanyStatistics();

        assertThat(response.applications()).hasSize(1);
        assertThat(response.applications().getFirst().id()).isEqualTo(appId);
    }

    @Test
    void getCompanyStatisticsCalculatesTotalMonthlyPrice() {
        UUID companyId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        var application = createApplicationEntityWithEnvironments(appId, companyId);

        when(securityContext.getCompanyId()).thenReturn(Optional.of(companyId));
        when(applicationRepository.findByCompanyId(companyId)).thenReturn(List.of(application));

        var response = companyService.getCompanyStatistics();

        // FREE tier has 0 monthly price
        assertThat(response.totalMonthlyPriceUsd()).isEqualTo(0);
    }

    @Test
    void getCompanyStatisticsThrowsWhenNoCompanyAssigned() {
        when(securityContext.getCompanyId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompanyStatistics()).isInstanceOf(
                NoCompanyAssignedException.class);
    }

    @Test
    void getCompanyStatisticsReturnsEmptyListWhenNoApplications() {
        UUID companyId = UUID.randomUUID();

        when(securityContext.getCompanyId()).thenReturn(Optional.of(companyId));
        when(applicationRepository.findByCompanyId(companyId)).thenReturn(List.of());

        var response = companyService.getCompanyStatistics();

        assertThat(response.applications()).isEmpty();
        assertThat(response.totalMonthlyPriceUsd()).isEqualTo(0);
    }

    // ========== Helper Methods ==========

    private CompanyEntity createCompanyEntity(UUID id, String name) {
        var entity = new CompanyEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private CustomerEntity createCustomerEntity(UUID id, String email) {
        var entity = new CustomerEntity();
        entity.setId(id);
        entity.setEmail(email);
        entity.setFirstName("Test");
        entity.setLastName("User");
        return entity;
    }

    private ApplicationEntity createApplicationEntityWithEnvironments(UUID appId, UUID companyId) {
        var application = new ApplicationEntity();
        application.setId(appId);
        application.setCompanyId(companyId);
        application.setName("Test App");

        var environment = new EnvironmentEntity();
        environment.setId(UUID.randomUUID());
        environment.setName("Development");
        environment.setDescription("Dev env");
        environment.setTier(PricingTier.FREE);
        environment.setApplication(application);

        return application;
    }
}
