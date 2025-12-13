package io.hephaistos.flagforge.customerapi.controller.dto;

public record FeatureFlagResponse(String key, Object value, String type) {
}
