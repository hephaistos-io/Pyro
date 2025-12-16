-- Create company invite table for single-use invite links
-- Invites pre-define company, role, and application access for new users

CREATE TABLE company_invite
(
    id            UUID                   DEFAULT uuidv7() PRIMARY KEY,
    token         VARCHAR(64)   NOT NULL UNIQUE,
    email         VARCHAR(255)  NOT NULL,
    company_id    UUID          NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    created_by    UUID          NOT NULL REFERENCES customer (id),
    assigned_role customer_role NOT NULL DEFAULT 'READ_ONLY',
    used_at       TIMESTAMP,
    used_by       UUID REFERENCES customer (id),
    expires_at    TIMESTAMP     NOT NULL,
    created_at    TIMESTAMP              DEFAULT NOW()
);

-- Join table for pre-assigned application access on invites
CREATE TABLE company_invite_application_access
(
    id             UUID DEFAULT uuidv7() PRIMARY KEY,
    invite_id      UUID NOT NULL REFERENCES company_invite (id) ON DELETE CASCADE,
    application_id UUID NOT NULL REFERENCES application (id) ON DELETE CASCADE,
    CONSTRAINT uq_invite_application UNIQUE (invite_id, application_id)
);

-- Index for fast token lookup
CREATE UNIQUE INDEX idx_company_invite_token ON company_invite (token);

-- Index for listing invites by company
CREATE INDEX idx_company_invite_company_id ON company_invite (company_id);

-- Index for application access lookup
CREATE INDEX idx_invite_app_access_invite ON company_invite_application_access (invite_id);
