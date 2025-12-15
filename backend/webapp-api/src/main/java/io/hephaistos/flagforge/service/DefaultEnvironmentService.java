package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
import io.hephaistos.flagforge.data.ApplicationEntity;
import io.hephaistos.flagforge.data.EnvironmentEntity;
import io.hephaistos.flagforge.data.KeyType;
import io.hephaistos.flagforge.data.PricingTier;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.exception.OperationNotAllowedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DefaultEnvironmentService implements EnvironmentService {

    private static final String DEFAULT_ENVIRONMENT_NAME = "Development";
    private static final String DEFAULT_ENVIRONMENT_DESCRIPTION = "Default development environment";

    private final EnvironmentRepository environmentRepository;
    private final ApplicationRepository applicationRepository;
    private final ApiKeyService apiKeyService;

    public DefaultEnvironmentService(EnvironmentRepository environmentRepository,
            ApplicationRepository applicationRepository, ApiKeyService apiKeyService) {
        this.environmentRepository = environmentRepository;
        this.applicationRepository = applicationRepository;
        this.apiKeyService = apiKeyService;
    }

    @Override
    public EnvironmentResponse createEnvironment(UUID applicationId,
            EnvironmentCreationRequest request) {
        var application = getApplicationOrThrow(applicationId);

        if (environmentRepository.existsByNameAndApplication_Id(request.name(), applicationId)) {
            throw new DuplicateResourceException(
                    "Environment with name '%s' already exists for this application".formatted(
                            request.name()));
        }

        var environment = new EnvironmentEntity();
        environment.setApplication(application);
        environment.setName(request.name());
        environment.setDescription(request.description());

        EnvironmentEntity saved = environmentRepository.save(environment);

        createApiKeysForEnvironment(applicationId, saved.getId());

        return EnvironmentResponse.fromEntity(saved);
    }

    @Override
    public EnvironmentResponse createDefaultEnvironment(UUID applicationId) {
        var application = getApplicationOrThrow(applicationId);

        var environment = new EnvironmentEntity();
        environment.setApplication(application);
        environment.setName(DEFAULT_ENVIRONMENT_NAME);
        environment.setDescription(DEFAULT_ENVIRONMENT_DESCRIPTION);
        environment.setTier(PricingTier.FREE);

        EnvironmentEntity saved = environmentRepository.save(environment);

        createApiKeysForEnvironment(applicationId, saved.getId());

        return EnvironmentResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentResponse> getEnvironmentsForApplication(UUID applicationId) {
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found");
        }

        return environmentRepository.findByApplication_Id(applicationId)
                .stream()
                .map(EnvironmentResponse::fromEntity)
                .toList();
    }

    @Override
    public void deleteEnvironment(UUID applicationId, UUID environmentId) {
        // Verify application exists (with filter applied)
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found");
        }

        var environment = environmentRepository.findById(environmentId)
                .orElseThrow(() -> new NotFoundException("Environment not found"));

        // Verify environment belongs to the application
        if (!environment.getApplicationId().equals(applicationId)) {
            throw new NotFoundException("Environment not found");
        }

        // Prevent deletion of FREE tier environments
        if (environment.getTier() == PricingTier.FREE) {
            throw new OperationNotAllowedException(
                    "Cannot delete the default environment. FREE tier environments cannot be deleted.");
        }

        environmentRepository.deleteById(environmentId);
    }

    private ApplicationEntity getApplicationOrThrow(UUID applicationId) {
        return applicationRepository.findByIdFiltered(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    private void createApiKeysForEnvironment(UUID applicationId, UUID environmentId) {
        apiKeyService.createApiKey(applicationId, environmentId, KeyType.READ);
        apiKeyService.createApiKey(applicationId, environmentId, KeyType.WRITE);
    }
}
