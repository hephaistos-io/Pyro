package io.hephaistos.flagforge.customerapi.controller.security;

import io.hephaistos.flagforge.common.data.ApiKeyEntity;
import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.customerapi.data.repository.ApiKeyRepository;
import io.hephaistos.flagforge.customerapi.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.customerapi.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.customerapi.exception.ApiKeyExpiredException;
import io.hephaistos.flagforge.customerapi.exception.InvalidApiKeyException;
import io.hephaistos.flagforge.customerapi.security.ApiKeySecurityContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class ApiKeyOncePerRequestFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyOncePerRequestFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;
    private final EnvironmentRepository environmentRepository;

    public ApiKeyOncePerRequestFilter(ApiKeyRepository apiKeyRepository,
            ApplicationRepository applicationRepository,
            EnvironmentRepository environmentRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.applicationRepository = applicationRepository;
        this.environmentRepository = environmentRepository;
    }

    @Override
    @NullMarked
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Extract API key from header
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (isBlank(apiKey)) {
            // No API key provided, let Spring Security handle 401
            filterChain.doFilter(request, response);
            return;
        }

        // Query database for API key
        ApiKeyEntity apiKeyEntity = apiKeyRepository.findByKey(apiKey)
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API key"));

        // Validate expiration
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (apiKeyEntity.getExpirationDate().isBefore(now)) {
            throw new ApiKeyExpiredException("API key has expired");
        }

        // Get companyId from ApplicationEntity
        ApplicationEntity application =
                applicationRepository.findById(apiKeyEntity.getApplicationId())
                        .orElseThrow(() -> new InvalidApiKeyException(
                                "Application not found for API key"));

        // Get rate limits from EnvironmentEntity
        EnvironmentEntity environment =
                environmentRepository.findById(apiKeyEntity.getEnvironmentId())
                        .orElseThrow(() -> new InvalidApiKeyException(
                                "Environment not found for API key"));

        // Create Spring Security Authentication with authorities based on KeyType
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + apiKeyEntity.getKeyType().name()));
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                apiKeyEntity.getApplicationId(), // principal
                null, // credentials
                authorities);

        // Populate ApiKeySecurityContext
        ApiKeySecurityContext securityContext = new ApiKeySecurityContext();
        securityContext.setAuthentication(authToken);
        securityContext.setApiKeyId(apiKeyEntity.getId());
        securityContext.setApplicationId(apiKeyEntity.getApplicationId());
        securityContext.setCompanyId(application.getCompanyId());
        securityContext.setEnvironmentId(apiKeyEntity.getEnvironmentId());
        securityContext.setKeyType(apiKeyEntity.getKeyType());
        securityContext.setRateLimitPerSecond(environment.getRateLimitRequestsPerSecond());
        securityContext.setRequestsPerMonth(environment.getRequestsPerMonth());

        // Set context in SecurityContextHolder
        SecurityContextHolder.setContext(securityContext);

        LOGGER.debug("API key authentication successful for application: {}",
                apiKeyEntity.getApplicationId());

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
