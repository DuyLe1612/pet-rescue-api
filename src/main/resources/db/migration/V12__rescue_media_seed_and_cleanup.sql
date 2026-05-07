-- ============================================================
-- V12: Rescue media support cleanup
-- - Ensure rescue_media table exists
-- - Ensure indexes exist for faster lookup
-- ============================================================

CREATE TABLE IF NOT EXISTS rescue_media (
    case_id  UUID NOT NULL REFERENCES rescue_cases (case_id) ON DELETE CASCADE,
    media_id UUID NOT NULL REFERENCES media_files (media_id) ON DELETE CASCADE,
    PRIMARY KEY (case_id, media_id)
);

CREATE INDEX IF NOT EXISTS idx_rescue_media_case ON rescue_media (case_id);
CREATE INDEX IF NOT EXISTS idx_rescue_media_media ON rescue_media (media_id);
