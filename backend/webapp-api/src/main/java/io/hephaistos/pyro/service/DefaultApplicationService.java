package io.hephaistos.pyro.service;

import io.hephaistos.pyro.controller.dto.ApplicationCreationRequest;
import io.hephaistos.pyro.controller.dto.ApplicationResponse;
import io.hephaistos.pyro.data.ApplicationEntity;
import io.hephaistos.pyro.data.repository.ApplicationRepository;
import io.hephaistos.pyro.exception.DuplicateResourceException;
import io.hephaistos.pyro.exception.NoCompanyAssignedException;
import io.hephaistos.pyro.security.PyroSecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DefaultApplicationService implements ApplicationService {

    private final ApplicationRepository applicationRepository;

    public DefaultApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
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

        return ApplicationResponse.fromEntity(application);
    }

    @Override
    public List<ApplicationResponse> getApplicationsForCurrentUserCompany() {
        UUID companyId = getCompanyIdFromSecurityContext();

        return applicationRepository.findByCompanyId(companyId)
                .stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }

    private UUID getCompanyIdFromSecurityContext() {
        var pyroSecurityContext = (PyroSecurityContext) SecurityContextHolder.getContext();
        return pyroSecurityContext.getCompanyId()
                .orElseThrow(() -> new NoCompanyAssignedException(
                        "User has no company assigned. Cannot perform application operations."));
    }
}
