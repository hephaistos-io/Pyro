package io.hephaistos.flagforge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record PasswordResetRequestDto(
        @Schema(requiredMode = REQUIRED) @Email(message = "Invalid email format") @NotBlank(
                message = "Email is required") String email) {
}
