package io.hephaistos.pyro.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

public class PyroSecurityContext implements SecurityContext {

    private Authentication authentication;
    private String userName;

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
}
