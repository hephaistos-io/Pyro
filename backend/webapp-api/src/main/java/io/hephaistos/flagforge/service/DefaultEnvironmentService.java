package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.cache.CacheInvalidationPublisher;
import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.enums.KeyType;
import io.hephaistos.flagforge.common.enums.PaymentStatus;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
import io.hephaistos.flagforge.controller.dto.EnvironmentUpdateRequest;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.security.RequireDev;
import io.hephaistos.flagforge.security.RequireReadOnly;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DefaultEnvironmentService implements EnvironmentService {

    private static final String DEFAULT_ENVIRONMENT_NAME = "Development";
    private static final String DEFAULT_ENVIRONMENT_DESCRIPTION = "Default development environment";
    private static final String PRODUCTION_ENVIRONMENT_NAME = "Production";
    private static final String PRODUCTION_ENVIRONMENT_DESCRIPTION =
            "Default production environment";

    private final EnvironmentRepository environmentRepository;
    private final ApplicationRepository applicationRepository;
    private final ApiKeyService apiKeyService;
    private final RedisCleanupService redisCleanupService;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;
    private final StripeService stripeService;

    public DefaultEnvironmentService(EnvironmentRepository environmentRepository,
            ApplicationRepository applicationRepository, ApiKeyService apiKeyService,
            RedisCleanupService redisCleanupService,
            CacheInvalidationPublisher cacheInvalidationPublisher, StripeService stripeService) {
        this.environmentRepository = environmentRepository;
        this.applicationRepository = applicationRepository;
        this.apiKeyService = apiKeyService;
        this.redisCleanupService = redisCleanupService;
        this.cacheInvalidationPublisher = cacheInvalidationPublisher;
        this.stripeService = stripeService;
    }

    @Override
    @RequireDev
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
        environment.setTier(request.tier());

        // Paid tiers start with PENDING status until payment is completed
        if (request.tier() != PricingTier.FREE) {
            environment.setPaymentStatus(PaymentStatus.PENDING);
        }

        EnvironmentEntity saved = environmentRepository.save(environment);

        createApiKeysForEnvironment(applicationId, saved.getId());

        return EnvironmentResponse.fromEntity(saved);
    }

    @Override
    public EnvironmentResponse createDefaultEnvironment(UUID applicationId) {
        var application = getApplicationOrThrow(applicationId);
        EnvironmentEntity saved = createEnvironmentInternal(application, DEFAULT_ENVIRONMENT_NAME,
                DEFAULT_ENVIRONMENT_DESCRIPTION, PricingTier.FREE);
        return EnvironmentResponse.fromEntity(saved);
    }

    @Override
    public void createDefaultEnvironments(ApplicationEntity application) {
        createEnvironmentInternal(application, DEFAULT_ENVIRONMENT_NAME,
                DEFAULT_ENVIRONMENT_DESCRIPTION, PricingTier.FREE);
        createEnvironmentInternal(application, PRODUCTION_ENVIRONMENT_NAME,
                PRODUCTION_ENVIRONMENT_DESCRIPTION, PricingTier.FREE);
    }

    private EnvironmentEntity createEnvironmentInternal(ApplicationEntity application, String name,
            String description, PricingTier tier) {
        var environment = new EnvironmentEntity();
        environment.setApplication(application);
        environment.setName(name);
        environment.setDescription(description);
        environment.setTier(tier);
        EnvironmentEntity saved = environmentRepository.save(environment);
        createApiKeysForEnvironment(application.getId(), saved.getId());
        return saved;
    }

    @Override
    @RequireReadOnly
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
    @RequireDev
    public EnvironmentResponse updateEnvironment(UUID applicationId, UUID environmentId,
            EnvironmentUpdateRequest request) {
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

        // Check for duplicate name (only if name is changing)
        if (!environment.getName()
                .equals(request.name()) && environmentRepository.existsByNameAndApplication_Id(
                request.name(), applicationId)) {
            throw new DuplicateResourceException(
                    "Environment with name '%s' already exists for this application".formatted(
                            request.name()));
        }

        environment.setName(request.name());
        environment.setDescription(request.description());

        EnvironmentEntity saved = environmentRepository.save(environment);
        return EnvironmentResponse.fromEntity(saved);
    }

    @Override
    @RequireDev
    public void deleteEnvironment(UUID applicationId, UUID environmentId) {
        // Verify application exists (with filter applied)
        var application = applicationRepository.findByIdFiltered(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        var environment = environmentRepository.findById(environmentId)
                .orElseThrow(() -> new NotFoundException("Environment not found"));

        // Verify environment belongs to the application
        if (!environment.getApplicationId().equals(applicationId)) {
            throw new NotFoundException("Environment not found");
        }

        // Remove subscription item from Stripe if this was a paid environment
        if (environment.getTier() != null && environment.getTier().isPaid()) {
            stripeService.removeSubscriptionItem(application.getCompanyId(), environmentId);
        }

        // Delete from database
        environmentRepository.deleteById(environmentId);

        // Cleanup Redis keys (fail-open, non-blocking)
        redisCleanupService.cleanupEnvironmentKeys(environmentId);

        // Invalidate any cached templates for this environment
        cacheInvalidationPublisher.publishEnvironmentDeleted(applicationId, environmentId);
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
