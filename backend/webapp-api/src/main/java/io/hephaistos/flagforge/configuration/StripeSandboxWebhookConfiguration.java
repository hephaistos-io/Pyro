package io.hephaistos.flagforge.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for loading Stripe webhook secret from file in sandbox mode.
 * <p>
 * In sandbox mode with Docker, the stripe-cli container writes the dynamically
 * generated webhook signing secret to a shared volume. This configuration reads
 * that file and updates the StripeConfiguration with the correct secret.
 * <p>
 * This is only active when {@code flagforge.stripe.mode=sandbox} AND a file path
 * is configured via {@code flagforge.stripe.webhook-secret-file}.
 */
@Configuration
@ConditionalOnProperty(name = "flagforge.stripe.mode", havingValue = "sandbox")
public class StripeSandboxWebhookConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StripeSandboxWebhookConfiguration.class);

    private final StripeConfiguration stripeConfiguration;

    @Value("${flagforge.stripe.webhook-secret-file:}")
    private String webhookSecretFile;

    public StripeSandboxWebhookConfiguration(StripeConfiguration stripeConfiguration) {
        this.stripeConfiguration = stripeConfiguration;
    }

    @PostConstruct
    public void loadWebhookSecretFromFile() {
        if (webhookSecretFile == null || webhookSecretFile.isEmpty()) {
            log.info("Sandbox mode: No webhook secret file configured, using environment variable");
            return;
        }

        Path path = Path.of(webhookSecretFile);
        if (!Files.exists(path)) {
            log.warn("Sandbox mode: Webhook secret file not found at {}, using environment variable", webhookSecretFile);
            return;
        }

        try {
            String fileSecret = Files.readString(path).trim();
            if (fileSecret.isEmpty()) {
                log.warn("Sandbox mode: Webhook secret file is empty, using environment variable");
                return;
            }

            stripeConfiguration.setWebhookSecret(fileSecret);
            log.info("Sandbox mode: Loaded webhook secret from file {}", webhookSecretFile);
        } catch (IOException e) {
            log.error("Sandbox mode: Failed to read webhook secret from file {}, using environment variable",
                    webhookSecretFile, e);
        }
    }
}
