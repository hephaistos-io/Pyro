package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.EmailChangeTokenEntity;
import io.hephaistos.flagforge.controller.dto.EmailChangeResponse;

/**
 * Service for handling email change operations with verification.
 */
public interface EmailChangeService {

    /**
     * Requests an email change for the currently authenticated user. Sends verification to the new
     * email address.
     *
     * @param newEmail the new email address to change to
     * @param baseUrl  the base URL for constructing the verification link
     * @return the email change response
     * @throws io.hephaistos.flagforge.exception.DuplicateResourceException if email is already
     *                                                                      taken
     */
    EmailChangeResponse requestEmailChange(String newEmail, String baseUrl);

    /**
     * Validates an email change token.
     *
     * @param token the token to validate
     * @return the token entity if valid
     * @throws io.hephaistos.flagforge.exception.InvalidTokenException if token is invalid
     */
    EmailChangeTokenEntity validateToken(String token);

    /**
     * Confirms the email change after verification.
     *
     * @param token the verification token
     * @throws io.hephaistos.flagforge.exception.InvalidTokenException      if token is invalid
     * @throws io.hephaistos.flagforge.exception.DuplicateResourceException if email was taken in
     *                                                                      the meantime
     */
    void confirmEmailChange(String token);
}
