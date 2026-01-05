-- Stripe Payment Integration Tables
-- Creates tables for subscription tracking, payment history, and dispute evidence

-- Company subscription: Links a company to their Stripe subscription
CREATE TABLE company_subscription
(
    id                     UUID                  DEFAULT uuidv7() PRIMARY KEY,
    company_id             UUID         NOT NULL UNIQUE REFERENCES company (id) ON DELETE CASCADE,
    stripe_customer_id     VARCHAR(255) NOT NULL UNIQUE,
    stripe_subscription_id VARCHAR(255) UNIQUE,
    status                 VARCHAR(50)  NOT NULL DEFAULT 'INCOMPLETE',
    current_period_start   TIMESTAMP,
    current_period_end     TIMESTAMP,
    cancel_at_period_end   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Indexes for Stripe ID lookups
CREATE INDEX idx_company_subscription_stripe_customer ON company_subscription (stripe_customer_id);
CREATE INDEX idx_company_subscription_stripe_subscription ON company_subscription (stripe_subscription_id);

-- Subscription items: Maps environments to Stripe subscription items
-- Each paid environment has a corresponding subscription item in Stripe
CREATE TABLE subscription_item
(
    id                          UUID                  DEFAULT uuidv7() PRIMARY KEY,
    subscription_id             UUID         NOT NULL REFERENCES company_subscription (id) ON DELETE CASCADE,
    environment_id              UUID         NOT NULL REFERENCES environment (id) ON DELETE CASCADE,
    stripe_subscription_item_id VARCHAR(255) NOT NULL,
    stripe_price_id             VARCHAR(255) NOT NULL,
    quantity                    INTEGER      NOT NULL DEFAULT 1,
    created_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (subscription_id, environment_id)
);

CREATE INDEX idx_subscription_item_environment ON subscription_item (environment_id);

-- Payment history: Audit trail of all payments/invoices
CREATE TABLE payment_history
(
    id                 UUID                  DEFAULT uuidv7() PRIMARY KEY,
    company_id         UUID         NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    stripe_invoice_id  VARCHAR(255) NOT NULL,
    stripe_charge_id   VARCHAR(255),
    amount_cents       INTEGER      NOT NULL,
    currency           VARCHAR(3)   NOT NULL DEFAULT 'usd',
    status             VARCHAR(50)  NOT NULL,
    invoice_pdf_url    TEXT,
    hosted_invoice_url TEXT,
    period_start       TIMESTAMP,
    period_end         TIMESTAMP,
    paid_at            TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_history_company ON payment_history (company_id);
CREATE INDEX idx_payment_history_stripe_invoice ON payment_history (stripe_invoice_id);

-- Dispute evidence: Stores chargeback/dispute information and evidence
CREATE TABLE dispute_evidence
(
    id                        UUID                  DEFAULT uuidv7() PRIMARY KEY,
    company_id                UUID         NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    stripe_dispute_id         VARCHAR(255) NOT NULL UNIQUE,
    stripe_charge_id          VARCHAR(255) NOT NULL,
    amount_cents              INTEGER      NOT NULL,
    reason                    VARCHAR(100),
    status                    VARCHAR(50)  NOT NULL,
    evidence_submitted        BOOLEAN      NOT NULL DEFAULT FALSE,
    customer_email            VARCHAR(255),
    customer_name             VARCHAR(255),
    product_description       TEXT,
    usage_logs                TEXT,
    service_documentation_url TEXT,
    created_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dispute_evidence_company ON dispute_evidence (company_id);
CREATE INDEX idx_dispute_evidence_stripe_dispute ON dispute_evidence (stripe_dispute_id);

-- Add payment status to environments
-- PAID = payment confirmed, PENDING = awaiting checkout, UNPAID = payment failed
ALTER TABLE environment
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PAID';

-- All existing environments are grandfathered as PAID (created before billing)
-- No UPDATE needed since DEFAULT is 'PAID'

COMMENT ON TABLE company_subscription IS 'Links companies to their Stripe subscriptions';
COMMENT ON TABLE subscription_item IS 'Maps environments to Stripe subscription line items';
COMMENT ON TABLE payment_history IS 'Audit trail of all invoices and payments';
COMMENT ON TABLE dispute_evidence IS 'Stores chargeback disputes and evidence for responses';
COMMENT ON COLUMN environment.payment_status IS 'PAID=confirmed, PENDING=awaiting checkout, UNPAID=failed';
