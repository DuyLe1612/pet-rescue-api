ALTER TABLE adoption_applications
    ADD COLUMN IF NOT EXISTS housing_condition VARCHAR(50);

ALTER TABLE adoption_applications
    ADD COLUMN IF NOT EXISTS e_signature_id UUID REFERENCES media_files (media_id);

CREATE INDEX IF NOT EXISTS idx_adoption_pending_spam
    ON adoption_applications (applicant_id, status)
    WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS reputation_logs (
    log_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (user_id),
    delta INTEGER NOT NULL,
    reason VARCHAR(100) NOT NULL,
    reference_type VARCHAR(100),
    reference_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_reputation_logs_user ON reputation_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_reputation_logs_reference ON reputation_logs (reference_type, reference_id);

CREATE TABLE IF NOT EXISTS reclaim_logs (
    reclaim_id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES adoption_applications (application_id),
    pet_id UUID NOT NULL REFERENCES pets (pet_id),
    organization_id UUID NOT NULL REFERENCES organizations (organization_id),
    user_id UUID NOT NULL REFERENCES users (user_id),
    reclaimed_by UUID NOT NULL REFERENCES users (user_id),
    reason TEXT NOT NULL,
    proof_id UUID NOT NULL REFERENCES media_files (media_id),
    reputation_delta INTEGER NOT NULL,
    reclaimed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE INDEX IF NOT EXISTS idx_reclaim_application ON reclaim_logs (application_id);
CREATE INDEX IF NOT EXISTS idx_reclaim_org ON reclaim_logs (organization_id);
CREATE INDEX IF NOT EXISTS idx_reclaim_user ON reclaim_logs (user_id);
