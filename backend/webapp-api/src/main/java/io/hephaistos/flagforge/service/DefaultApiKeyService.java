package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.ApiKeyResponse;
import io.hephaistos.flagforge.data.ApiKeyEntity;
import io.hephaistos.flagforge.data.KeyType;
import io.hephaistos.flagforge.data.repository.ApiKeyRepository;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.security.RequireAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class DefaultApiKeyService implements ApiKeyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultApiKeyService.class);
    private static final int API_KEY_RANDOM_LENGTH = 64;
    private static final int DEFAULT_RATE_LIMIT = 1000;

    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public DefaultApiKeyService(ApiKeyRepository apiKeyRepository,
            ApplicationRepository applicationRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public void createApiKey(UUID applicationId, UUID environmentId, KeyType keyType) {
        var apiKey = new ApiKeyEntity();
        apiKey.setApplicationId(applicationId);
        apiKey.setEnvironmentId(environmentId);
        apiKey.setKey(generateSecretKey());
        apiKey.setRateLimitRequestsPerMinute(DEFAULT_RATE_LIMIT);
        apiKey.setKeyType(keyType);

        apiKeyRepository.save(apiKey);
        LOGGER.info("Created API key (type: {}) for application {} and environment {}", keyType,
                applicationId, environmentId);
    }

    @Override
    @RequireAdmin
    @Transactional(readOnly = true)
    public ApiKeyResponse getApiKeyByType(UUID applicationId, UUID environmentId, KeyType keyType) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new NotFoundException("Application not found");
        }

        return apiKeyRepository.findByApplicationIdAndEnvironmentIdAndKeyType(applicationId,
                        environmentId, keyType)
                .map(ApiKeyResponse::fromEntity)
                .orElseThrow(() -> new NotFoundException("API key not found for type " + keyType));
    }

    @Override
    @RequireAdmin
    public ApiKeyResponse regenerateKey(UUID applicationId, UUID environmentId, KeyType keyType) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new NotFoundException("Application not found");
        }

        ApiKeyEntity apiKey =
                apiKeyRepository.findByApplicationIdAndEnvironmentIdAndKeyType(applicationId,
                                environmentId, keyType)
                        .orElseThrow(() -> new NotFoundException(
                                "API key not found for type " + keyType));

        String newKey = generateSecretKey();
        apiKey.setKey(newKey);

        apiKeyRepository.save(apiKey);
        LOGGER.info("Regenerated API key (type: {}) for application {} and environment {}", keyType,
                applicationId, environmentId);

        return ApiKeyResponse.fromEntity(apiKey);
    }

    private String generateSecretKey() {
        byte[] randomBytes = new byte[API_KEY_RANDOM_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes).substring(0, API_KEY_RANDOM_LENGTH);
    }
}
