-- AI triage of a reported rescue case (phase 3). The model only labels: it never changes the
-- reporter's data, never lowers a priority, never closes or merges a case. A case without a row
-- here, or with a failed one, is handled exactly as before.

CREATE TABLE IF NOT EXISTS rescue_case_triage (
    case_id           UUID PRIMARY KEY REFERENCES rescue_cases (case_id),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',   -- PENDING, ANALYZED, FAILED
    attempts          INTEGER      NOT NULL DEFAULT 0,
    claimed_at        TIMESTAMPTZ,                               -- a claim older than a few minutes belongs to a dead worker
    model             VARCHAR(120),
    prompt_version    INTEGER,
    ai_urgency        VARCHAR(20),                               -- LOW, NORMAL, HIGH, CRITICAL; null when the model sees no emergency
    needs_rescue      BOOLEAN,
    condition_summary TEXT,
    species           VARCHAR(100),                              -- from the species catalog, never free text
    district          VARCHAR(100),
    province          VARCHAR(100),
    missing_fields    VARCHAR(255),                              -- comma separated, found by rules rather than by the model
    failure           TEXT,
    raw_output        TEXT,
    latency_ms        BIGINT,
    input_tokens      BIGINT,
    output_tokens     BIGINT,
    analyzed_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- the retry pass looks for work by status and age
CREATE INDEX IF NOT EXISTS idx_rescue_triage_retry
    ON rescue_case_triage (updated_at)
    WHERE status IN ('PENDING', 'FAILED');
