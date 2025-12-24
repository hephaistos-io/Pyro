-- Per-user template overrides for USER templates
-- user_id is a UUID derived from customer's identifier string via deterministic hashing
CREATE TABLE user_template_values
(
    id             UUID           DEFAULT uuidv7() PRIMARY KEY,
    application_id UUID  NOT NULL REFERENCES application (id) ON DELETE CASCADE,
    environment_id UUID  NOT NULL REFERENCES environment (id) ON DELETE CASCADE,
    user_id        UUID  NOT NULL,
    values         JSONB NOT NULL DEFAULT '{}',
    created_at     TIMESTAMP      DEFAULT NOW(),
    updated_at     TIMESTAMP      DEFAULT NOW(),
    CONSTRAINT uq_user_template_values UNIQUE (application_id, environment_id, user_id)
);

CREATE INDEX idx_user_template_values_lookup
    ON user_template_values (application_id, environment_id, user_id);
