package io.hephaistos.flagforge.configuration;


import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("flagforge.security.jwt")
public class JwtConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtConfiguration.class);

    /**
     * The default secret used for local development only. In production, set the
     * FLAGFORGE_JWT_SECRET environment variable.
     */
    private static final String INSECURE_DEFAULT_SECRET =
            "4407a837731d92425e627e49632afbabd538a25e080fafed65a9ff7e71a9f5d1";

    private String secret;

    private int expirationDurationSeconds;

    @PostConstruct
    public void validateConfiguration() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret must be configured. Set the FLAGFORGE_JWT_SECRET environment variable.");
        }

        if (secret.equals(INSECURE_DEFAULT_SECRET)) {
            LOGGER.warn("========================================");
            LOGGER.warn("SECURITY WARNING: Using default JWT secret!");
            LOGGER.warn("This is insecure for production use.");
            LOGGER.warn("Set FLAGFORGE_JWT_SECRET environment variable.");
            LOGGER.warn("========================================");
        }

        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters long for security.");
        }
    }

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
