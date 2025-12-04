package io.hephaistos.pyro.service;

import io.hephaistos.pyro.controller.dto.AuthenticationResponse;
import io.hephaistos.pyro.controller.dto.UserAuthenticationRequest;
import io.hephaistos.pyro.controller.dto.UserRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DefaultAuthenticationService implements AuthenticationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DefaultAuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public DefaultAuthenticationService(AuthenticationManager authenticationManager,
            JwtService jwtService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    public void register(UserRegistrationRequest userRegistrationRequest) {
        LOGGER.info("Registering user: {}", userRegistrationRequest);
        userService.registerUser(userRegistrationRequest);
    }

    @Override
    public AuthenticationResponse login(UserAuthenticationRequest userAuthenticationRequest) {
        LOGGER.info("User logging in: {}", userAuthenticationRequest.email());
        var authorization = authenticationManager.authenticate(
                userAuthenticationRequest.toUsernamePasswordAuthenticationToken());
        SecurityContextHolder.getContext().setAuthentication(authorization);

        return new AuthenticationResponse(userAuthenticationRequest.email(),
                jwtService.generateToken(userAuthenticationRequest));
    }
}
