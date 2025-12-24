package io.hephaistos.flagforge.customerapi.controller.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hephaistos.flagforge.customerapi.security.ApiKeySecurityContext;
import io.hephaistos.flagforge.customerapi.service.RateLimitService;
import io.hephaistos.flagforge.customerapi.service.RateLimitService.RateLimitResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that enforces rate limits and tracks usage for authenticated requests. Must run after
 * ApiKeyOncePerRequestFilter to have access to the security context.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Only apply rate limiting to authenticated requests
        if (!(SecurityContextHolder.getContext() instanceof ApiKeySecurityContext securityContext)) {
            filterChain.doFilter(request, response);
            return;
        }

        var environmentId = securityContext.getEnvironmentId();
        var rateLimitPerSecond = securityContext.getRateLimitPerSecond();
        var requestsPerMonth = securityContext.getRequestsPerMonth();

        // Check rate limit
        RateLimitResult result = rateLimitService.tryConsume(environmentId, rateLimitPerSecond);

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitPerSecond));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));

        if (!result.allowed()) {
            // Track rejected request
            rateLimitService.incrementRejectedRequests(environmentId);

            long retryAfterSeconds = (result.retryAfterMillis() + 999) / 1000;
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            LOGGER.debug("Rate limit exceeded for environment {}, retry after {}ms", environmentId,
                    result.retryAfterMillis());
            var errorResponse = new ErrorResponse("RATE_LIMIT_EXCEEDED",
                    "Rate limit exceeded. Maximum " + rateLimitPerSecond + " requests per second.");
            OBJECT_MAPPER.writeValue(response.getOutputStream(), errorResponse);
            return;
        }

        // Increment usage counters (all fail gracefully due to fail-open)
        long monthlyUsage = rateLimitService.incrementMonthlyUsage(environmentId);
        rateLimitService.incrementDailyUsage(environmentId);
        rateLimitService.trackPeakBurst(environmentId);

        response.setHeader("X-Monthly-Usage", String.valueOf(monthlyUsage));
        response.setHeader("X-Monthly-Limit", String.valueOf(requestsPerMonth));

        // Continue with request
        filterChain.doFilter(request, response);
    }


    public record ErrorResponse(String code, String message) {
    }
}
