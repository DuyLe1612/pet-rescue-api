CREATE TABLE IF NOT EXISTS rescue_case_completions (
    completion_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    case_id UUID NOT NULL,

    rescued_at TIMESTAMPTZ NOT NULL,

    rescue_note TEXT,

    location_note TEXT,

    verified_by UUID NOT NULL,

    verified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_at      TIMESTAMPTZ,
    updated_by      UUID,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID,

    CONSTRAINT fk_rescue_completion_case
        FOREIGN KEY (case_id)
            REFERENCES rescue_cases(case_id),

    CONSTRAINT fk_rescue_completion_verified_by
        FOREIGN KEY (verified_by)
            REFERENCES users(user_id)
);
CREATE TABLE IF NOT EXISTS rescue_completion_media (
    completion_id UUID NOT NULL,
    media_id UUID NOT NULL,

    PRIMARY KEY (completion_id, media_id),

    CONSTRAINT fk_completion_media_completion
        FOREIGN KEY (completion_id)
            REFERENCES rescue_case_completions(completion_id),

    CONSTRAINT fk_completion_media_media
        FOREIGN KEY (media_id)
            REFERENCES media_files(media_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_rescue_completion_case
    ON rescue_case_completions(case_id);

CREATE INDEX IF NOT EXISTS idx_completion_rescued_at
    ON rescue_case_completions(rescued_at DESC);

CREATE INDEX IF NOT EXISTS idx_rescue_cases_location
    ON rescue_cases
    USING GIST(location);

CREATE INDEX IF NOT EXISTS idx_rescue_cases_status_reported
    ON rescue_cases(
                    status,
                    reported_at DESC
        )
    WHERE is_deleted = false;