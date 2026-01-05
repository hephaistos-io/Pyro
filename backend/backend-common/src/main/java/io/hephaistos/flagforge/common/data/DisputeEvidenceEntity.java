package io.hephaistos.flagforge.common.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a Stripe dispute (chargeback) and its evidence.
 * <p>
 * When a customer disputes a charge, this entity stores the dispute details and any evidence
 * collected for the dispute response.
 */
@Entity
@Table(name = "dispute_evidence")
public class DisputeEvidenceEntity extends CompanyOwnedEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "stripe_dispute_id", nullable = false, unique = true)
    private String stripeDisputeId;

    @Column(name = "stripe_charge_id", nullable = false)
    private String stripeChargeId;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(length = 100)
    private String reason;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "evidence_submitted", nullable = false)
    private Boolean evidenceSubmitted = false;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Column(name = "usage_logs", columnDefinition = "TEXT")
    private String usageLogs;

    @Column(name = "service_documentation_url")
    private String serviceDocumentationUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStripeDisputeId() {
        return stripeDisputeId;
    }

    public void setStripeDisputeId(String stripeDisputeId) {
        this.stripeDisputeId = stripeDisputeId;
    }

    public String getStripeChargeId() {
        return stripeChargeId;
    }

    public void setStripeChargeId(String stripeChargeId) {
        this.stripeChargeId = stripeChargeId;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Integer amountCents) {
        this.amountCents = amountCents;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getEvidenceSubmitted() {
        return evidenceSubmitted;
    }

    public void setEvidenceSubmitted(Boolean evidenceSubmitted) {
        this.evidenceSubmitted = evidenceSubmitted;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getUsageLogs() {
        return usageLogs;
    }

    public void setUsageLogs(String usageLogs) {
        this.usageLogs = usageLogs;
    }

    public String getServiceDocumentationUrl() {
        return serviceDocumentationUrl;
    }

    public void setServiceDocumentationUrl(String serviceDocumentationUrl) {
        this.serviceDocumentationUrl = serviceDocumentationUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Updates the updatedAt timestamp. Call this before saving.
     */
    public void touch() {
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the amount in dollars (convenience method).
     */
    public double getAmountDollars() {
        return amountCents / 100.0;
    }
}
