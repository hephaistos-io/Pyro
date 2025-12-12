package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.AuthenticationResponse;
import io.hephaistos.flagforge.controller.dto.CustomerAuthenticationRequest;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
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
    private final CustomerService customerService;

    public DefaultAuthenticationService(AuthenticationManager authenticationManager,
            JwtService jwtService, CustomerService customerService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.customerService = customerService;
    }

    @Override
    public void register(CustomerRegistrationRequest customerRegistrationRequest) {
        LOGGER.info("Registering customer: {}", customerRegistrationRequest);
        customerService.registerCustomer(customerRegistrationRequest);
    }

    @Override
    public AuthenticationResponse login(
            CustomerAuthenticationRequest customerAuthenticationRequest) {
        LOGGER.info("Customer logging in: {}", customerAuthenticationRequest.email());
        var authorization = authenticationManager.authenticate(
                customerAuthenticationRequest.toUsernamePasswordAuthenticationToken());
        SecurityContextHolder.getContext().setAuthentication(authorization);

        return new AuthenticationResponse(customerAuthenticationRequest.email(),
                jwtService.generateToken(customerAuthenticationRequest));
    }
}
