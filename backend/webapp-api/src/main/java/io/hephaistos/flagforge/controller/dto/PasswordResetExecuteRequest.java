package io.hephaistos.flagforge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record PasswordResetExecuteRequest(
        @Schema(requiredMode = REQUIRED) @NotBlank(message = "Token is required") String token,

        @Schema(requiredMode = REQUIRED, minLength = 8) @NotBlank(
                message = "Password is required") @Size(min = 8,
                message = "Password must be at least 8 characters") String newPassword) {
}
