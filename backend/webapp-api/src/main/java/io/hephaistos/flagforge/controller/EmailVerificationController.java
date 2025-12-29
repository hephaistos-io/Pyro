package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.exception.InvalidTokenException;
import io.hephaistos.flagforge.service.EmailChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/email-verification")
@Tag(name = "email-verification")
public class EmailVerificationController {

    private final EmailChangeService emailChangeService;

    public EmailVerificationController(EmailChangeService emailChangeService) {
        this.emailChangeService = emailChangeService;
    }

    @Operation(summary = "Validate email change token")
    @GetMapping(value = "/validate", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> validateToken(@RequestParam String token) {
        try {
            var tokenEntity = emailChangeService.validateToken(token);
            return Map.of("valid", true, "newEmail", tokenEntity.getNewEmail());
        }
        catch (InvalidTokenException e) {
            return Map.of("valid", false);
        }
    }

    @Operation(summary = "Confirm email change")
    @PostMapping(value = "/confirm")
    @ResponseStatus(HttpStatus.OK)
    public void confirmEmailChange(@RequestParam String token) {
        emailChangeService.confirmEmailChange(token);
    }
}
