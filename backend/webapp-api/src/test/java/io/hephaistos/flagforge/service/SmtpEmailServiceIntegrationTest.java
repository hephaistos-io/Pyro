package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.MailpitClient;
import io.hephaistos.flagforge.MailpitClient.MessageContent;
import io.hephaistos.flagforge.MailpitTestConfiguration;
import io.hephaistos.flagforge.configuration.EmailConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SmtpEmailService using Mailpit as a test SMTP server. These tests verify
 * actual email sending, content, and formatting.
 */
@SpringBootTest(classes = {SmtpEmailService.class, DefaultEmailTemplateService.class,
        EmailConfiguration.class})
@Import(MailpitTestConfiguration.class)
@Tag("integration")
class SmtpEmailServiceIntegrationTest {

    private static final String FROM_ADDRESS = "noreply@flagforge.io";
    private static final String FROM_NAME = "FlagForge";
    private static final String BASE_URL = "http://localhost";
    private static final String TEST_EMAIL = "user@example.com";
    private static final String RESET_TOKEN = "abc123def456";
    private static final String VERIFICATION_TOKEN = "xyz789";

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MailpitClient mailpitClient;

    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        mailpitClient.clearMailbox();

        EmailConfiguration config = new EmailConfiguration();
        config.setFromAddress(FROM_ADDRESS);
        config.setFromName(FROM_NAME);
        config.setBaseUrl(BASE_URL);

        EmailTemplateService templateService = new DefaultEmailTemplateService();

