package io.hephaistos.flagforge.service;

import java.util.UUID;

/**
 * Service for handling email verification during registration.
 */
public interface RegistrationVerificationService {

    /**
     * Creates a verification token and sends a verification email to the customer.
     *
     * @param customerId the ID of the customer to verify
     * @param email      the email address to send the verification to
     */
    void sendVerificationEmail(UUID customerId, String email);

    /**
     * Verifies a registration token and marks the customer as email-verified.
     *
     * @param token the verification token
     * @throws io.hephaistos.flagforge.exception.InvalidTokenException if the token is invalid,
     *                                                                 expired, or already used
     */
    void verifyToken(String token);

    /**
     * Resends the verification email for a customer.
     *
     * @param email the email address of the customer
     * @throws io.hephaistos.flagforge.exception.NotFoundException if no customer is found with this
     *                                                             email
     * @throws IllegalStateException                               if the customer is already
     *                                                             verified
     */
    void resendVerificationEmail(String email);
}
