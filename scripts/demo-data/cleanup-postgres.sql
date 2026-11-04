-- Removes the demo data of seed-postgres.sql, and whatever people did with it during a demo.
-- Only rows belonging to demo accounts, organizations, pets and cases go; a real user who, say,
-- applied to adopt a demo pet loses that application.
--
--   psql "$STAGING_DB_URL" -v ON_ERROR_STOP=1 -f scripts/demo-data/cleanup-postgres.sql

BEGIN;

CREATE TEMP TABLE gone_users ON COMMIT DROP AS SELECT user_id FROM users WHERE user_code LIKE 'DMU%';
CREATE TEMP TABLE gone_orgs  ON COMMIT DROP AS SELECT organization_id FROM organizations WHERE organization_code LIKE 'DMO%';
CREATE TEMP TABLE gone_pets  ON COMMIT DROP AS SELECT pet_id FROM pets WHERE pet_code LIKE 'DMP%';
CREATE TEMP TABLE gone_cases ON COMMIT DROP AS SELECT case_id FROM rescue_cases WHERE case_code LIKE 'DMC%';
CREATE TEMP TABLE gone_apps  ON COMMIT DROP AS
    SELECT application_id FROM adoption_applications
    WHERE adoption_code LIKE 'DMA%'
       OR pet_id IN (SELECT pet_id FROM gone_pets)
       OR applicant_id IN (SELECT user_id FROM gone_users)
       OR organization_id IN (SELECT organization_id FROM gone_orgs);
CREATE TEMP TABLE gone_completions ON COMMIT DROP AS
    SELECT completion_id FROM rescue_case_completions
    WHERE case_id IN (SELECT case_id FROM gone_cases)
       OR submitted_by IN (SELECT user_id FROM gone_users)
       OR verified_by IN (SELECT user_id FROM gone_users);
CREATE TEMP TABLE gone_media ON COMMIT DROP AS
    SELECT media_id FROM media_files WHERE uploader_id IN (SELECT user_id FROM gone_users);

DELETE FROM notifications WHERE user_id IN (SELECT user_id FROM gone_users);
DELETE FROM friend_requests WHERE requester_id IN (SELECT user_id FROM gone_users) OR addressee_id IN (SELECT user_id FROM gone_users);

DELETE FROM rescue_completion_media WHERE completion_id IN (SELECT completion_id FROM gone_completions)
    OR media_id IN (SELECT media_id FROM gone_media);
DELETE FROM rescue_case_completions WHERE completion_id IN (SELECT completion_id FROM gone_completions);
DELETE FROM rescue_case_claims WHERE case_id IN (SELECT case_id FROM gone_cases)
    OR user_id IN (SELECT user_id FROM gone_users)
    OR organization_id IN (SELECT organization_id FROM gone_orgs);
DELETE FROM rescue_media WHERE case_id IN (SELECT case_id FROM gone_cases) OR media_id IN (SELECT media_id FROM gone_media);

DELETE FROM reclaim_logs WHERE application_id IN (SELECT application_id FROM gone_apps)
    OR pet_id IN (SELECT pet_id FROM gone_pets)
    OR user_id IN (SELECT user_id FROM gone_users)
    OR organization_id IN (SELECT organization_id FROM gone_orgs);
DELETE FROM adoption_applications WHERE application_id IN (SELECT application_id FROM gone_apps);

-- A case and the pet that came out of it point at each other.
UPDATE rescue_cases SET pet_id = NULL WHERE pet_id IN (SELECT pet_id FROM gone_pets);
UPDATE pets SET rescue_case_id = NULL WHERE rescue_case_id IN (SELECT case_id FROM gone_cases);
DELETE FROM rescue_cases WHERE case_id IN (SELECT case_id FROM gone_cases)
    OR reported_by IN (SELECT user_id FROM gone_users);

DELETE FROM pet_medical_records WHERE pet_id IN (SELECT pet_id FROM gone_pets);
DELETE FROM pet_media WHERE pet_id IN (SELECT pet_id FROM gone_pets) OR media_file_id IN (SELECT media_id FROM gone_media);
DELETE FROM pet_ownerships WHERE pet_id IN (SELECT pet_id FROM gone_pets);
DELETE FROM pets_current_owner WHERE pet_id IN (SELECT pet_id FROM gone_pets);
DELETE FROM pets WHERE pet_id IN (SELECT pet_id FROM gone_pets);

DELETE FROM media_files WHERE media_id IN (SELECT media_id FROM gone_media);
DELETE FROM organization_members WHERE organization_id IN (SELECT organization_id FROM gone_orgs)
    OR user_id IN (SELECT user_id FROM gone_users);
DELETE FROM organizations WHERE organization_id IN (SELECT organization_id FROM gone_orgs);

DELETE FROM reputation_logs WHERE user_id IN (SELECT user_id FROM gone_users);
DELETE FROM user_reputations WHERE user_id IN (SELECT user_id FROM gone_users);
DELETE FROM refresh_tokens WHERE user_id IN (SELECT user_id FROM gone_users);
DELETE FROM user_roles WHERE user_id IN (SELECT user_id FROM gone_users);
DELETE FROM users WHERE user_id IN (SELECT user_id FROM gone_users);

COMMIT;
