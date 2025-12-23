package io.hephaistos.flagforge.customerapi.configuration;

import io.hephaistos.flagforge.customerapi.controller.security.ApiKeyAuthenticationEntryPoint;
import io.hephaistos.flagforge.customerapi.controller.security.ApiKeyOncePerRequestFilter;
import io.hephaistos.flagforge.customerapi.controller.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class ApiSecurityConfiguration {

    private static final String[] WHITELIST_GET_ENDPOINTS =
            {"/v3/api-docs", "/v3/api-docs/**", "/health"};

    private final ApiKeyOncePerRequestFilter apiKeyOncePerRequestFilter;
    private final RateLimitFilter rateLimitFilter;

    public ApiSecurityConfiguration(ApiKeyOncePerRequestFilter apiKeyOncePerRequestFilter,
            RateLimitFilter rateLimitFilter) {
        this.apiKeyOncePerRequestFilter = apiKeyOncePerRequestFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(HttpMethod.GET, WHITELIST_GET_ENDPOINTS)
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                        new ApiKeyAuthenticationEntryPoint()))
                .addFilterBefore(apiKeyOncePerRequestFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, ApiKeyOncePerRequestFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.applyPermitDefaultValues();
        configuration.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
