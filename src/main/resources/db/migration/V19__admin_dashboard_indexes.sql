-- ============================================================
-- V19: Composite indexes for admin dashboard query optimization
-- Supports date-range filtered queries (created_at >= start AND created_at < end)
-- which replace EXTRACT(MONTH/YEAR) comparisons.
-- ============================================================

-- organizations: date + soft-delete filter
CREATE INDEX IF NOT EXISTS idx_org_created_at_deleted ON organizations (created_at, is_deleted);
CREATE INDEX IF NOT EXISTS idx_org_status_deleted ON organizations (status, is_deleted);

-- pets: date + soft-delete filter
CREATE INDEX IF NOT EXISTS idx_pets_created_at_deleted ON pets (created_at, is_deleted);

-- rescue_cases: date + soft-delete filter, status filter
CREATE INDEX IF NOT EXISTS idx_rescue_created_at_deleted ON rescue_cases (created_at, is_deleted);
CREATE INDEX IF NOT EXISTS idx_rescue_status_deleted ON rescue_cases (status, is_deleted);

-- adoption_applications: date + soft-delete filter, status filter, org join
CREATE INDEX IF NOT EXISTS idx_adoption_created_at_deleted ON adoption_applications (created_at, is_deleted);
CREATE INDEX IF NOT EXISTS idx_adoption_status_deleted ON adoption_applications (status, is_deleted);
CREATE INDEX IF NOT EXISTS idx_adoption_org_created_at ON adoption_applications (organization_id, created_at, is_deleted);
