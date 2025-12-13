package io.hephaistos.flagforge.customerapi.controller.security;

import io.hephaistos.flagforge.customerapi.security.ApiKeySecurityContext;
import io.hephaistos.flagforge.customerapi.service.ApiKeyInfo;
import io.hephaistos.flagforge.customerapi.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class ApiKeyOncePerRequestFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyOncePerRequestFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final ApiKeyService apiKeyService;

    public ApiKeyOncePerRequestFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    @NullMarked
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        extractApiKey(request).flatMap(apiKeyService::validateAndGetApplication)
                .ifPresent(apiKeyInfo -> updateSecurityContext(apiKeyInfo, request));

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractApiKey(HttpServletRequest request) {
        // Try X-API-Key header first
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            return Optional.of(apiKey);
        }

        // Fallback to Authorization: Bearer <key>
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            return Optional.of(authHeader.substring(7));
        }

        return Optional.empty();
    }

    private void updateSecurityContext(ApiKeyInfo apiKeyInfo, HttpServletRequest request) {
        var securityContext = new ApiKeySecurityContext();

        securityContext.setApplicationId(apiKeyInfo.applicationId());
        securityContext.setCompanyId(apiKeyInfo.companyId());
        securityContext.setApiKeyId(apiKeyInfo.apiKeyId());
        securityContext.setRateLimitPerMinute(apiKeyInfo.rateLimitPerMinute());

        var auth = new ApiKeyAuthenticationToken(apiKeyInfo);
        auth.setDetails(new WebAuthenticationDetails(request));
        securityContext.setAuthentication(auth);

        SecurityContextHolder.setContext(securityContext);

        LOGGER.debug("Authenticated API key for application {}", apiKeyInfo.applicationId());
    }
}
