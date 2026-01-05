package io.hephaistos.flagforge.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.hephaistos.flagforge.configuration.StripeConfiguration;
import io.hephaistos.flagforge.service.StripeWebhookHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling Stripe webhook events.
 * <p>
 * This endpoint is called by Stripe to notify us of events such as successful payments,
 * subscription changes, and disputes.
 * <p>
 * Security: Webhook signature is verified using the Stripe webhook secret. This endpoint does not
 * require authentication as it's called by Stripe.
 */
@RestController
@RequestMapping("/webhooks")
@Tag(name = "webhooks")
public class StripeWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeWebhookHandler webhookHandler;
    private final StripeConfiguration stripeConfiguration;

    public StripeWebhookController(StripeWebhookHandler webhookHandler,
            StripeConfiguration stripeConfiguration) {
        this.webhookHandler = webhookHandler;
        this.stripeConfiguration = stripeConfiguration;
    }

    @Operation(summary = "Handle Stripe webhook events")
    @PostMapping(value = "/stripe", consumes = "application/json")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            // Verify webhook signature (skip in mock mode for testing)
            if (stripeConfiguration.isMock()) {
                // In mock mode, parse without signature verification
                event = Event.GSON.fromJson(payload, Event.class);
                LOGGER.debug("Mock mode: skipping signature verification");
            }
            else {
                // Both sandbox and production verify signatures
                event = Webhook.constructEvent(payload, sigHeader,
                        stripeConfiguration.getWebhookSecret());
            }
        }
        catch (SignatureVerificationException e) {
            LOGGER.warn("Invalid webhook signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }
        catch (Exception e) {
            LOGGER.error("Error parsing webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        String eventType = event.getType();
        LOGGER.info("Received Stripe webhook: {} (id: {})", eventType, event.getId());

        try {
            switch (eventType) {
                // Checkout events
                case "checkout.session.completed" -> webhookHandler.handleCheckoutCompleted(event);

                // Subscription events
                case "customer.subscription.created" ->
                        webhookHandler.handleSubscriptionCreated(event);
                case "customer.subscription.updated" ->
                        webhookHandler.handleSubscriptionUpdated(event);
                case "customer.subscription.deleted" ->
                        webhookHandler.handleSubscriptionDeleted(event);

                // Invoice events
                case "invoice.paid" -> webhookHandler.handleInvoicePaid(event);
                case "invoice.payment_failed" -> webhookHandler.handlePaymentFailed(event);

                // Dispute events
                case "charge.dispute.created" -> webhookHandler.handleDisputeCreated(event);
                case "charge.dispute.updated" -> webhookHandler.handleDisputeUpdated(event);
                case "charge.dispute.closed" -> webhookHandler.handleDisputeClosed(event);

                default -> LOGGER.debug("Unhandled event type: {}", eventType);
            }
        }
        catch (Exception e) {
            LOGGER.error("Error processing webhook event {}: {}", event.getId(), e.getMessage(), e);
            // Return 200 to prevent Stripe from retrying - we've logged the error
            // and can investigate/fix the issue manually
        }

        return ResponseEntity.ok("OK");
    }
}
