package io.hephaistos.flagforge.common.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that extracts or generates a trace ID for each request. The trace ID is placed in the
 * SLF4J MDC for inclusion in log messages.
 * <p>
 * Must run before all other filters to ensure trace ID is available throughout the request
 * lifecycle.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";
    private static final Logger LOGGER = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String traceId = request.getHeader(TRACE_ID_HEADER);

        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
            LOGGER.debug("Generated new trace ID: {}", traceId);
        }
        else {
            LOGGER.debug("Received trace ID from header: {}", traceId);
        }

        // Put trace ID in MDC for logging
        MDC.put(MDC_TRACE_ID_KEY, traceId);

        // Add trace ID to response header for debugging/correlation
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent memory leaks in pooled threads
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }

    private String generateTraceId() {
        // Use UUID without dashes for compactness (32 hex characters)
        return UUID.randomUUID().toString().replace("-", "");
    }
}
