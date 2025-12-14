package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.data.ApplicationEntity;
import io.hephaistos.flagforge.data.CustomerEntity;
import io.hephaistos.flagforge.data.PricingTier;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DefaultApplicationService implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final EnvironmentService environmentService;
    private final CustomerRepository customerRepository;

    public DefaultApplicationService(ApplicationRepository applicationRepository,
            EnvironmentService environmentService, CustomerRepository customerRepository) {
        this.applicationRepository = applicationRepository;
        this.environmentService = environmentService;
        this.customerRepository = customerRepository;
    }

    @Override
    public ApplicationResponse createApplication(ApplicationCreationRequest request) {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        UUID companyId = securityContext.getCompanyId()
                .orElseThrow(() -> new NoCompanyAssignedException(
                        "Customer has no company assigned. Cannot perform application operations."));
        UUID customerId = securityContext.getCustomerId();

        if (applicationRepository.existsByNameAndCompanyId(request.name(), companyId)) {
            throw new DuplicateResourceException(
                    "Application with name '%s' already exists for this company".formatted(
                            request.name()));
        }

        // First application for a company is FREE, subsequent applications are PAID
        boolean isFirstApplication = applicationRepository.countByCompanyId(companyId) == 0;
        PricingTier pricingTier = isFirstApplication ? PricingTier.FREE : PricingTier.PAID;

        var application = new ApplicationEntity();
        application.setName(request.name());
        application.setCompanyId(companyId);
        application.setPricingTier(pricingTier);
        applicationRepository.save(application);

        // Grant the creator access to the application
        CustomerEntity customer = customerRepository.findById(customerId).orElseThrow();
        customer.getAccessibleApplications().add(application);

        environmentService.createDefaultEnvironment(application.getId());

        return ApplicationResponse.fromEntity(application);
    }

    @Override
    public List<ApplicationResponse> getApplicationsForCurrentCustomerCompany() {
        UUID companyId = getCompanyIdFromSecurityContext();

        return applicationRepository.findByCompanyId(companyId)
                .stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }

    private UUID getCompanyIdFromSecurityContext() {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        return securityContext.getCompanyId()
                .orElseThrow(() -> new NoCompanyAssignedException(
                        "Customer has no company assigned. Cannot perform application operations."));
    }
}
