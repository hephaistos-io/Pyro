package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.data.ApplicationEntity;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
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

    public DefaultApplicationService(ApplicationRepository applicationRepository,
            EnvironmentService environmentService) {
        this.applicationRepository = applicationRepository;
        this.environmentService = environmentService;
    }

    @Override
    public ApplicationResponse createApplication(ApplicationCreationRequest request) {
        UUID companyId = getCompanyIdFromSecurityContext();

        if (applicationRepository.existsByNameAndCompanyId(request.name(), companyId)) {
            throw new DuplicateResourceException(
                    "Application with name '%s' already exists for this company".formatted(
                            request.name()));
        }

        var application = new ApplicationEntity();
        application.setName(request.name());
        application.setCompanyId(companyId);
        applicationRepository.save(application);

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
