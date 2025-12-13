package io.hephaistos.flagforge.customerapi.controller;

import io.hephaistos.flagforge.customerapi.controller.dto.EventRequest;
import io.hephaistos.flagforge.customerapi.security.ApiKeySecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/events")
@Tag(name = "events")
@Tag(name = "v1")
public class EventsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventsController.class);

    @Operation(summary = "Ingest a single event from SDK")
    @PostMapping(value = "", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingestEvent(@Valid @RequestBody EventRequest event) {
        var context = ApiKeySecurityContext.getCurrent();
        LOGGER.debug("Received event '{}' for application {}", event.eventType(),
                context.getApplicationId());
        // TODO: Implement event storage
    }

    @Operation(summary = "Ingest multiple events from SDK (batch)")
    @PostMapping(value = "/batch", produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingestEventBatch(@Valid @RequestBody List<EventRequest> events) {
        var context = ApiKeySecurityContext.getCurrent();
        LOGGER.debug("Received {} events for application {}", events.size(),
                context.getApplicationId());
        // TODO: Implement batch event storage
    }
}
