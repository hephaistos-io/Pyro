package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.data.ApiKeyEntity;
import io.hephaistos.flagforge.data.KeyType;
import io.hephaistos.flagforge.data.repository.ApiKeyRepository;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
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
class DefaultApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    private DefaultApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new DefaultApiKeyService(apiKeyRepository, applicationRepository);
    }

    // ========== Create API Key Tests ==========

    @Test
    void createApiKeySetsCorrectApplicationId() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> {
            ApiKeyEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        apiKeyService.createApiKey(applicationId, environmentId, KeyType.READ);

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo(applicationId);
    }

    @Test
    void createApiKeySetsCorrectEnvironmentId() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> {
            ApiKeyEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        apiKeyService.createApiKey(applicationId, environmentId, KeyType.WRITE);

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getEnvironmentId()).isEqualTo(environmentId);
    }

    @Test
    void createApiKeySetsCorrectKeyType() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> {
            ApiKeyEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        apiKeyService.createApiKey(applicationId, environmentId, KeyType.READ);

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getKeyType()).isEqualTo(KeyType.READ);
    }

    @Test
    void createApiKeyGeneratesSecretKey() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> {
            ApiKeyEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        apiKeyService.createApiKey(applicationId, environmentId, KeyType.READ);

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getKey()).isNotNull();
        assertThat(captor.getValue().getKey()).hasSize(64);
    }

    @Test
    void createApiKeySetsDefaultRateLimit() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> {
            ApiKeyEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        apiKeyService.createApiKey(applicationId, environmentId, KeyType.READ);

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getRateLimitRequestsPerMinute()).isEqualTo(1000);
    }

    @Test
    void createApiKeyGeneratesUniqueKeysOnMultipleCalls() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> {
            ApiKeyEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        apiKeyService.createApiKey(applicationId, environmentId, KeyType.READ);
        apiKeyService.createApiKey(applicationId, environmentId, KeyType.WRITE);

        verify(apiKeyRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        var savedEntities = captor.getAllValues();
        assertThat(savedEntities.get(0).getKey()).isNotEqualTo(savedEntities.get(1).getKey());
    }

    // ========== Get API Key by Type Tests ==========

    @Test
    void getApiKeyByTypeReturnsKeyWhenFound() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();

        var apiKeyEntity = createApiKeyEntity(keyId, applicationId, environmentId, KeyType.READ);
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(apiKeyRepository.findByApplicationIdAndEnvironmentIdAndKeyType(applicationId,
                environmentId, KeyType.READ)).thenReturn(Optional.of(apiKeyEntity));

        var result = apiKeyService.getApiKeyByType(applicationId, environmentId, KeyType.READ);

        assertThat(result.id()).isEqualTo(keyId);
        assertThat(result.environmentId()).isEqualTo(environmentId);
        assertThat(result.keyType()).isEqualTo(KeyType.READ);
        assertThat(result.secretKey()).isEqualTo(apiKeyEntity.getKey());
    }

    @Test
    void getApiKeyByTypeThrowsNotFoundExceptionWhenApplicationNotFound() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        when(applicationRepository.existsById(applicationId)).thenReturn(false);

        assertThatThrownBy(() -> apiKeyService.getApiKeyByType(applicationId, environmentId,
                KeyType.READ)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Application not found");

        verify(apiKeyRepository, never()).findByApplicationIdAndEnvironmentIdAndKeyType(any(),
                any(), any());
    }

    @Test
    void getApiKeyByTypeThrowsNotFoundExceptionWhenKeyNotFound() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(apiKeyRepository.findByApplicationIdAndEnvironmentIdAndKeyType(applicationId,
                environmentId, KeyType.WRITE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.getApiKeyByType(applicationId, environmentId,
                KeyType.WRITE)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("API key not found for type WRITE");
    }

    // ========== Regenerate API Key Tests ==========

    @Test
    void regenerateKeyReturnsNewKeyValue() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        String originalKey = "originalkey123";

        var apiKeyEntity = createApiKeyEntity(keyId, applicationId, environmentId, KeyType.READ);
        apiKeyEntity.setKey(originalKey);

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKeyEntity));
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        var result = apiKeyService.regenerateKey(applicationId, keyId);

        assertThat(result.id()).isEqualTo(keyId);
        assertThat(result.secretKey()).isNotNull();
        assertThat(result.secretKey()).isNotEqualTo(originalKey);
        assertThat(result.secretKey()).hasSize(64);
    }

    @Test
    void regenerateKeyUpdatesEntityWithNewKey() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        String originalKey = "originalkey123";

        var apiKeyEntity = createApiKeyEntity(keyId, applicationId, environmentId, KeyType.WRITE);
        apiKeyEntity.setKey(originalKey);

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKeyEntity));
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        apiKeyService.regenerateKey(applicationId, keyId);

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getKey()).isNotEqualTo(originalKey);
    }

    @Test
    void regenerateKeyThrowsNotFoundExceptionWhenKeyNotFound() {
        UUID applicationId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.regenerateKey(applicationId, keyId)).isInstanceOf(
                NotFoundException.class).hasMessageContaining("API key not found");
    }

    @Test
    void regenerateKeyThrowsNotFoundExceptionWhenKeyBelongsToDifferentApplication() {
        UUID applicationId = UUID.randomUUID();
        UUID differentApplicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();

        var apiKeyEntity =
                createApiKeyEntity(keyId, differentApplicationId, environmentId, KeyType.READ);

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKeyEntity));

        assertThatThrownBy(() -> apiKeyService.regenerateKey(applicationId, keyId)).isInstanceOf(
                        NotFoundException.class)
                .hasMessageContaining("API key not found for this application");

        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void regenerateKeyPreservesKeyType() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();

        var apiKeyEntity = createApiKeyEntity(keyId, applicationId, environmentId, KeyType.WRITE);

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKeyEntity));
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        apiKeyService.regenerateKey(applicationId, keyId);

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getKeyType()).isEqualTo(KeyType.WRITE);
    }

    @Test
    void regenerateKeyPreservesEnvironmentId() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();

        var apiKeyEntity = createApiKeyEntity(keyId, applicationId, environmentId, KeyType.READ);

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKeyEntity));
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        apiKeyService.regenerateKey(applicationId, keyId);

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getEnvironmentId()).isEqualTo(environmentId);
    }

    // ========== Helper Methods ==========

    private ApiKeyEntity createApiKeyEntity(UUID id, UUID applicationId, UUID environmentId,
            KeyType keyType) {
        var entity = new ApiKeyEntity();
        entity.setId(id);
        entity.setApplicationId(applicationId);
        entity.setEnvironmentId(environmentId);
        entity.setKeyType(keyType);
        entity.setKey("testkey123456789012345678901234567890123456789012345678901234");
        entity.setRateLimitRequestsPerMinute(1000);
        return entity;
    }
}
