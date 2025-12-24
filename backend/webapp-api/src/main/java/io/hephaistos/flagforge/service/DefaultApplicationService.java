package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationListResponse;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.ApplicationStatisticsResponse;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.UserTemplateValuesRepository;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.exception.NotFoundException;
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
    private final TemplateService templateService;
    private final CustomerRepository customerRepository;
    private final UserTemplateValuesRepository userTemplateValuesRepository;
    private final UsageTrackingService usageTrackingService;

    public DefaultApplicationService(ApplicationRepository applicationRepository,
            EnvironmentService environmentService, TemplateService templateService,
            CustomerRepository customerRepository,
            UserTemplateValuesRepository userTemplateValuesRepository,
            UsageTrackingService usageTrackingService) {
        this.applicationRepository = applicationRepository;
        this.environmentService = environmentService;
        this.templateService = templateService;
        this.customerRepository = customerRepository;
        this.userTemplateValuesRepository = userTemplateValuesRepository;
        this.usageTrackingService = usageTrackingService;
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

        var application = new ApplicationEntity();
        application.setName(request.name());
        application.setCompanyId(companyId);
        applicationRepository.save(application);

        // Grant the creator access to the application
        CustomerEntity customer = customerRepository.findById(customerId).orElseThrow();
        customer.getAccessibleApplications().add(application);

        // Update the security context's cached accessible application IDs
        Set<UUID> updatedAppIds = new HashSet<>(securityContext.getAccessibleApplicationIds());
        updatedAppIds.add(application.getId());
        securityContext.setAccessibleApplicationIds(updatedAppIds);

        environmentService.createDefaultEnvironments(application);
        templateService.createDefaultTemplates(application);

        return ApplicationResponse.fromEntity(application);
    }

    @Override
    @RequireReadOnly
    @Transactional(readOnly = true)
    public List<ApplicationListResponse> getApplications() {
        return applicationRepository.findAll()
                .stream().map(ApplicationListResponse::fromEntity)
                .toList();
    }

    @Override
    @RequireReadOnly
    @Transactional(readOnly = true)
    public ApplicationStatisticsResponse getApplicationStatistics(UUID applicationId) {
        // Verify application exists and user has access
        var application = applicationRepository.findByIdFiltered(applicationId)
                .orElseThrow(
                        () -> new NotFoundException("Application not found: " + applicationId));

        long userCount = userTemplateValuesRepository.countByApplicationId(applicationId);

        // Sum hits across all environments
        long totalHits = application.getEnvironments()
                .stream()
                .mapToLong(env -> usageTrackingService.getMonthlyUsage(env.getId()))
                .sum();

        return new ApplicationStatisticsResponse(userCount, totalHits);
    }
}
