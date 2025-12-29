package io.hephaistos.flagforge.configuration;

import io.hephaistos.flagforge.controller.security.DefaultAuthenticationEntryPoint;
import io.hephaistos.flagforge.controller.security.JwtOncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ApiSecurityConfiguration {

    private static final String[] WHITELIST_POST_ENDPOINTS =
            {"/v1/auth/register", "/v1/auth/authenticate", "/v1/auth/verify-registration",
                    "/v1/auth/resend-verification", "/v1/password-reset/request",
                    "/v1/password-reset/reset", "/v1/email-verification/confirm"};
    private static final String[] WHITELIST_GET_ENDPOINTS =
            {"/v3/api-docs", "/v1/invite/**", "/v1/password-reset/validate",
                    "/v1/email-verification/validate"};

    private final JwtOncePerRequestFilter jwtOncePerRequestFilter;
    private final String allowedOrigins;

    public ApiSecurityConfiguration(JwtOncePerRequestFilter jwtOncePerRequestFilter,
            @Value("${flagforge.security.cors.allowed-origins}") String allowedOrigins) {
        this.jwtOncePerRequestFilter = jwtOncePerRequestFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // Prevent clickjacking attacks
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        // Prevent MIME type sniffing
                        .contentTypeOptions(Customizer.withDefaults())
                        // Enable XSS protection (legacy, but still useful for older browsers)
                        .xssProtection(Customizer.withDefaults())
                        // HTTP Strict Transport Security - only over HTTPS
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true)
                                .maxAgeInSeconds(31536000))) // 1 year
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(HttpMethod.POST, WHITELIST_POST_ENDPOINTS)
                                .permitAll()
                                .requestMatchers(HttpMethod.GET, WHITELIST_GET_ENDPOINTS)
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                        new DefaultAuthenticationEntryPoint()))
                .addFilterBefore(jwtOncePerRequestFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configures CORS with explicit allowed origins instead of permitting all. Origins are
     * configured via the flagforge.security.cors.allowed-origins property. Multiple origins can be
     * specified as a comma-separated list.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated origins and set them explicitly
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);

        // Allow standard HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow common headers including Authorization for JWT
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Defines the role hierarchy: ADMIN > DEV > READ_ONLY. Higher roles automatically inherit
     * permissions of lower roles.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN")
                .implies("DEV")
                .role("DEV")
                .implies("READ_ONLY")
                .build();
    }

}
