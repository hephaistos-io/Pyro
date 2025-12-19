package io.hephaistos.flagforge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CopyOverridesResponse(
        @Schema(description = "Number of overrides successfully copied") int copiedCount,

        @Schema(description = "Number of overrides skipped (already existed and overwrite=false)") int skippedCount) {
}
