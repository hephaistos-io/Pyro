package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.enums.PricingTier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request to create a Stripe Checkout session.
 */
public record CheckoutRequest(@NotEmpty @Valid List<PendingEnvironmentDto> pendingEnvironments,
                              @NotBlank String successUrl, @NotBlank String cancelUrl) {
    /**
     * An environment pending payment.
     */
    public record PendingEnvironmentDto(@NotNull UUID environmentId, String environmentName,
                                        @NotNull PricingTier tier) {
    }
}
