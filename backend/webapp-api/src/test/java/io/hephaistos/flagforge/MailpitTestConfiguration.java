package io.hephaistos.flagforge;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Test configuration that provides a Mailpit Testcontainer for integration tests.
 * <p>
 * Mailpit is an SMTP testing tool with a REST API for verifying sent emails. The container is
 * shared across all tests for performance.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MailpitTestConfiguration {

    private static final int SMTP_PORT = 1025;
    private static final int API_PORT = 8025;

    private static final GenericContainer<?> MAILPIT =
            new GenericContainer<>("axllent/mailpit:latest").withExposedPorts(SMTP_PORT, API_PORT)
                    .waitingFor(Wait.forLogMessage(".*accessible via.*", 1));

    static {
        MAILPIT.start();
    }

    @Bean
    public GenericContainer<?> mailpitContainer() {
        return MAILPIT;
    }

    @Bean
    @Primary
    public JavaMailSender testMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(MAILPIT.getHost());
        mailSender.setPort(MAILPIT.getMappedPort(SMTP_PORT));
        return mailSender;
    }

    @Bean
    public MailpitClient mailpitClient() {
        String baseUrl = String.format("http://%s:%d/api/v1", MAILPIT.getHost(),
                MAILPIT.getMappedPort(API_PORT));
        return new MailpitClient(baseUrl);
    }
}