        emailService = new SmtpEmailService(mailSender, config, templateService);
    }

    @Test
    void sendPasswordResetEmail_sendsEmailWithCorrectRecipient() {
        String resetUrl = BASE_URL + "/reset-password?token=" + RESET_TOKEN;

        emailService.sendPasswordResetEmail(TEST_EMAIL, resetUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message).isNotNull();
        assertThat(message.to()).hasSize(1);
        assertThat(message.to().get(0).address()).isEqualTo(TEST_EMAIL);
    }

    @Test
    void sendPasswordResetEmail_hasCorrectSubject() {
        String resetUrl = BASE_URL + "/reset-password?token=" + RESET_TOKEN;

        emailService.sendPasswordResetEmail(TEST_EMAIL, resetUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message.subject()).isEqualTo("Reset your Flagforge password");
    }

    @Test
    void sendPasswordResetEmail_hasCorrectFromAddress() {
        String resetUrl = BASE_URL + "/reset-password?token=" + RESET_TOKEN;

        emailService.sendPasswordResetEmail(TEST_EMAIL, resetUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message.from().address()).isEqualTo(FROM_ADDRESS);
        assertThat(message.from().name()).isEqualTo(FROM_NAME);
    }

    @Test
    void sendPasswordResetEmail_containsResetUrl() {
        String resetUrl = BASE_URL + "/reset-password?token=" + RESET_TOKEN;

        emailService.sendPasswordResetEmail(TEST_EMAIL, resetUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message.html()).contains(resetUrl);
    }

    @Test
    void sendPasswordResetEmail_containsResetButton() {
        String resetUrl = BASE_URL + "/reset-password?token=" + RESET_TOKEN;

        emailService.sendPasswordResetEmail(TEST_EMAIL, resetUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        // Button text may have whitespace between words in HTML
        assertThat(message.html()).containsPattern("Reset\\s+Password");
        assertThat(message.html()).contains("href=\"" + resetUrl + "\"");
    }

    @Test
    void sendEmailChangeVerification_sendsEmailWithCorrectRecipient() {
        String verificationUrl = BASE_URL + "/verify-email?token=" + VERIFICATION_TOKEN;

        emailService.sendEmailChangeVerification(TEST_EMAIL, verificationUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message).isNotNull();
        assertThat(message.to()).hasSize(1);
        assertThat(message.to().get(0).address()).isEqualTo(TEST_EMAIL);
    }

    @Test
    void sendEmailChangeVerification_hasCorrectSubject() {
        String verificationUrl = BASE_URL + "/verify-email?token=" + VERIFICATION_TOKEN;

        emailService.sendEmailChangeVerification(TEST_EMAIL, verificationUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message.subject()).isEqualTo("Verify your new email address");
    }

    @Test
    void sendEmailChangeVerification_hasCorrectFromAddress() {
        String verificationUrl = BASE_URL + "/verify-email?token=" + VERIFICATION_TOKEN;

        emailService.sendEmailChangeVerification(TEST_EMAIL, verificationUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message.from().address()).isEqualTo(FROM_ADDRESS);
        assertThat(message.from().name()).isEqualTo(FROM_NAME);
    }

    @Test
    void sendEmailChangeVerification_containsVerificationUrl() {
        String verificationUrl = BASE_URL + "/verify-email?token=" + VERIFICATION_TOKEN;

        emailService.sendEmailChangeVerification(TEST_EMAIL, verificationUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message.html()).contains(verificationUrl);
    }

    @Test
    void sendEmailChangeVerification_containsVerifyButton() {
        String verificationUrl = BASE_URL + "/verify-email?token=" + VERIFICATION_TOKEN;

        emailService.sendEmailChangeVerification(TEST_EMAIL, verificationUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        // Button text may have whitespace between words in HTML
        assertThat(message.html()).containsPattern("Verify\\s+Email");
        assertThat(message.html()).contains("href=\"" + verificationUrl + "\"");
    }

    @Test
    void sendRegistrationVerificationEmail_sendsEmailWithCorrectRecipient() {
        String verificationUrl = BASE_URL + "/verify-registration?token=" + VERIFICATION_TOKEN;

        emailService.sendRegistrationVerificationEmail(TEST_EMAIL, verificationUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message).isNotNull();
        assertThat(message.to()).hasSize(1);
        assertThat(message.to().get(0).address()).isEqualTo(TEST_EMAIL);
    }

    @Test
    void sendRegistrationVerificationEmail_hasCorrectSubject() {
        String verificationUrl = BASE_URL + "/verify-registration?token=" + VERIFICATION_TOKEN;

        emailService.sendRegistrationVerificationEmail(TEST_EMAIL, verificationUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message.subject()).isEqualTo("Verify your Flagforge account");
    }

    @Test
    void sendRegistrationVerificationEmail_containsVerificationUrl() {
        String verificationUrl = BASE_URL + "/verify-registration?token=" + VERIFICATION_TOKEN;

        emailService.sendRegistrationVerificationEmail(TEST_EMAIL, verificationUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message.html()).contains(verificationUrl);
    }

    @Test
    void sendRegistrationVerificationEmail_containsWelcomeMessage() {
        String verificationUrl = BASE_URL + "/verify-registration?token=" + VERIFICATION_TOKEN;

        emailService.sendRegistrationVerificationEmail(TEST_EMAIL, verificationUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message.html()).contains("Welcome to Flagforge");
    }

    @Test
    void emails_areHtmlFormatted() {
        String resetUrl = BASE_URL + "/reset-password?token=" + RESET_TOKEN;

        emailService.sendPasswordResetEmail(TEST_EMAIL, resetUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        // HTML content is present
        assertThat(message.html()).isNotEmpty();
        assertThat(message.html()).contains("<html");
    }

    @Test
    void emails_containFlagforgeBranding() {
        String resetUrl = BASE_URL + "/reset-password?token=" + RESET_TOKEN;

        emailService.sendPasswordResetEmail(TEST_EMAIL, resetUrl);

        MessageContent message = mailpitClient.getLatestMessageForRecipient(TEST_EMAIL);
        assertThat(message.html()).contains("Flagforge");
    }

    @Test
    void multipleEmails_areAllReceived() {
        String resetUrl1 = BASE_URL + "/reset-password?token=token1";
        String resetUrl2 = BASE_URL + "/reset-password?token=token2";

        emailService.sendPasswordResetEmail("user1@example.com", resetUrl1);
        emailService.sendPasswordResetEmail("user2@example.com", resetUrl2);

        assertThat(mailpitClient.getMessageCount()).isEqualTo(2);
    }
}
