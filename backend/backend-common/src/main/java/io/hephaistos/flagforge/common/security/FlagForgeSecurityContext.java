package io.hephaistos.flagforge.common.security;

import io.hephaistos.flagforge.common.enums.CustomerRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Custom security context that provides typed access to FlagForge-specific user information. This
 * context is stored in the SecurityContextHolder and populated during authentication.
 */
public class FlagForgeSecurityContext implements SecurityContext {

    private String customerName;
    private Authentication authentication;
    private UUID userId;
    private UUID companyId;
    private Set<UUID> accessibleApplicationIds = Set.of();

    /**
     * Returns the current FlagForgeSecurityContext from the SecurityContextHolder. This is a
     * convenience method to avoid casting in multiple places.
     *
     * @return the current FlagForgeSecurityContext
     */
    public static FlagForgeSecurityContext getCurrent() {
        return (FlagForgeSecurityContext) SecurityContextHolder.getContext();
    }

    @Override
    public Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Optional<UUID> getCompanyId() {
        return Optional.ofNullable(companyId);
    }

    public void setCompanyId(String companyId) {
        if (companyId != null && !companyId.isBlank()) {
            this.companyId = UUID.fromString(companyId);
        }
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public Set<UUID> getAccessibleApplicationIds() {
        return accessibleApplicationIds;
    }

    public void setAccessibleApplicationIds(Set<UUID> accessibleApplicationIds) {
        this.accessibleApplicationIds =
                accessibleApplicationIds != null ? accessibleApplicationIds : Set.of();
    }

    public UUID getCustomerId() {
        return userId;
    }

    public void setCustomerId(@NotNull @NotEmpty UUID userId) {
        this.userId = userId;
    }

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Returns the customer's role from the authentication authorities.
     *
     * @return the CustomerRole
     * @throws IllegalStateException if authentication is null or no valid role is found
     */
    public CustomerRole getRole() {
        if (authentication == null) {
            throw new IllegalStateException(
                    "Cannot determine role: authentication context is null");
        }
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth != null && auth.startsWith(ROLE_PREFIX))
                .map(auth -> auth.substring(ROLE_PREFIX.length()))
                .filter(roleName -> {
                    try {
                        CustomerRole.valueOf(roleName);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .findFirst()
                .map(CustomerRole::valueOf)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot determine role: no valid role authority found in authentication"));
    }
}
