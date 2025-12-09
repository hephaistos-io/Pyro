package io.hephaistos.pyro.configuration;

import io.hephaistos.pyro.controller.security.DefaultAuthenticationEntryPoint;
import io.hephaistos.pyro.controller.security.JwtOncePerRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
public class ApiSecurityConfiguration {

    private static final String[] WHITELIST_POST_ENDPOINTS =
            {"/v1/auth/register", "/v1/auth/authenticate"};
    private static final String[] WHITELIST_GET_ENDPOINTS = {"/v3/api-docs"};

    private final JwtOncePerRequestFilter jwtOncePerRequestFilter;

    public ApiSecurityConfiguration(JwtOncePerRequestFilter jwtOncePerRequestFilter) {
        this.jwtOncePerRequestFilter = jwtOncePerRequestFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
        return source;
    }

}
