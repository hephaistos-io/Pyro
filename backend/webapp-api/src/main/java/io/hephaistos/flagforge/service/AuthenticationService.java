package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.AuthenticationResponse;
import io.hephaistos.flagforge.controller.dto.UserAuthenticationRequest;
import io.hephaistos.flagforge.controller.dto.UserRegistrationRequest;

public interface AuthenticationService {

    void register(UserRegistrationRequest userRegistrationRequest);

    AuthenticationResponse login(UserAuthenticationRequest userAuthenticationRequest);

}
