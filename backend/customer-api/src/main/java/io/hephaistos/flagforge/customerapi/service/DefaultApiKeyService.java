package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.customerapi.data.ApiKeyEntity;
import io.hephaistos.flagforge.customerapi.data.repository.ApiKeyRepository;
import io.hephaistos.flagforge.customerapi.data.repository.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Transactional
public class DefaultApiKeyService implements ApiKeyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultApiKeyService.class);
    private static final int DEFAULT_RATE_LIMIT = 1000;

    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;

    public DefaultApiKeyService(ApiKeyRepository apiKeyRepository,
            ApplicationRepository applicationRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public Optional<ApiKeyInfo> validateAndGetApplication(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        String keyHash = hashApiKey(apiKey);

        return apiKeyRepository.findByKeyHash(keyHash)
                .filter(this::isKeyValid)
                .flatMap(this::buildApiKeyInfo)
                .map(info -> {
                    updateLastUsed(info.apiKeyId());
                    return info;
                });
    }

    private boolean isKeyValid(ApiKeyEntity apiKey) {
        if (!Boolean.TRUE.equals(apiKey.getIsActive())) {
            LOGGER.debug("API key {} is not active", apiKey.getKeyPrefix());
            return false;
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(OffsetDateTime.now())) {
            LOGGER.debug("API key {} has expired", apiKey.getKeyPrefix());
            return false;
        }

        return true;
    }

    private Optional<ApiKeyInfo> buildApiKeyInfo(ApiKeyEntity apiKey) {
        return applicationRepository.findById(apiKey.getApplicationId())
                .map(application -> new ApiKeyInfo(apiKey.getId(), application.getId(),
                        application.getCompanyId(), apiKey.getRateLimitRequestsPerMinute() != null ?
                        apiKey.getRateLimitRequestsPerMinute() :
                        DEFAULT_RATE_LIMIT));
    }

    private void updateLastUsed(java.util.UUID apiKeyId) {
        try {
            apiKeyRepository.updateLastUsedAt(apiKeyId, OffsetDateTime.now());
        }
        catch (Exception e) {
            LOGGER.warn("Failed to update last_used_at for API key {}: {}", apiKeyId,
                    e.getMessage());
        }
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
