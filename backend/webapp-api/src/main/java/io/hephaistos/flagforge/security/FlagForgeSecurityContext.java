package io.hephaistos.flagforge.security;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
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
}
