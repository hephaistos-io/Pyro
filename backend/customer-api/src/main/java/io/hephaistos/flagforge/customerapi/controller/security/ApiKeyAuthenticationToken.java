package io.hephaistos.flagforge.customerapi.controller.security;

import io.hephaistos.flagforge.customerapi.service.ApiKeyInfo;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final ApiKeyInfo apiKeyInfo;

    public ApiKeyAuthenticationToken(ApiKeyInfo apiKeyInfo) {
        super(Collections.emptyList());
        this.apiKeyInfo = apiKeyInfo;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return apiKeyInfo.applicationId();
    }

    public ApiKeyInfo getApiKeyInfo() {
        return apiKeyInfo;
    }
}
