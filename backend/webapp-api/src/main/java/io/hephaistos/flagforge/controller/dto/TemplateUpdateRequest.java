package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.types.TemplateSchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record TemplateUpdateRequest(@Schema(requiredMode = REQUIRED) @NotNull(
        message = "Template schema is required") @Valid TemplateSchema schema) {
}
