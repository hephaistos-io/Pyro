package io.hephaistos.flagforge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

public record ProfileUpdateRequest(@Schema(requiredMode = NOT_REQUIRED) @Size(min = 2, max = 50,
        message = "First name must be 2-50 characters") String firstName,

                                   @Schema(requiredMode = NOT_REQUIRED) @Size(min = 2, max = 50,
                                           message = "Last name must be 2-50 characters") String lastName) {
}
