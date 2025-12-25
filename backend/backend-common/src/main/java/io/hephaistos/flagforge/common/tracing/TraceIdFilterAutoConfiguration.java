package io.hephaistos.flagforge.common.tracing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the TraceIdFilter. Only activates in servlet-based web applications.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TraceIdFilterAutoConfiguration {

    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }
}
