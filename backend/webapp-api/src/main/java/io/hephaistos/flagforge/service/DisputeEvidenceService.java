package io.hephaistos.flagforge.service;

import java.util.UUID;

/**
 * Service for collecting and submitting dispute evidence to Stripe.
 */
public interface DisputeEvidenceService {

    /**
     * Collects evidence for a dispute.
     *
     * @param companyId the company ID
     * @param chargeId  the Stripe charge ID associated with the dispute
     * @return collected evidence
     */
    DisputeEvidence collectEvidence(UUID companyId, String chargeId);

    /**
     * Submits evidence to Stripe for a dispute.
     *
     * @param disputeId the Stripe dispute ID
     * @param evidence  the collected evidence
     */
    void submitEvidence(String disputeId, DisputeEvidence evidence);


    /**
     * Evidence data collected for a dispute.
     */
    record DisputeEvidence(String customerEmail, String customerName, String productDescription,
                           String usageLogs, String serviceDocumentationUrl) {
    }
}
