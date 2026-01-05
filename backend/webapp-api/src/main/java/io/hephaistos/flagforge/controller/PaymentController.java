package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.configuration.StripeConfiguration;
import io.hephaistos.flagforge.controller.dto.CheckoutRequest;
import io.hephaistos.flagforge.controller.dto.CheckoutSessionResponse;
import io.hephaistos.flagforge.controller.dto.InvoiceResponse;
import io.hephaistos.flagforge.controller.dto.SubscriptionStatusResponse;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import io.hephaistos.flagforge.service.StripeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller for payment and billing operations.
 */
@RestController
@RequestMapping("/v1/payment")
@Tag(name = "payment")
public class PaymentController {

    private final StripeService stripeService;
    private final StripeConfiguration stripeConfiguration;

    public PaymentController(StripeService stripeService, StripeConfiguration stripeConfiguration) {
        this.stripeService = stripeService;
        this.stripeConfiguration = stripeConfiguration;
    }

    @Operation(summary = "Get Stripe publishable key for client-side integration")
    @GetMapping(value = "/config", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public PaymentConfigResponse getPaymentConfig() {
        return new PaymentConfigResponse(stripeConfiguration.getPublishableKey());
    }

    @Operation(summary = "Create a Stripe Checkout session for pending environments")
    @PostMapping(value = "/checkout", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CheckoutSessionResponse createCheckoutSession(
            @Valid @RequestBody CheckoutRequest request) {
        UUID companyId = getCompanyId();

        List<StripeService.PendingEnvironmentItem> items = request.pendingEnvironments()
                .stream()
                .map(dto -> new StripeService.PendingEnvironmentItem(dto.environmentId(),
                        dto.environmentName(), dto.tier()))
                .toList();

        return stripeService.createCheckoutSession(companyId, items, request.successUrl(),
                request.cancelUrl());
    }

    @Operation(summary = "Create a Stripe Customer Portal session for managing billing")
    @PostMapping(value = "/portal", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public PortalSessionResponse createPortalSession(@Valid @RequestBody PortalRequest request) {
        UUID companyId = getCompanyId();
        String url = stripeService.createPortalSession(companyId, request.returnUrl());
        return new PortalSessionResponse(url);
    }

    @Operation(summary = "Get current subscription status")
    @GetMapping(value = "/subscription", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public SubscriptionStatusResponse getSubscriptionStatus() {
        UUID companyId = getCompanyId();
        return stripeService.getSubscriptionStatus(companyId);
    }

    @Operation(summary = "Get invoice history")
    @GetMapping(value = "/invoices", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<InvoiceResponse> getInvoices(@RequestParam(defaultValue = "10") int limit) {
        UUID companyId = getCompanyId();
        return stripeService.getInvoices(companyId, limit);
    }

    @Operation(summary = "Update environment pricing tier")
    @PutMapping(value = "/environments/{environmentId}/tier", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void updateEnvironmentTier(@PathVariable UUID environmentId,
            @Valid @RequestBody UpdateTierRequest request) {
        UUID companyId = getCompanyId();
        stripeService.updateSubscriptionItem(companyId, environmentId, request.tier());
    }

    @Operation(summary = "Create or get Stripe customer for the company")
    @PostMapping(value = "/customer", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CustomerResponse createOrGetCustomer(@Valid @RequestBody CustomerRequest request) {
        UUID companyId = getCompanyId();
        String customerId = stripeService.createOrGetCustomer(companyId, request.companyName(),
                request.email());
        return new CustomerResponse(customerId);
    }

    @Operation(summary = "Complete a checkout session (for mock/development mode)")
    @PostMapping(value = "/checkout/complete", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void completeCheckoutSession(@Valid @RequestBody CompleteCheckoutRequest request) {
        stripeService.completeCheckoutSession(request.sessionId());
    }

    private UUID getCompanyId() {
        return FlagForgeSecurityContext.getCurrent()
                .getCompanyId()
                .orElseThrow(() -> new NoCompanyAssignedException(
                        "Customer must belong to a company to access payment features"));
    }

    public record PaymentConfigResponse(String publishableKey) {
    }


    public record PortalRequest(@NotBlank String returnUrl) {
    }


    public record PortalSessionResponse(String url) {
    }


    public record UpdateTierRequest(@NotNull PricingTier tier) {
    }


    public record CustomerRequest(@NotBlank String companyName, @NotBlank String email) {
    }


    public record CustomerResponse(String customerId) {
    }


    public record CompleteCheckoutRequest(@NotBlank String sessionId) {
    }
}
