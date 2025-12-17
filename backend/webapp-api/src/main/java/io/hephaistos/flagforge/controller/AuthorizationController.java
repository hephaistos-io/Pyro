package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.controller.dto.AuthenticationResponse;
import io.hephaistos.flagforge.controller.dto.CustomerAuthenticationRequest;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
import io.hephaistos.flagforge.service.AuthenticationService;
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
public class AuthorizationController {

    private final AuthenticationService authenticationService;

    public AuthorizationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Operation(summary = "Send registration request")
    @PostMapping(value = "/register", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void register(
            @Valid @RequestBody CustomerRegistrationRequest customerRegistrationRequest) {
        authenticationService.register(customerRegistrationRequest);
    }

    @Operation(summary = "Authenticate customer")
    @PostMapping(value = "/authenticate", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public AuthenticationResponse authenticate(
            @Valid @RequestBody CustomerAuthenticationRequest customerAuthenticationRequest) {
        return authenticationService.login(customerAuthenticationRequest);
    }
}
