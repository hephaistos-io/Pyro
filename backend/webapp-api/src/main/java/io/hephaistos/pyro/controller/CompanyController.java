package io.hephaistos.pyro.controller;

import io.hephaistos.pyro.controller.dto.CompanyCreationRequest;
import io.hephaistos.pyro.controller.dto.CompanyResponse;
import io.hephaistos.pyro.exception.NoCompanyAssignedException;
import io.hephaistos.pyro.exception.NotFoundException;
import io.hephaistos.pyro.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/company")
@Tag(name = "company")
@Tag(name = "v1")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Operation(summary = "Retrieve company for current user")
    @GetMapping(value = "", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CompanyResponse getCompanyForCurrentUser() {
        return companyService.getCompanyForCurrentUser()
                .map(CompanyResponse::fromEntity)
                .orElseThrow(() -> new NoCompanyAssignedException("User has no company assigned!"));
    }

    @Operation(summary = "Create company for current user")
    @PostMapping(value = "", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse createCompanyForCurrentUser(
            @Valid @RequestBody CompanyCreationRequest companyCreationRequest) {
        return companyService.createCompanyForCurrentUser(companyCreationRequest);
    }

    @Operation(summary = "Retrieve company with id")
    @GetMapping(value = "/{companyId}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CompanyResponse getCompanyById(@PathVariable UUID companyId) {
        return companyService.getCompany(companyId)
                .map(CompanyResponse::fromEntity)
                .orElseThrow(() -> new NotFoundException(
                        "Couldn't find company for id %s!".formatted(companyId)));
    }
}
