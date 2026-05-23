ALTER TABLE pet_media
    ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_pet_media_primary
    ON pet_media (pet_id)
    WHERE is_primary = TRUE;
