package io.hephaistos.flagforge.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Custom UserDetails implementation that holds all security-relevant user data. This eliminates the
 * need for additional database calls after authentication by carrying customer ID, company ID, and
 * accessible application IDs.
 */
public class FlagForgeUserDetails implements UserDetails {

    private final String email;
    private final String password;
    private final UUID customerId;
    private final UUID companyId;
    private final Set<UUID> accessibleApplicationIds;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Instant passwordChangedAt;

    public FlagForgeUserDetails(String email, String password, UUID customerId, UUID companyId,
            Set<UUID> accessibleApplicationIds, Collection<? extends GrantedAuthority> authorities,
            Instant passwordChangedAt) {
        this.email = email;
        this.password = password;
        this.customerId = customerId;
        this.companyId = companyId;
        this.accessibleApplicationIds =
                accessibleApplicationIds != null ? accessibleApplicationIds : Set.of();
        this.authorities = authorities != null ? authorities : Set.of();
        this.passwordChangedAt = passwordChangedAt;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }


    public UUID getCustomerId() {
        return customerId;
    }

    public Optional<UUID> getCompanyId() {
        return Optional.ofNullable(companyId);
    }

    public Set<UUID> getAccessibleApplicationIds() {
        return accessibleApplicationIds;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }
}
