package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.controller.dto.CompanyCreationRequest;
import io.hephaistos.flagforge.controller.dto.CompanyResponse;
import io.hephaistos.flagforge.controller.dto.CompanyStatisticsResponse;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.service.CompanyService;
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
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Operation(summary = "Retrieve company for current customer")
    @GetMapping(value = "", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CompanyResponse getCompanyForCurrentCustomer() {
        return companyService.getCompanyForCurrentCustomer()
                .map(CompanyResponse::fromEntity)
                .orElseThrow(
                        () -> new NoCompanyAssignedException("Customer has no company assigned!"));
    }

    @Operation(summary = "Create company for current customer")
    @PostMapping(value = "", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse createCompanyForCurrentCustomer(
            @Valid @RequestBody CompanyCreationRequest companyCreationRequest) {
        return companyService.createCompanyForCurrentCustomer(companyCreationRequest);
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

    @Operation(
            summary = "Retrieve pricing statistics for all applications and environments in the company")
    @GetMapping(value = "/statistics", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CompanyStatisticsResponse getCompanyStatistics() {
        return companyService.getCompanyStatistics();
    }
}
