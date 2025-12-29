package io.hephaistos.flagforge.service;

/**
 * Service for sending emails. Uses SMTP for sending emails via JavaMailSender.
 *
 * <p>Configuration:
 * <ul>
 *     <li>Local/Dev/Test: Mailpit on localhost:1025</li>
 *     <li>Production: Scaleway TEM via SMTP (smtp.tem.scaleway.com:465)</li>
 * </ul>
 */
public interface EmailService {

    /**
     * Sends a password reset email with a link to reset the password.
     *
     * @param email    the recipient's email address
     * @param resetUrl the password reset URL
     */
    void sendPasswordResetEmail(String email, String resetUrl);

    /**
     * Sends an email change verification email to the new email address.
     *
     * @param newEmail        the new email address to verify
     * @param verificationUrl the verification URL
     */
    void sendEmailChangeVerification(String newEmail, String verificationUrl);

    /**
     * Sends a registration verification email to confirm the user's email address.
     *
     * @param email           the recipient's email address
     * @param verificationUrl the verification URL
     */
    void sendRegistrationVerificationEmail(String email, String verificationUrl);

    /**
     * Sends an invitation email to join a company.
     *
     * @param email       the recipient's email address
     * @param inviteUrl   the invite URL
     * @param companyName the name of the company the user is being invited to
     * @param roleName    the display name of the role being assigned (e.g., "Admin", "Developer")
     */
    void sendInviteEmail(String email, String inviteUrl, String companyName, String roleName);
}
