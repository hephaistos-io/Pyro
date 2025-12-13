package io.hephaistos.flagforge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record EnvironmentCreationRequest(@Schema(requiredMode = REQUIRED, minLength = 2) @NotBlank(
        message = "Environment name can't be blank") @Size(min = 2,
        message = "Environment name must be at least 2 characters") String name,
                                         @Schema(requiredMode = REQUIRED) @NotBlank(
                                                 message = "Environment description can't be blank") String description) {
}
