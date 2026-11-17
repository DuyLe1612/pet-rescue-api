-- Phase 4: a case is a rescue, a lost pet or a found animal. Existing cases are rescues.
-- LOST and FOUND never go through RESCUED: they close with a resolution.

ALTER TABLE rescue_cases
    ADD COLUMN IF NOT EXISTS case_type              VARCHAR(10) NOT NULL DEFAULT 'RESCUE',
    ADD COLUMN IF NOT EXISTS holding_status         VARCHAR(20),   -- FOUND only: ON_STREET, AT_HOME, AT_ORGANIZATION
    ADD COLUMN IF NOT EXISTS last_seen_at           TIMESTAMPTZ,   -- LOST only
    ADD COLUMN IF NOT EXISTS distinctive_marks      VARCHAR(500),
    ADD COLUMN IF NOT EXISTS resolution             VARCHAR(20),   -- REUNITED when a lost and a found case meet
    ADD COLUMN IF NOT EXISTS matched_case_id        UUID REFERENCES rescue_cases (case_id),
    ADD COLUMN IF NOT EXISTS pet_status_before_lost VARCHAR(20);   -- given back to the pet on reunion

CREATE INDEX IF NOT EXISTS idx_rescue_cases_type_status
    ON rescue_cases (case_type, status)
    WHERE is_deleted = FALSE;

-- A pair the rules think may be the same animal: always a LOST case and a FOUND or RESCUE one.
CREATE TABLE IF NOT EXISTS case_match_suggestions (
    suggestion_id   UUID PRIMARY KEY,
    lost_case_id    UUID        NOT NULL REFERENCES rescue_cases (case_id),
    other_case_id   UUID        NOT NULL REFERENCES rescue_cases (case_id),
    distance_m      INTEGER     NOT NULL,
    score           INTEGER     NOT NULL,
    reasons         TEXT        NOT NULL,   -- one Vietnamese reason per line
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING, CONFIRMED, REJECTED, DISMISSED
    decided_by      UUID REFERENCES users (user_id),
    decided_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (lost_case_id, other_case_id)
);

CREATE INDEX IF NOT EXISTS idx_case_match_other ON case_match_suggestions (other_case_id);
