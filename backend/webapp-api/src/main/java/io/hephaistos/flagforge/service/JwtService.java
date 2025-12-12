package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.CustomerAuthenticationRequest;

public interface JwtService {

    String generateToken(CustomerAuthenticationRequest customerAuthenticationRequest);

    String decomposeToken(String token);

    boolean validateToken(String token);
}