package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.UserAuthenticationRequest;

public interface JwtService {

    String generateToken(UserAuthenticationRequest userAuthenticationRequest);

    String decomposeToken(String token);

    boolean validateToken(String token);
}
