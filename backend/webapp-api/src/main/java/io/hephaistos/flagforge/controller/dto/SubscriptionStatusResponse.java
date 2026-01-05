package io.hephaistos.flagforge.controller.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response containing current subscription status and details.
 */
public record SubscriptionStatusResponse(String status, Integer totalMonthlyPriceCents,
                                         Instant currentPeriodEnd, Boolean cancelAtPeriodEnd,
                                         List<SubscriptionItemDto> items) {
    /**
     * Details of a single subscription item (one per environment).
     */
    public record SubscriptionItemDto(UUID environmentId, String environmentName, String tier,
                                      Integer priceCents) {
    }
}
