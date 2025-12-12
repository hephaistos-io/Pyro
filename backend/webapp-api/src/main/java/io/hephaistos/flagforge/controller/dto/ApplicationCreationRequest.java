package io.hephaistos.flagforge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record ApplicationCreationRequest(@Schema(requiredMode = REQUIRED, minLength = 2) @NotBlank(
        message = "Application name can't be blank") @Size(min = 2,
        message = "Application name must be at least 2 characters") String name) {
}
