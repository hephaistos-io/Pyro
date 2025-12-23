package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.ApiKeyEntity;
import io.hephaistos.flagforge.common.enums.KeyType;
import io.hephaistos.flagforge.common.util.ApiKeyHasher;
import io.hephaistos.flagforge.controller.dto.ApiKeyResponse;
import io.hephaistos.flagforge.data.repository.ApiKeyRepository;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.security.RequireAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class DefaultApiKeyService implements ApiKeyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultApiKeyService.class);
    private static final int API_KEY_RANDOM_LENGTH = 64;
    private static final OffsetDateTime DEFAULT_EXPIRATION_DATE =
            OffsetDateTime.of(2100, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public DefaultApiKeyService(ApiKeyRepository apiKeyRepository,
            ApplicationRepository applicationRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public ApiKeyResponse createApiKey(UUID applicationId, UUID environmentId, KeyType keyType) {
        String plaintextKey = generateSecretKey();
        String hashedKey = ApiKeyHasher.hash(plaintextKey);

        var apiKey = new ApiKeyEntity();
        apiKey.setApplicationId(applicationId);
        apiKey.setEnvironmentId(environmentId);
        apiKey.setKey(hashedKey);
        apiKey.setKeyType(keyType);
        apiKey.setExpirationDate(DEFAULT_EXPIRATION_DATE);

        ApiKeyEntity saved = apiKeyRepository.save(apiKey);
        LOGGER.info("Created API key (type: {}) for application {} and environment {}", keyType,
                applicationId, environmentId);

        return ApiKeyResponse.fromEntityWithSecret(saved, plaintextKey);
    }

    @Override
    @RequireAdmin
    @Transactional(readOnly = true)
    public ApiKeyResponse getApiKeyByType(UUID applicationId, UUID environmentId, KeyType keyType) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new NotFoundException("Application not found");
        }

        return apiKeyRepository.findActiveByApplicationIdAndEnvironmentIdAndKeyType(applicationId,
                        environmentId, keyType, OffsetDateTime.now(ZoneOffset.UTC))
                .map(ApiKeyResponse::fromEntity)
                .orElseThrow(() -> new NotFoundException("API key not found for type " + keyType));
    }

    @Override
    @RequireAdmin
    public ApiKeyResponse regenerateKey(UUID applicationId, UUID environmentId, KeyType keyType) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new NotFoundException("Application not found");
        }

        ApiKeyEntity oldKey =
                apiKeyRepository.findActiveByApplicationIdAndEnvironmentIdAndKeyType(applicationId,
                                environmentId, keyType, OffsetDateTime.now(ZoneOffset.UTC))
                        .orElseThrow(() -> new NotFoundException(
                                "API key not found for type " + keyType));

        OffsetDateTime newExpirationDateForOldKey = OffsetDateTime.now(ZoneOffset.UTC).plusWeeks(1);
        oldKey.setExpirationDate(newExpirationDateForOldKey);
        apiKeyRepository.save(oldKey);

        String plaintextKey = generateSecretKey();
        String hashedKey = ApiKeyHasher.hash(plaintextKey);

        var newKey = new ApiKeyEntity();
        newKey.setApplicationId(applicationId);
        newKey.setEnvironmentId(environmentId);
        newKey.setKey(hashedKey);
        newKey.setKeyType(keyType);
        newKey.setExpirationDate(DEFAULT_EXPIRATION_DATE);

        ApiKeyEntity saved = apiKeyRepository.save(newKey);
        LOGGER.info("Regenerated API key (type: {}) for application {} and environment {}", keyType,
                applicationId, environmentId);

        return new ApiKeyResponse(saved.getId(), saved.getEnvironmentId(), saved.getKeyType(),
                plaintextKey, newExpirationDateForOldKey);
    }

    private String generateSecretKey() {
        byte[] randomBytes = new byte[API_KEY_RANDOM_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes).substring(0, API_KEY_RANDOM_LENGTH);
    }
}
