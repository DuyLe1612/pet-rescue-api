-- Indexes matching what the read queries really do.

-- rescue_cases.location had three identical GiST indexes and organizations.location two.
DROP INDEX IF EXISTS idx_rescue_location_gist;
DROP INDEX IF EXISTS idx_rescue_cases_location;
DROP INDEX IF EXISTS idx_org_location_gist;

-- "Nearby" compares geography values, which a geometry index cannot serve.
CREATE INDEX IF NOT EXISTS idx_rescue_location_geography
    ON rescue_cases USING GIST ((location::geography));

CREATE INDEX IF NOT EXISTS idx_rescue_cases_organization
    ON rescue_cases (organization_id)
    WHERE is_deleted = FALSE;

-- The organization rescue queue: unassigned, unfinished cases of one province.
CREATE INDEX IF NOT EXISTS idx_rescue_queue
    ON rescue_cases (province_code, reported_at)
    WHERE is_deleted = FALSE AND organization_id IS NULL AND status IN ('REPORTED', 'IN_PROGRESS', 'RESCUED');

-- Substring search on columns searched without a join. A search spanning a join (rescue cases by
-- reporter name) still scans, because the OR across tables cannot use these.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_pets_name_trgm ON pets USING GIN (LOWER(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_username_trgm ON users USING GIN (LOWER(username) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_full_name_trgm ON users USING GIN (LOWER(full_name) gin_trgm_ops);

-- The queue checks each rescued case for an approved report; the partial unique index on
-- case_id only covers open, undeleted reports and cannot answer that.
CREATE INDEX IF NOT EXISTS idx_rescue_completion_case ON rescue_case_completions (case_id);
