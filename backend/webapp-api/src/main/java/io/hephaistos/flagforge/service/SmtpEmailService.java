package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.configuration.EmailConfiguration;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * SMTP-based email service implementation. Sends emails via SMTP server configured in
 * application.yml.
 *
 * <p>Environment configuration:
 * <ul>
 *     <li>Local/Dev/Test: Mailpit on localhost:1025</li>
 *     <li>Production: Scaleway TEM on smtp.tem.scaleway.com:465</li>
 * </ul>
 *
 * <p>Email templates are loaded from resources/templates/email/ and can be edited
 * independently without modifying code.
 */
@Service
public class SmtpEmailService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailService.class);

    private static final String TEMPLATE_PASSWORD_RESET = "password-reset.html";
    private static final String TEMPLATE_EMAIL_VERIFICATION = "email-verification.html";
    private static final String TEMPLATE_REGISTRATION_VERIFICATION =
            "registration-verification.html";
    private static final String TEMPLATE_INVITE = "invite.html";

    private final JavaMailSender mailSender;
    private final EmailConfiguration config;
    private final EmailTemplateService templateService;

    public SmtpEmailService(JavaMailSender mailSender, EmailConfiguration config,
            EmailTemplateService templateService) {
        this.mailSender = mailSender;
        this.config = config;
        this.templateService = templateService;
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetUrl) {
        String subject = "Reset your Flagforge password";
        String htmlContent = templateService.processTemplate(TEMPLATE_PASSWORD_RESET,
                Map.of("ACTION_URL", resetUrl));
        sendHtmlEmail(email, subject, htmlContent);
    }

    @Override
    public void sendEmailChangeVerification(String newEmail, String verificationUrl) {
        String subject = "Verify your new email address";
        String htmlContent = templateService.processTemplate(TEMPLATE_EMAIL_VERIFICATION,
                Map.of("ACTION_URL", verificationUrl));
        sendHtmlEmail(newEmail, subject, htmlContent);
    }

    @Override
    public void sendRegistrationVerificationEmail(String email, String verificationUrl) {
        String subject = "Verify your Flagforge account";
        String htmlContent = templateService.processTemplate(TEMPLATE_REGISTRATION_VERIFICATION,
                Map.of("ACTION_URL", verificationUrl));
        sendHtmlEmail(email, subject, htmlContent);
    }

    @Override
    public void sendInviteEmail(String email, String inviteUrl, String companyName,
            String roleName) {
        String subject = "You're invited to join " + companyName + " on Flagforge";
        String htmlContent = templateService.processTemplate(TEMPLATE_INVITE,
                Map.of("ACTION_URL", inviteUrl, "COMPANY_NAME", companyName, "ROLE", roleName));
        sendHtmlEmail(email, subject, htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(new InternetAddress(config.getFromAddress(), config.getFromName()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            LOGGER.info("Email sent successfully to: {}", to);

        }
        catch (MessagingException | UnsupportedEncodingException e) {
            LOGGER.error("Failed to create email message for: {}", to, e);
            throw new EmailSendException("Failed to create email message", e);
        }
        catch (MailException e) {
            LOGGER.error("Failed to send email to: {}", to, e);
            throw new EmailSendException("Failed to send email", e);
        }
    }

    /**
     * Exception thrown when email sending fails.
     */
    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
