package io.hephaistos.flagforge.controller.security;

import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import io.hephaistos.flagforge.security.FlagForgeUserDetails;
import io.hephaistos.flagforge.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.isNull;

@Component
public class JwtOncePerRequestFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtOncePerRequestFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtOncePerRequestFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    @NullMarked
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        extractJwtToken(request).filter(jwtService::validateToken)
                .map(jwtService::decomposeToken)
                .ifPresent(email -> updateSecurityContext(email, request));

        filterChain.doFilter(request, response);
    }

    /**
     * From the request, we check if there is an authorization header, and if there is one, we
     * extract its value. As it has the prefix 'Bearer ', we use substring to ignore the first 7
     * chars of it.
     */
    private Optional<String> extractJwtToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(AUTHORIZATION_HEADER))
                .filter(header -> header.startsWith("Bearer "))
                .filter(header -> header.length() > 7)
                .map(header -> header.substring(7));
    }

    /**
     * Updates the security context for the currently logged in user. Using the email provided, we
     * fetch the user as well as any details that are relevant for multi-tenancy filtering.
     */
    private void updateSecurityContext(String email, HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isNull(authentication) || !email.equals(authentication.getName())) {
            var userDetails = (FlagForgeUserDetails) userDetailsService.loadUserByUsername(email);
            var newAuthorization = new UsernamePasswordAuthenticationToken(userDetails, null,
                    userDetails.getAuthorities());
            newAuthorization.setDetails(new WebAuthenticationDetails(request));

            var securityContext = new FlagForgeSecurityContext();
            securityContext.setAuthentication(newAuthorization);
            securityContext.setCustomerName(email);
            securityContext.setCustomerId(userDetails.getCustomerId());
            userDetails.getCompanyId().ifPresent(securityContext::setCompanyId);
            securityContext.setAccessibleApplicationIds(userDetails.getAccessibleApplicationIds());

            SecurityContextHolder.setContext(securityContext);
        }
        else {
            LOGGER.info("Authentication not required, already so");
        }
    }
}
