package io.hephaistos.flagforge.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("flagforge.security.jwt")
public class JwtConfiguration {

    private String secret;

    private int expirationDurationSeconds;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getExpirationDurationSeconds() {
        return expirationDurationSeconds;
    }

    public void setExpirationDurationSeconds(int expirationDurationSeconds) {
        this.expirationDurationSeconds = expirationDurationSeconds;
    }
}
