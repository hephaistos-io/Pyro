package io.hephaistos.flagforge.security;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class FlagForgeSecurityContext implements SecurityContext {

    private Authentication authentication;
    private String userName;
    private UUID userId;
    private UUID companyId;

    @Override
    public @Nullable Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public void setAuthentication(@Nullable Authentication authentication) {
        this.authentication = authentication;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Optional<UUID> getCompanyId() {
        return Optional.ofNullable(companyId);
    }

    public void setCompanyId(String companyId) {
        if (isNotBlank(companyId)) {
            this.companyId = UUID.fromString(companyId);
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(@NotNull @NotEmpty String userId) {
        this.userId = UUID.fromString(userId);
    }
}