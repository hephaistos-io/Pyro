package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.CustomerAuthenticationRequest;

import java.time.Instant;

public interface JwtService {

    String generateToken(CustomerAuthenticationRequest customerAuthenticationRequest);

    String decomposeToken(String token);

    boolean validateToken(String token);

    /**
     * Extracts the issued-at timestamp from a JWT token.
     *
     * @param token the JWT token
     * @return the instant when the token was issued
     */
    Instant getTokenIssuedAt(String token);
}