package io.hephaistos.pyro.controller;

import io.hephaistos.pyro.controller.dto.AuthenticationResponse;
import io.hephaistos.pyro.controller.dto.UserAuthenticationRequest;
import io.hephaistos.pyro.controller.dto.UserRegistrationRequest;
import io.hephaistos.pyro.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "authorization")
@Tag(name = "v1")
public class AuthorizationController {

    private final AuthenticationService authenticationService;

    public AuthorizationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Operation(summary = "Send registration request")
    @PostMapping(value = "/register", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody UserRegistrationRequest userRegistrationRequest) {
        authenticationService.register(userRegistrationRequest);
    }

    @Operation(summary = "Authenticate user")
    @PostMapping(value = "/authenticate", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public AuthenticationResponse authenticate(
            @Valid @RequestBody UserAuthenticationRequest userAuthenticationRequest) {
        return authenticationService.login(userAuthenticationRequest);
    }
}
