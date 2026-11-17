-- Progress notes on a rescue case (C3) and community flags saying a case is false or already
-- handled (C8). A flag never changes the case: enough of them only alert the reporter and admins.

CREATE TABLE IF NOT EXISTS rescue_case_updates (
    update_id  UUID PRIMARY KEY,
    case_id    UUID        NOT NULL REFERENCES rescue_cases (case_id),
    author_id  UUID        NOT NULL REFERENCES users (user_id),
    note       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rescue_case_updates_case ON rescue_case_updates (case_id, created_at DESC);

CREATE TABLE IF NOT EXISTS rescue_case_update_media (
    update_id UUID     NOT NULL REFERENCES rescue_case_updates (update_id),
    media_id  UUID     NOT NULL REFERENCES media_files (media_id),
    position  SMALLINT NOT NULL,
    PRIMARY KEY (update_id, media_id)
);

CREATE TABLE IF NOT EXISTS rescue_case_flags (
    case_id    UUID        NOT NULL REFERENCES rescue_cases (case_id),
    user_id    UUID        NOT NULL REFERENCES users (user_id),
    reason     VARCHAR(30) NOT NULL,   -- FALSE_REPORT, ALREADY_HANDLED, DUPLICATE
    note       VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (case_id, user_id)
);

-- set once, when the flags first reach the threshold, so the alert goes out a single time
ALTER TABLE rescue_cases ADD COLUMN IF NOT EXISTS flag_alerted_at TIMESTAMPTZ;
