package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.PasswordResetTokenEntity;
import io.hephaistos.flagforge.controller.dto.PasswordResetResponse;

import java.util.Optional;

/**
 * Service for handling password reset operations.
 */
public interface PasswordResetService {

    /**
     * Requests a password reset for the given email. Returns the reset response if the email
     * exists, empty otherwise. Callers should always return success to prevent account
     * enumeration.
     *
     * @param email   the email address to reset password for
     * @param baseUrl the base URL for constructing the reset link
     * @return the password reset response, or empty if email not found
     */
    Optional<PasswordResetResponse> requestPasswordReset(String email, String baseUrl);

    /**
     * Requests a password reset for the currently authenticated user.
     *
     * @param baseUrl the base URL for constructing the reset link
     * @return the password reset response
     */
    PasswordResetResponse requestPasswordResetAuthenticated(String baseUrl);

    /**
     * Validates a password reset token.
     *
     * @param token the token to validate
     * @return the token entity if valid
     * @throws io.hephaistos.flagforge.exception.InvalidTokenException if token is invalid
     */
    PasswordResetTokenEntity validateToken(String token);

    /**
     * Executes the password reset - changes password and invalidates all sessions.
     *
     * @param token       the reset token
     * @param newPassword the new password
     * @throws io.hephaistos.flagforge.exception.InvalidTokenException     if token is invalid
     * @throws io.hephaistos.flagforge.exception.BreachedPasswordException if password is in breach
     *                                                                     database
     */
    void resetPassword(String token, String newPassword);
}
