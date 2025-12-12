package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.AuthenticationResponse;
import io.hephaistos.flagforge.controller.dto.CustomerAuthenticationRequest;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;

public interface AuthenticationService {

    void register(CustomerRegistrationRequest customerRegistrationRequest);

    AuthenticationResponse login(CustomerAuthenticationRequest customerAuthenticationRequest);

}