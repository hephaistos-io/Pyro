package io.hephaistos.flagforge.customerapi.controller.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.hephaistos.flagforge.customerapi.security.ApiKeySecurityContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_EXCEEDED_MESSAGE = """
            {"code": "RATE_LIMIT_EXCEEDED", "message": "Too many requests. Please slow down."}
            """;

    private final ConcurrentHashMap<UUID, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    @NullMarked
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        var context = SecurityContextHolder.getContext();

        if (!(context instanceof ApiKeySecurityContext apiContext)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID apiKeyId = apiContext.getApiKeyId();
        int limitPerMinute = apiContext.getRateLimitPerMinute();

        Bucket bucket = buckets.computeIfAbsent(apiKeyId, id -> createBucket(limitPerMinute));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        }
        else {
            LOGGER.warn("Rate limit exceeded for API key {}", apiKeyId);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write(RATE_LIMIT_EXCEEDED_MESSAGE);
        }
    }

    private Bucket createBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
