package io.hephaistos.flagforge.customerapi.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.Map;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record EventRequest(@Schema(requiredMode = REQUIRED, example = "flag_evaluated") @NotBlank(
        message = "Event type can't be blank") String eventType,

                           @Schema(description = "Additional event data") Map<String, Object> data,

                           @Schema(description = "Client-side timestamp when event occurred") OffsetDateTime clientTimestamp) {
}
