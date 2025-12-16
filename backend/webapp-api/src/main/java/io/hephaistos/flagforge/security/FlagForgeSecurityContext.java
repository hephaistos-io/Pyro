package io.hephaistos.flagforge.security;

import io.hephaistos.flagforge.data.CustomerRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class FlagForgeSecurityContext implements SecurityContext {

    private String customerName;

    private Authentication authentication;

    /**
     * Returns the current FlagForgeSecurityContext from the SecurityContextHolder. This is a
     * convenience method to avoid casting in multiple places.
     *
     * @return the current FlagForgeSecurityContext
     */
    public static FlagForgeSecurityContext getCurrent() {
        return (FlagForgeSecurityContext) SecurityContextHolder.getContext();
    }
    private UUID userId;
    private UUID companyId;
    private Set<UUID> accessibleApplicationIds = Set.of();

    @Override
    public @Nullable Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public void setAuthentication(@Nullable Authentication authentication) {
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
        if (isNotBlank(companyId)) {
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
     * @return the CustomerRole, or READ_ONLY if no role authority is found
     */
    public CustomerRole getRole() {
        if (authentication == null) {
            return CustomerRole.READ_ONLY;
        }
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith(ROLE_PREFIX))
                .map(auth -> auth.substring(ROLE_PREFIX.length()))
                .filter(roleName -> {
                    try {
                        CustomerRole.valueOf(roleName);
                        return true;
                    }
                    catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .findFirst()
                .map(CustomerRole::valueOf)
                .orElse(CustomerRole.READ_ONLY);
    }
}
