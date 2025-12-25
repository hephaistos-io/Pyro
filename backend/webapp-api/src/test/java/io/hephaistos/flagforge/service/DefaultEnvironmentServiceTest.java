package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.cache.CacheInvalidationPublisher;
import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.enums.KeyType;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentUpdateRequest;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class DefaultEnvironmentServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private RedisCleanupService redisCleanupService;

    @Mock
    private CacheInvalidationPublisher cacheInvalidationPublisher;

    private DefaultEnvironmentService environmentService;

    private UUID testApplicationId;

    @BeforeEach
    void setUp() {
        environmentService =
                new DefaultEnvironmentService(environmentRepository, applicationRepository,
                        apiKeyService, redisCleanupService, cacheInvalidationPublisher);
        testApplicationId = UUID.randomUUID();
    }

    // ========== Create Environment Tests ==========

    @Test
    void createEnvironmentCreatesReadAndWriteApiKeys() {
        var request = new EnvironmentCreationRequest("Production", "Prod env");
        var application = createApplicationEntity(testApplicationId);
        UUID environmentId = UUID.randomUUID();

        when(applicationRepository.findByIdFiltered(testApplicationId)).thenReturn(
                Optional.of(application));
        when(environmentRepository.existsByNameAndApplication_Id("Production",
                testApplicationId)).thenReturn(false);
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> {
            EnvironmentEntity entity = invocation.getArgument(0);
            entity.setId(environmentId);
            return entity;
        });

        environmentService.createEnvironment(testApplicationId, request);

        verify(apiKeyService).createApiKey(testApplicationId, environmentId, KeyType.READ);
        verify(apiKeyService).createApiKey(testApplicationId, environmentId, KeyType.WRITE);
    }

    @Test
    void createEnvironmentReturnsCorrectResponse() {
        var request = new EnvironmentCreationRequest("Staging", "Staging environment");
        var application = createApplicationEntity(testApplicationId);
        UUID environmentId = UUID.randomUUID();

        when(applicationRepository.findByIdFiltered(testApplicationId)).thenReturn(
                Optional.of(application));
        when(environmentRepository.existsByNameAndApplication_Id("Staging",
                testApplicationId)).thenReturn(false);
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> {
            EnvironmentEntity entity = invocation.getArgument(0);
            entity.setId(environmentId);
            return entity;
        });

        var response = environmentService.createEnvironment(testApplicationId, request);

        assertThat(response.id()).isEqualTo(environmentId);
        assertThat(response.name()).isEqualTo("Staging");
        assertThat(response.description()).isEqualTo("Staging environment");
        assertThat(response.applicationId()).isEqualTo(testApplicationId);
    }

    @Test
    void createEnvironmentThrowsDuplicateResourceExceptionForExistingName() {
        var request = new EnvironmentCreationRequest("Production", "Prod");
        var application = createApplicationEntity(testApplicationId);

        when(applicationRepository.findByIdFiltered(testApplicationId)).thenReturn(
                Optional.of(application));
        when(environmentRepository.existsByNameAndApplication_Id("Production",
                testApplicationId)).thenReturn(true);

        assertThatThrownBy(() -> environmentService.createEnvironment(testApplicationId,
                request)).isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Production");

        verify(apiKeyService, never()).createApiKey(any(), any(), any());
    }

    @Test
    void createEnvironmentThrowsNotFoundExceptionForNonExistentApplication() {
        var request = new EnvironmentCreationRequest("Production", "Prod");

        when(applicationRepository.findByIdFiltered(testApplicationId)).thenReturn(
                Optional.empty());

        assertThatThrownBy(() -> environmentService.createEnvironment(testApplicationId,
                request)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Application not found");

        verify(apiKeyService, never()).createApiKey(any(), any(), any());
    }

    // ========== Create Default Environment Tests ==========

    @Test
    void createDefaultEnvironmentCreatesReadAndWriteApiKeys() {
        var application = createApplicationEntity(testApplicationId);
        UUID environmentId = UUID.randomUUID();

        when(applicationRepository.findByIdFiltered(testApplicationId)).thenReturn(
                Optional.of(application));
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> {
            EnvironmentEntity entity = invocation.getArgument(0);
            entity.setId(environmentId);
            return entity;
        });

        environmentService.createDefaultEnvironment(testApplicationId);

        verify(apiKeyService).createApiKey(testApplicationId, environmentId, KeyType.READ);
        verify(apiKeyService).createApiKey(testApplicationId, environmentId, KeyType.WRITE);
    }

    @Test
    void createDefaultEnvironmentSetsCorrectDefaults() {
        var application = createApplicationEntity(testApplicationId);
        UUID environmentId = UUID.randomUUID();

        when(applicationRepository.findByIdFiltered(testApplicationId)).thenReturn(
                Optional.of(application));
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> {
            EnvironmentEntity entity = invocation.getArgument(0);
            entity.setId(environmentId);
            return entity;
        });

        var response = environmentService.createDefaultEnvironment(testApplicationId);

        assertThat(response.name()).isEqualTo("Development");
        assertThat(response.description()).isEqualTo("Default development environment");
        assertThat(response.tier()).isEqualTo(PricingTier.FREE);
    }

    @Test
    void createDefaultEnvironmentSavesEntityWithFreeTier() {
        var application = createApplicationEntity(testApplicationId);

        when(applicationRepository.findByIdFiltered(testApplicationId)).thenReturn(
                Optional.of(application));
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> {
            EnvironmentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        environmentService.createDefaultEnvironment(testApplicationId);

        ArgumentCaptor<EnvironmentEntity> captor = ArgumentCaptor.forClass(EnvironmentEntity.class);
        verify(environmentRepository).save(captor.capture());
        assertThat(captor.getValue().getTier()).isEqualTo(PricingTier.FREE);
    }

    @Test
    void createDefaultEnvironmentThrowsNotFoundExceptionForNonExistentApplication() {
        when(applicationRepository.findByIdFiltered(testApplicationId)).thenReturn(
                Optional.empty());

        assertThatThrownBy(
                () -> environmentService.createDefaultEnvironment(testApplicationId)).isInstanceOf(
                NotFoundException.class).hasMessageContaining("Application not found");

        verify(apiKeyService, never()).createApiKey(any(), any(), any());
    }

    // ========== Create Default Environments Tests (Plural) ==========

    @Test
    void createDefaultEnvironmentsCreatesBothDevelopmentAndProduction() {
        var application = createApplicationEntity(testApplicationId);

        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> {
            EnvironmentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        environmentService.createDefaultEnvironments(application);

        // Verify two environments were created
        ArgumentCaptor<EnvironmentEntity> captor = ArgumentCaptor.forClass(EnvironmentEntity.class);
        verify(environmentRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        var environments = captor.getAllValues();
        assertThat(environments).hasSize(2);

        // Verify Development environment
        assertThat(environments.stream()
                .anyMatch(e -> e.getName()
                        .equals("Development") && e.getTier() == PricingTier.FREE)).isTrue();

        // Verify Production environment
        assertThat(environments.stream()
                .anyMatch(e -> e.getName()
                        .equals("Production") && e.getTier() == PricingTier.FREE)).isTrue();
    }

    @Test
    void createDefaultEnvironmentsCreatesApiKeysForBothEnvironments() {
        var application = createApplicationEntity(testApplicationId);
        UUID devEnvId = UUID.randomUUID();
        UUID prodEnvId = UUID.randomUUID();

        java.util.concurrent.atomic.AtomicInteger callCount =
                new java.util.concurrent.atomic.AtomicInteger(0);
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> {
            EnvironmentEntity entity = invocation.getArgument(0);
            // First save is Development, second is Production
            entity.setId(callCount.getAndIncrement() == 0 ? devEnvId : prodEnvId);
            return entity;
        });

        environmentService.createDefaultEnvironments(application);

        // Verify API keys created for both environments (2 keys per environment)
        verify(apiKeyService).createApiKey(testApplicationId, devEnvId, KeyType.READ);
        verify(apiKeyService).createApiKey(testApplicationId, devEnvId, KeyType.WRITE);
        verify(apiKeyService).createApiKey(testApplicationId, prodEnvId, KeyType.READ);
        verify(apiKeyService).createApiKey(testApplicationId, prodEnvId, KeyType.WRITE);
    }

    // ========== Delete Environment Tests ==========

    @Test
    void deleteEnvironmentSucceedsForBasicTier() {
        UUID environmentId = UUID.randomUUID();
        var environment =
                createEnvironmentEntity(environmentId, testApplicationId, PricingTier.BASIC);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));

        environmentService.deleteEnvironment(testApplicationId, environmentId);

        verify(environmentRepository).deleteById(environmentId);
        verify(redisCleanupService).cleanupEnvironmentKeys(environmentId);
        verify(cacheInvalidationPublisher).publishEnvironmentDeleted(testApplicationId,
                environmentId);
    }

    @Test
    void deleteEnvironmentSucceedsForFreeTier() {
        UUID environmentId = UUID.randomUUID();
        var environment =
                createEnvironmentEntity(environmentId, testApplicationId, PricingTier.FREE);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));

        environmentService.deleteEnvironment(testApplicationId, environmentId);

        verify(environmentRepository).deleteById(environmentId);
        verify(redisCleanupService).cleanupEnvironmentKeys(environmentId);
        verify(cacheInvalidationPublisher).publishEnvironmentDeleted(testApplicationId,
                environmentId);
    }

    @Test
    void deleteEnvironmentThrowsNotFoundExceptionForNonExistentEnvironment() {
        UUID environmentId = UUID.randomUUID();

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> environmentService.deleteEnvironment(testApplicationId,
                environmentId)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Environment not found");
    }

    @Test
    void deleteEnvironmentThrowsNotFoundExceptionForEnvironmentInDifferentApplication() {
        UUID environmentId = UUID.randomUUID();
        UUID differentApplicationId = UUID.randomUUID();
        var environment =
                createEnvironmentEntity(environmentId, differentApplicationId, PricingTier.BASIC);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));

        assertThatThrownBy(() -> environmentService.deleteEnvironment(testApplicationId,
                environmentId)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Environment not found");

        verify(environmentRepository, never()).deleteById(any());
        verify(redisCleanupService, never()).cleanupEnvironmentKeys(any());
        verify(cacheInvalidationPublisher, never()).publishEnvironmentDeleted(any(), any());
    }

    // ========== Update Environment Tests ==========

    @Test
    void updateEnvironmentSuccessfullyUpdatesNameAndDescription() {
        UUID environmentId = UUID.randomUUID();
        var request = new EnvironmentUpdateRequest("Updated Name", "Updated description");
        var environment =
                createEnvironmentEntity(environmentId, testApplicationId, PricingTier.BASIC);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));
        when(environmentRepository.existsByNameAndApplication_Id("Updated Name",
                testApplicationId)).thenReturn(false);
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        var response =
                environmentService.updateEnvironment(testApplicationId, environmentId, request);

        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.description()).isEqualTo("Updated description");
        verify(environmentRepository).save(any(EnvironmentEntity.class));
    }

    @Test
    void updateEnvironmentAllowsSameNameWhenNotChanging() {
        UUID environmentId = UUID.randomUUID();
        var environment =
                createEnvironmentEntity(environmentId, testApplicationId, PricingTier.BASIC);
        environment.setName("Original Name");
        var request = new EnvironmentUpdateRequest("Original Name", "New description");

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        var response =
                environmentService.updateEnvironment(testApplicationId, environmentId, request);

        assertThat(response.description()).isEqualTo("New description");
        verify(environmentRepository, never()).existsByNameAndApplication_Id(any(), any());
    }

    @Test
    void updateEnvironmentThrowsDuplicateResourceExceptionForDuplicateName() {
        UUID environmentId = UUID.randomUUID();
        var request = new EnvironmentUpdateRequest("Duplicate Name", "Description");
        var environment =
                createEnvironmentEntity(environmentId, testApplicationId, PricingTier.BASIC);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));
        when(environmentRepository.existsByNameAndApplication_Id("Duplicate Name",
                testApplicationId)).thenReturn(true);

        assertThatThrownBy(
                () -> environmentService.updateEnvironment(testApplicationId, environmentId,
                        request)).isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Duplicate Name");

        verify(environmentRepository, never()).save(any());
    }

    @Test
    void updateEnvironmentThrowsNotFoundExceptionForNonExistentApplication() {
        UUID environmentId = UUID.randomUUID();
        var request = new EnvironmentUpdateRequest("Name", "Description");

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(false);

        assertThatThrownBy(
                () -> environmentService.updateEnvironment(testApplicationId, environmentId,
                        request)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    void updateEnvironmentThrowsNotFoundExceptionForNonExistentEnvironment() {
        UUID environmentId = UUID.randomUUID();
        var request = new EnvironmentUpdateRequest("Name", "Description");

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> environmentService.updateEnvironment(testApplicationId, environmentId,
                        request)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Environment not found");
    }

    @Test
    void updateEnvironmentThrowsNotFoundExceptionForEnvironmentInDifferentApplication() {
        UUID environmentId = UUID.randomUUID();
        UUID differentApplicationId = UUID.randomUUID();
        var request = new EnvironmentUpdateRequest("Name", "Description");
        var environment =
                createEnvironmentEntity(environmentId, differentApplicationId, PricingTier.BASIC);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));

        assertThatThrownBy(
                () -> environmentService.updateEnvironment(testApplicationId, environmentId,
                        request)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Environment not found");

        verify(environmentRepository, never()).save(any());
    }

    // ========== Helper Methods ==========

    private ApplicationEntity createApplicationEntity(UUID id) {
        var entity = new ApplicationEntity();
        entity.setId(id);
        entity.setName("Test App");
        entity.setCompanyId(UUID.randomUUID());
        return entity;
    }

    private EnvironmentEntity createEnvironmentEntity(UUID id, UUID applicationId,
            PricingTier tier) {
        var entity = new EnvironmentEntity();
        entity.setId(id);
        entity.setApplicationId(applicationId);
        entity.setName("Test Environment");
        entity.setDescription("Test description");
        entity.setTier(tier);
        return entity;
    }
}
