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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    @Test
    void createApiKeySetsExpirationDateTo2100() {
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
        assertThat(captor.getValue().getExpirationDate()).isNotNull();
        assertThat(captor.getValue().getExpirationDate().getYear()).isEqualTo(2100);
    }

    // ========== Get API Key by Type Tests ==========

    @Test
    void getApiKeyByTypeReturnsKeyWhenFound() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();

        var apiKeyEntity = createApiKeyEntity(keyId, applicationId, environmentId, KeyType.READ);
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(apiKeyRepository.findActiveByApplicationIdAndEnvironmentIdAndKeyType(any(UUID.class),
                any(UUID.class), any(KeyType.class), any(OffsetDateTime.class))).thenReturn(
                Optional.of(apiKeyEntity));

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

        verify(apiKeyRepository, never()).findActiveByApplicationIdAndEnvironmentIdAndKeyType(any(),
                any(), any(), any());
    }

    @Test
    void getApiKeyByTypeThrowsNotFoundExceptionWhenKeyNotFound() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(apiKeyRepository.findActiveByApplicationIdAndEnvironmentIdAndKeyType(any(UUID.class),
                any(UUID.class), any(KeyType.class), any(OffsetDateTime.class))).thenReturn(
                Optional.empty());

        assertThatThrownBy(() -> apiKeyService.getApiKeyByType(applicationId, environmentId,
                KeyType.WRITE)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("API key not found for type WRITE");
    }

    // ========== Regenerate API Key Tests ==========

    @Test
    void regenerateKeyCreatesNewKeyAndReturnsNewExpirationDateOfOldKey() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID oldKeyId = UUID.randomUUID();
        String originalKey = "originalkey123";

        var oldApiKeyEntity =
                createApiKeyEntity(oldKeyId, applicationId, environmentId, KeyType.READ);
        oldApiKeyEntity.setKey(originalKey);

        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(apiKeyRepository.findActiveByApplicationIdAndEnvironmentIdAndKeyType(any(UUID.class),
                any(UUID.class), any(KeyType.class), any(OffsetDateTime.class))).thenReturn(
                Optional.of(oldApiKeyEntity));
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        var result = apiKeyService.regenerateKey(applicationId, environmentId, KeyType.READ);

        assertThat(result.id()).isNotEqualTo(oldKeyId);
        assertThat(result.secretKey()).isNotNull();
        assertThat(result.secretKey()).isNotEqualTo(originalKey);
        assertThat(result.secretKey()).hasSize(64);
        assertThat(result.expirationDate()).isAfter(OffsetDateTime.now());
        assertThat(result.expirationDate()).isBefore(
                OffsetDateTime.now().plusWeeks(1).plusMinutes(1));
    }

    @Test
    void regenerateKeySavesBothOldAndNewKeys() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID oldKeyId = UUID.randomUUID();

        var oldApiKeyEntity =
                createApiKeyEntity(oldKeyId, applicationId, environmentId, KeyType.WRITE);

        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(apiKeyRepository.findActiveByApplicationIdAndEnvironmentIdAndKeyType(any(UUID.class),
                any(UUID.class), any(KeyType.class), any(OffsetDateTime.class))).thenReturn(
                Optional.of(oldApiKeyEntity));
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        apiKeyService.regenerateKey(applicationId, environmentId, KeyType.WRITE);

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        verify(apiKeyRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        var savedEntities = captor.getAllValues();

        assertThat(savedEntities.getFirst().getId()).isEqualTo(oldKeyId);
        assertThat(savedEntities.getFirst().getExpirationDate()).isAfter(OffsetDateTime.now());
        assertThat(savedEntities.getFirst().getExpirationDate()).isBefore(
                OffsetDateTime.now().plusWeeks(1).plusMinutes(1));

        assertThat(savedEntities.get(1).getId()).isNotEqualTo(oldKeyId);
        assertThat(savedEntities.get(1).getExpirationDate().getYear()).isEqualTo(2100);
    }

    @Test
    void regenerateKeyThrowsNotFoundExceptionWhenApplicationNotFound() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        when(applicationRepository.existsById(applicationId)).thenReturn(false);

        assertThatThrownBy(() -> apiKeyService.regenerateKey(applicationId, environmentId,
                KeyType.READ)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Application not found");

        verify(apiKeyRepository, never()).findActiveByApplicationIdAndEnvironmentIdAndKeyType(any(),
                any(), any(), any());
    }

    @Test
    void regenerateKeyThrowsNotFoundExceptionWhenKeyNotFound() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(apiKeyRepository.findActiveByApplicationIdAndEnvironmentIdAndKeyType(any(UUID.class),
                any(UUID.class), any(KeyType.class), any(OffsetDateTime.class))).thenReturn(
                Optional.empty());

        assertThatThrownBy(() -> apiKeyService.regenerateKey(applicationId, environmentId,
                KeyType.READ)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("API key not found for type READ");
    }

    @Test
    void regenerateKeyPreservesRateLimit() {
        UUID applicationId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID oldKeyId = UUID.randomUUID();
        int customRateLimit = 5000;

        var oldApiKeyEntity =
                createApiKeyEntity(oldKeyId, applicationId, environmentId, KeyType.WRITE);
        oldApiKeyEntity.setRateLimitRequestsPerMinute(customRateLimit);

        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(apiKeyRepository.findActiveByApplicationIdAndEnvironmentIdAndKeyType(any(UUID.class),
                any(UUID.class), any(KeyType.class), any(OffsetDateTime.class))).thenReturn(
                Optional.of(oldApiKeyEntity));
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        apiKeyService.regenerateKey(applicationId, environmentId, KeyType.WRITE);

        ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
        verify(apiKeyRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        var newKey = captor.getAllValues().get(1);
        assertThat(newKey.getRateLimitRequestsPerMinute()).isEqualTo(customRateLimit);
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
        entity.setExpirationDate(OffsetDateTime.of(2100, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        return entity;
    }
}
