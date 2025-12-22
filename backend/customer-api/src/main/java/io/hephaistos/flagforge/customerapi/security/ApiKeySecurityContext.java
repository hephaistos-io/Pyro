package io.hephaistos.flagforge.customerapi.security;

import io.hephaistos.flagforge.common.enums.KeyType;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class ApiKeySecurityContext implements SecurityContext {

    private Authentication authentication;
    private UUID applicationId;
    private UUID companyId;
    private UUID apiKeyId;
    private UUID environmentId;
    private KeyType keyType;
    private int rateLimitPerSecond;
    private int requestsPerMonth;

    public static ApiKeySecurityContext getCurrent() {
        return (ApiKeySecurityContext) SecurityContextHolder.getContext();
    }

    @Override
    public @Nullable Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public void setAuthentication(@Nullable Authentication authentication) {
        this.authentication = authentication;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public UUID getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(UUID apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
    }

    /**
     * Returns the key type (READ or WRITE) for this API key.
     *
     * @return the KeyType
     * @throws IllegalStateException if the security context is not properly initialized
     */
    public KeyType getKeyType() {
        if (keyType == null) {
            throw new IllegalStateException(
                    "Cannot determine key type: security context not properly initialized");
        }
        return keyType;
    }

    public void setKeyType(KeyType keyType) {
        this.keyType = keyType;
    }

    public int getRateLimitPerSecond() {
        return rateLimitPerSecond;
    }

    public void setRateLimitPerSecond(int rateLimitPerSecond) {
        this.rateLimitPerSecond = rateLimitPerSecond;
    }

    public int getRequestsPerMonth() {
        return requestsPerMonth;
    }

    public void setRequestsPerMonth(int requestsPerMonth) {
        this.requestsPerMonth = requestsPerMonth;
    }
}
