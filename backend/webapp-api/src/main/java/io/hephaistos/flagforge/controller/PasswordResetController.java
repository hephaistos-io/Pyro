package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.controller.dto.PasswordResetExecuteRequest;
import io.hephaistos.flagforge.controller.dto.PasswordResetRequestDto;
import io.hephaistos.flagforge.exception.InvalidTokenException;
import io.hephaistos.flagforge.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/password-reset")
@Tag(name = "password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "Request password reset (forgot password - public)")
    @PostMapping(value = "/request", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request,
            HttpServletRequest httpRequest) {
        String baseUrl = getBaseUrl(httpRequest);
        // Always return success to prevent account enumeration
        // The service returns empty if email doesn't exist, but we don't expose that
        passwordResetService.requestPasswordReset(request.email(), baseUrl);
    }

    @Operation(summary = "Validate password reset token")
    @GetMapping(value = "/validate", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Boolean> validateToken(@RequestParam String token) {
        try {
            passwordResetService.validateToken(token);
            return Map.of("valid", true);
        }
        catch (InvalidTokenException e) {
            return Map.of("valid", false);
        }
    }

    @Operation(summary = "Execute password reset")
    @PostMapping(value = "/reset", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void resetPassword(@Valid @RequestBody PasswordResetExecuteRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
    }

    private String getBaseUrl(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        return origin != null ? origin : request.getHeader("Referer");
    }
}
