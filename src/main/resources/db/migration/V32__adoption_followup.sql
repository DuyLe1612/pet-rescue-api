-- Follow-up after an adoption: scheduled questions, the work they create for the organization,
-- and how an adoption ended. See agent/11-design-followup.md.
ALTER TABLE adoption_applications
    ADD COLUMN completed_at TIMESTAMPTZ,
    ADD COLUMN followup_opt_out BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE adoption_applications SET completed_at = COALESCE(updated_at, created_at) WHERE status = 'COMPLETED';

CREATE TABLE adoption_followups (
    followup_id     UUID PRIMARY KEY,
    application_id  UUID        NOT NULL REFERENCES adoption_applications (application_id) ON DELETE CASCADE,
    pet_id          UUID        NOT NULL REFERENCES pets (pet_id),
    adopter_id      UUID        NOT NULL REFERENCES users (user_id),
    organization_id UUID        NOT NULL REFERENCES organizations (organization_id),
    milestone       VARCHAR(10) NOT NULL,
    status          VARCHAR(12) NOT NULL,
    due_at          TIMESTAMPTZ NOT NULL,
    sent_at         TIMESTAMPTZ,
    reminded_at     TIMESTAMPTZ,
    responded_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    token_hash      VARCHAR(64),
    form_version    INT         NOT NULL DEFAULT 1,
    answers         JSONB,
    still_has_pet   BOOLEAN,
    severity        VARCHAR(8),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_followup_milestone UNIQUE (application_id, milestone)
);

CREATE INDEX idx_followup_due ON adoption_followups (status, due_at);
CREATE UNIQUE INDEX idx_followup_token ON adoption_followups (token_hash) WHERE token_hash IS NOT NULL;
CREATE INDEX idx_followup_adopter ON adoption_followups (adopter_id);

CREATE TABLE followup_alerts (
    alert_id        UUID PRIMARY KEY,
    followup_id     UUID REFERENCES adoption_followups (followup_id) ON DELETE SET NULL,
    application_id  UUID        NOT NULL REFERENCES adoption_applications (application_id) ON DELETE CASCADE,
    organization_id UUID        NOT NULL REFERENCES organizations (organization_id),
    severity        VARCHAR(8)  NOT NULL,
    reason_codes    TEXT        NOT NULL,
    status          VARCHAR(12) NOT NULL DEFAULT 'OPEN',
    assigned_to     UUID REFERENCES users (user_id),
    first_action_at TIMESTAMPTZ,
    resolved_at     TIMESTAMPTZ,
    resolution      VARCHAR(30),
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_followup_alert_org ON followup_alerts (organization_id, status, severity);

CREATE TABLE adoption_outcomes (
    outcome_id            UUID PRIMARY KEY,
    application_id        UUID        NOT NULL UNIQUE REFERENCES adoption_applications (application_id) ON DELETE CASCADE,
    pet_id                UUID        NOT NULL REFERENCES pets (pet_id),
    outcome_type          VARCHAR(24) NOT NULL,
    reason_code           VARCHAR(20),
    initiated_by          UUID REFERENCES users (user_id),
    occurred_at           TIMESTAMPTZ NOT NULL,
    days_since_completed  INT,
    note                  TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
