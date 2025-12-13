package io.hephaistos.flagforge.customerapi.controller;

import io.hephaistos.flagforge.customerapi.controller.dto.FeatureFlagResponse;
import io.hephaistos.flagforge.customerapi.security.ApiKeySecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/flags")
@Tag(name = "flags")
@Tag(name = "v1")
public class FeatureFlagController {

    @Operation(summary = "Get all feature flags for the application")
    @GetMapping(value = "", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<FeatureFlagResponse> getAllFlags() {
        var context = ApiKeySecurityContext.getCurrent();
        // TODO: Implement feature flag retrieval
        return Collections.emptyList();
    }

    @Operation(summary = "Get a specific feature flag by key")
    @GetMapping(value = "/{flagKey}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public FeatureFlagResponse getFlag(@PathVariable String flagKey) {
        var context = ApiKeySecurityContext.getCurrent();
        // TODO: Implement single feature flag retrieval
        return new FeatureFlagResponse(flagKey, false, "boolean");
    }
}
