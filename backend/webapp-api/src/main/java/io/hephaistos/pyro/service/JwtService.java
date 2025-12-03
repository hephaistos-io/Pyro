package io.hephaistos.pyro.service;

import io.hephaistos.pyro.controller.dto.UserAuthenticationRequest;

public interface JwtService {

    String generateToken(UserAuthenticationRequest userAuthenticationRequest);

    String decomposeToken(String token);

    boolean validateToken(String token);
}
