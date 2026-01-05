package io.hephaistos.flagforge.controller.dto;

/**
 * Response containing Stripe Checkout session details.
 */
public record CheckoutSessionResponse(String sessionId, String checkoutUrl) {
}
