package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.configuration.EmailConfiguration;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    private static final String FROM_ADDRESS = "noreply@flagforge.dev";
    private static final String FROM_NAME = "FlagForge";
    private static final String BASE_URL = "http://localhost";
    private static final String TEST_EMAIL = "user@example.com";
    private static final String RESET_URL = "http://localhost/reset-password?token=abc123";
    private static final String VERIFICATION_URL = "http://localhost/verify-email?token=def456";

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private EmailTemplateService templateService;

    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        var config = new EmailConfiguration();
        config.setFromAddress(FROM_ADDRESS);
        config.setFromName(FROM_NAME);
        config.setBaseUrl(BASE_URL);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.processTemplate(anyString(), anyMap())).thenReturn(
                "<html>Test</html>");

        emailService = new SmtpEmailService(mailSender, config, templateService);
    }

    @Test
    void sendPasswordResetEmail_sendsEmail() {
        emailService.sendPasswordResetEmail(TEST_EMAIL, RESET_URL);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_usesCorrectTemplate() {
        emailService.sendPasswordResetEmail(TEST_EMAIL, RESET_URL);

        verify(templateService).processTemplate(eq("password-reset.html"),
                eq(Map.of("ACTION_URL", RESET_URL)));
    }

    @Test
    void sendEmailChangeVerification_sendsEmail() {
        emailService.sendEmailChangeVerification(TEST_EMAIL, VERIFICATION_URL);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmailChangeVerification_usesCorrectTemplate() {
        emailService.sendEmailChangeVerification(TEST_EMAIL, VERIFICATION_URL);

        verify(templateService).processTemplate(eq("email-verification.html"),
                eq(Map.of("ACTION_URL", VERIFICATION_URL)));
    }

    @Test
    void sendRegistrationVerificationEmail_sendsEmail() {
        emailService.sendRegistrationVerificationEmail(TEST_EMAIL, VERIFICATION_URL);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendRegistrationVerificationEmail_usesCorrectTemplate() {
        emailService.sendRegistrationVerificationEmail(TEST_EMAIL, VERIFICATION_URL);

        verify(templateService).processTemplate(eq("registration-verification.html"),
                eq(Map.of("ACTION_URL", VERIFICATION_URL)));
    }

    @Test
    void sendPasswordResetEmail_throwsOnMailException() {
        doThrow(new MailSendException("SMTP connection failed")).when(mailSender)
                .send(any(MimeMessage.class));

        assertThatThrownBy(
                () -> emailService.sendPasswordResetEmail(TEST_EMAIL, RESET_URL)).isInstanceOf(
                        SmtpEmailService.EmailSendException.class)
                .hasMessageContaining("Failed to send email");
    }

    @Test
    void sendEmailChangeVerification_throwsOnMailException() {
        doThrow(new MailSendException("SMTP connection failed")).when(mailSender)
                .send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendEmailChangeVerification(TEST_EMAIL,
                VERIFICATION_URL)).isInstanceOf(SmtpEmailService.EmailSendException.class)
                .hasMessageContaining("Failed to send email");
    }

    @Test
    void sendPasswordResetEmail_callsSendOnce() {
        emailService.sendPasswordResetEmail(TEST_EMAIL, RESET_URL);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailChangeVerification_callsSendOnce() {
        emailService.sendEmailChangeVerification(TEST_EMAIL, VERIFICATION_URL);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendMultipleEmails_eachCreatesNewMessage() {
        emailService.sendPasswordResetEmail(TEST_EMAIL, RESET_URL);
        emailService.sendEmailChangeVerification(TEST_EMAIL, VERIFICATION_URL);

        verify(mailSender, times(2)).createMimeMessage();
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }
}
