package io.hephaistos.flagforge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record TemplateValuesRequest(@Schema(requiredMode = REQUIRED,
        description = "Key-value pairs for template fields") @NotNull(
        message = "Values cannot be null") Map<String, Object> values) {
}
