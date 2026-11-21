-- The full name is the name people see on the platform; the username is what they sign in with
-- and must not be shown. Every account now needs a full name.

-- Accounts made before this rule may have none: give them a neutral name built from the user
-- code (never from the username). They can change it in their profile.
UPDATE users
SET full_name  = 'Thành viên ' || user_code,
    updated_at = NOW()
WHERE full_name IS NULL OR BTRIM(full_name) = '';

ALTER TABLE users
    ALTER COLUMN full_name SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT chk_users_full_name_not_blank CHECK (BTRIM(full_name) <> '');
