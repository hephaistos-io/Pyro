package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.controller.dto.InviteCreationRequest;
import io.hephaistos.flagforge.controller.dto.InviteCreationResponse;
import io.hephaistos.flagforge.controller.dto.InviteValidationResponse;
import io.hephaistos.flagforge.service.InviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Tag(name = "invite")
@Tag(name = "v1")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @Operation(summary = "Create an invite link for a new team member")
    @PostMapping(value = "/v1/company/invite", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public InviteCreationResponse createInvite(@Valid @RequestBody InviteCreationRequest request) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .replacePath(null)
                .toUriString();
        return inviteService.createInvite(request, baseUrl);
    }

    @Operation(summary = "Validate an invite token (public endpoint)")
    @GetMapping(value = "/v1/invite/{token}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public InviteValidationResponse validateInvite(@PathVariable String token) {
        return inviteService.validateInvite(token);
    }

    @Operation(summary = "Regenerate an invite link with a new token")
    @PostMapping(value = "/v1/company/invite/{id}/regenerate", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public InviteCreationResponse regenerateInvite(@PathVariable UUID id) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .replacePath(null)
                .toUriString();
        return inviteService.regenerateInvite(id, baseUrl);
    }

    @Operation(summary = "Delete an invite")
    @DeleteMapping(value = "/v1/company/invite/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvite(@PathVariable UUID id) {
        inviteService.deleteInvite(id);
    }
}
