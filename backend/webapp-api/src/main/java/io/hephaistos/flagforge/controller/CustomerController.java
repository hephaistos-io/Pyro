package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.controller.dto.CustomerResponse;
import io.hephaistos.flagforge.controller.dto.TeamResponse;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import io.hephaistos.flagforge.service.CustomerService;
import io.hephaistos.flagforge.service.InviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/customer")
@Tag(name = "customer")
@Tag(name = "v1")
public class CustomerController {

    private final CustomerService customerService;
    private final InviteService inviteService;

    public CustomerController(CustomerService customerService, InviteService inviteService) {
        this.customerService = customerService;
        this.inviteService = inviteService;
    }

    @Operation(summary = "Retrieve profile of customer")
    @GetMapping(value = "/profile", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CustomerResponse profile() {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        return customerService.getCustomerByEmail(securityContext.getCustomerName())
                .map(CustomerResponse::fromEntity)
                .orElseThrow(
                        () -> new UsernameNotFoundException("Customer not found for your session"));
    }

    @Operation(summary = "Retrieve all team members and pending invites for the assigned company")
    @GetMapping(value = "/all", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TeamResponse getTeam() {
        FlagForgeSecurityContext.getCurrent()
                .getCompanyId()
                .orElseThrow(() -> new NoCompanyAssignedException(
                        "Customer has no company assigned. Cannot perform application operations."));

        var members = customerService.getAllCustomers()
                .stream()
                .map(CustomerResponse::fromEntity)
                .toList();

        var pendingInvites = inviteService.getPendingInvitesForCompany();

        return new TeamResponse(members, pendingInvites);
    }
}
