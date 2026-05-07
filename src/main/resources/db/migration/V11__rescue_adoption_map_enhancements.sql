-- ============================================================
-- V11: Rescue, Adoption, and Map enhancements
-- - Add rescue contact phone
-- - Add adoption rejection reason and ready_at datetime
-- - Add rescue_media link support (existing table, no schema change here)
-- ============================================================

ALTER TABLE rescue_cases
ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(50);

ALTER TABLE adoption_applications
ADD COLUMN IF NOT EXISTS reject_reason TEXT;

ALTER TABLE adoption_applications
ADD COLUMN IF NOT EXISTS ready_at TIMESTAMPTZ;

COMMENT ON COLUMN rescue_cases.contact_phone IS 'Contact phone for the rescue reporter or organization';
COMMENT ON COLUMN adoption_applications.reject_reason IS 'Reason provided when an adoption application is rejected';
COMMENT ON COLUMN adoption_applications.ready_at IS 'Datetime when the pet is ready to be received after approval';
