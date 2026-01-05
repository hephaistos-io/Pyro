package io.hephaistos.flagforge.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for email sending. SMTP settings are handled by Spring Mail's auto-configuration
 * via spring.mail.* properties.
 */
@Configuration
@ConfigurationProperties("flagforge.email")
public class EmailConfiguration {

    /**
     * Email address used as the "From" address for outgoing emails.
     */
    private String fromAddress = "noreply@flagforge.dev";

    /**
     * Display name used in the "From" field of outgoing emails.
     */
    private String fromName = "FlagForge";

    /**
     * Base URL of the application, used for generating links in emails.
     */
    private String baseUrl = "http://localhost";

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
