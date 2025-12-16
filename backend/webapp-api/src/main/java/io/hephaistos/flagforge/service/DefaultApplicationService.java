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
import io.hephaistos.flagforge.security.RequireAdmin;
import io.hephaistos.flagforge.security.RequireReadOnly;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @RequireAdmin
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

        // Update the security context's cached accessible application IDs
        Set<UUID> updatedAppIds = new HashSet<>(securityContext.getAccessibleApplicationIds());
        updatedAppIds.add(application.getId());
        securityContext.setAccessibleApplicationIds(updatedAppIds);

        environmentService.createDefaultEnvironment(application.getId());

        return ApplicationResponse.fromEntity(application);
    }

    @Override
    @RequireReadOnly
    public List<ApplicationResponse> getApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }
}
