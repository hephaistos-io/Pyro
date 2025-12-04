package io.hephaistos.pyro.service;

import io.hephaistos.pyro.controller.dto.AuthenticationResponse;
import io.hephaistos.pyro.controller.dto.UserAuthenticationRequest;
import io.hephaistos.pyro.controller.dto.UserRegistrationRequest;

public interface AuthenticationService {

    void register(UserRegistrationRequest userRegistrationRequest);

    AuthenticationResponse login(UserAuthenticationRequest userAuthenticationRequest);

}
