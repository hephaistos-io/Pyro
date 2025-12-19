package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.TemplateType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record CopyOverridesRequest(@Schema(requiredMode = REQUIRED,
        description = "Source environment ID to copy from") @NotNull(
        message = "Source environment ID cannot be null") UUID sourceEnvironmentId,

                                   @Schema(requiredMode = REQUIRED,
                                           description = "Target environment ID to copy to") @NotNull(
                                           message = "Target environment ID cannot be null") UUID targetEnvironmentId,

                                   @Schema(description = "Template types to copy (USER, SYSTEM, or both). If not provided, copies all types.") List<TemplateType> types,

                                   @Schema(description = "Whether to overwrite existing overrides in the target environment. Default: false") Boolean overwrite,

                                   @Schema(description = "Optional list of specific identifiers to copy. If not provided, copies all identifiers.") List<String> identifiers) {
}
