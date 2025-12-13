package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.ApiKeyCreationResponse;
import io.hephaistos.flagforge.controller.dto.ApiKeyResponse;
import io.hephaistos.flagforge.data.ApiKeyEntity;
import io.hephaistos.flagforge.data.repository.ApiKeyRepository;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DefaultApiKeyService implements ApiKeyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultApiKeyService.class);
    private static final String API_KEY_PREFIX = "ff_live_";
    private static final int API_KEY_RANDOM_LENGTH = 32;
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
    public ApiKeyCreationResponse createApiKey(UUID applicationId, String name) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new NotFoundException("Application not found");
        }

        String secretKey = generateSecretKey();
        String keyHash = hashApiKey(secretKey);
        String keyPrefix = secretKey.substring(0, 8);

        var apiKey = new ApiKeyEntity();
        apiKey.setApplicationId(applicationId);
        apiKey.setName(name);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setCreatedAt(OffsetDateTime.now());
        apiKey.setIsActive(true);
        apiKey.setRateLimitRequestsPerMinute(DEFAULT_RATE_LIMIT);

        ApiKeyEntity saved = apiKeyRepository.save(apiKey);
        LOGGER.info("Created API key '{}' for application {}", name, applicationId);

        return new ApiKeyCreationResponse(saved.getId(), name, secretKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getApiKeysForApplication(UUID applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new NotFoundException("Application not found");
        }

        return apiKeyRepository.findByApplicationId(applicationId)
                .stream()
                .map(ApiKeyResponse::fromEntity)
                .toList();
    }

    @Override
    public void revokeApiKey(UUID applicationId, UUID apiKeyId) {
        ApiKeyEntity apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new NotFoundException("API key not found"));

        if (!apiKey.getApplicationId().equals(applicationId)) {
            throw new NotFoundException("API key not found for this application");
        }

        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        LOGGER.info("Revoked API key {} for application {}", apiKeyId, applicationId);
    }

    private String generateSecretKey() {
        byte[] randomBytes = new byte[API_KEY_RANDOM_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String randomPart =
                HexFormat.of().formatHex(randomBytes).substring(0, API_KEY_RANDOM_LENGTH);
        return API_KEY_PREFIX + randomPart;
    }

    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
