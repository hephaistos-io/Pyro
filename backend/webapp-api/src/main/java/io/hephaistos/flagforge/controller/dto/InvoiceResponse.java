package io.hephaistos.flagforge.controller.dto;

import java.time.Instant;

/**
 * Response containing invoice details.
 */
public record InvoiceResponse(String invoiceId, Integer amountCents, String currency, String status,
                              String pdfUrl, String hostedUrl, Instant periodStart,
                              Instant periodEnd, Instant paidAt, Instant createdAt) {
}
