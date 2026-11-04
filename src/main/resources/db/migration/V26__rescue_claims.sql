-- Phase 1: anyone can claim a rescue case, completions can be confirmed, disputed or
-- rejected, and users get in-app notifications.

CREATE TABLE IF NOT EXISTS rescue_case_claims (
    claim_id        UUID PRIMARY KEY,
    case_id         UUID        NOT NULL REFERENCES rescue_cases (case_id),
    user_id         UUID        NOT NULL REFERENCES users (user_id),
    organization_id UUID REFERENCES organizations (organization_id),
    status          VARCHAR(20) NOT NULL,
    release_reason  VARCHAR(30),
    claimed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    reminded_at     TIMESTAMPTZ,
    ended_at        TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_rescue_claim_active
    ON rescue_case_claims (case_id, user_id)
    WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_rescue_claim_case ON rescue_case_claims (case_id, status);
CREATE INDEX IF NOT EXISTS idx_rescue_claim_user ON rescue_case_claims (user_id, status);
CREATE INDEX IF NOT EXISTS idx_rescue_claim_expiry
    ON rescue_case_claims (expires_at)
    WHERE status = 'ACTIVE';

ALTER TABLE rescue_case_completions
    ADD COLUMN IF NOT EXISTS submitted_by UUID REFERENCES users (user_id),
    ADD COLUMN IF NOT EXISTS outcome      VARCHAR(30),
    ADD COLUMN IF NOT EXISTS status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS reject_reason TEXT,
    ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMPTZ;

UPDATE rescue_case_completions SET status = 'APPROVED' WHERE verified_at IS NOT NULL;

-- A rejected report must not block the next one.
DROP INDEX IF EXISTS uk_rescue_completion_case;
CREATE UNIQUE INDEX IF NOT EXISTS uk_rescue_completion_case_open
    ON rescue_case_completions (case_id)
    WHERE status <> 'REJECTED' AND is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_rescue_completion_status
    ON rescue_case_completions (status, created_at);

ALTER TABLE rescue_cases
    ADD COLUMN IF NOT EXISTS version         BIGINT  NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS resolution_note TEXT,
    ADD COLUMN IF NOT EXISTS needs_help      BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS notifications (
    notification_id UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (user_id),
    type            VARCHAR(50)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    body            TEXT,
    data            JSONB,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_unread
    ON notifications (user_id)
    WHERE read_at IS NULL;
