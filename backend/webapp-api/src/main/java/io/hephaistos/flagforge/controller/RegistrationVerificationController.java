package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.service.RegistrationVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "registration-verification")
public class RegistrationVerificationController {

    private final RegistrationVerificationService registrationVerificationService;

    public RegistrationVerificationController(
            RegistrationVerificationService registrationVerificationService) {
        this.registrationVerificationService = registrationVerificationService;
    }

    @Operation(summary = "Verify registration email")
    @PostMapping(value = "/verify-registration", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void verifyRegistration(@RequestParam String token) {
        registrationVerificationService.verifyToken(token);
    }

    @Operation(summary = "Resend verification email")
    @PostMapping(value = "/resend-verification", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void resendVerification(@RequestBody ResendVerificationRequest request) {
        registrationVerificationService.resendVerificationEmail(request.email());
    }

    public record ResendVerificationRequest(String email) {
    }
}
