-- ============================================================================
-- Pet Rescue demo data (PostgreSQL) — for staging and load tests, NEVER production.
--
-- Not a Flyway migration: it runs only when someone runs it. Needs every migration applied.
--
--   psql "$STAGING_DB_URL" -v ON_ERROR_STOP=1 -f scripts/demo-data/seed-postgres.sql
--
-- Safe to run again: ids are derived from a fixed text, so existing rows are left alone.
-- Remove everything with cleanup-postgres.sql. All accounts use the password in README.md.
--
-- Change demo.scale below to multiply the volume (1 = about 300 rows of main data, a good demo;
-- 100 or more is for measuring queries).
-- ============================================================================

BEGIN;

SELECT set_config('demo.scale', '1', true);

CREATE OR REPLACE FUNCTION pg_temp.did(kind TEXT, n INT) RETURNS UUID
    LANGUAGE sql IMMUTABLE AS $$ SELECT md5('pet-rescue-demo:' || kind || ':' || n)::uuid $$;
CREATE OR REPLACE FUNCTION pg_temp.scale() RETURNS INT
    LANGUAGE sql STABLE AS $$ SELECT current_setting('demo.scale')::int $$;
-- Spreads rows evenly over the last six months, so every dashboard month has data.
CREATE OR REPLACE FUNCTION pg_temp.ago(n INT, total INT) RETURNS TIMESTAMPTZ
    LANGUAGE sql STABLE AS $$ SELECT now() - make_interval(hours => (n * 4320 / GREATEST(total, 1))) $$;

CREATE TEMP TABLE demo_place (idx INT, province_code TEXT, province_name TEXT, ward_code TEXT, ward_name TEXT, lat FLOAT8, lng FLOAT8) ON COMMIT DROP;
INSERT INTO demo_place VALUES
    (0, '79', 'Thành phố Hồ Chí Minh', '25747', 'Phường Thủ Dầu Một', 10.98, 106.65),
    (1, '1',  'Thành phố Hà Nội',      '4',     'Phường Ba Đình',     21.03, 105.82),
    (2, '48', 'Thành phố Đà Nẵng',     '20194', 'Phường Hải Vân',     16.10, 108.15);

CREATE TEMP TABLE demo_image (idx INT, public_id TEXT) ON COMMIT DROP;
-- Sample images that come with every new Cloudinary account.
INSERT INTO demo_image VALUES
    (0, 'samples/animals/three-dogs'), (1, 'samples/animals/cat'),
    (2, 'samples/animals/kitten-playing'), (3, 'samples/animals/reindeer');

-- ── Accounts ─────────────────────────────────────────────────────────────────

CREATE TEMP TABLE demo_account ON COMMIT DROP AS
SELECT 'admin' AS kind, 1 AS n, 'demo.admin' AS username, 'Quản trị Demo' AS full_name, 'ADMIN' AS role
UNION ALL SELECT 'owner', n, 'demo.owner' || n, 'Chủ trung tâm ' || n, 'MEMBER' FROM generate_series(1, 8 * pg_temp.scale()) n
UNION ALL SELECT 'staff', n, 'demo.staff' || n, 'Nhân viên ' || n, 'MEMBER' FROM generate_series(1, 8 * pg_temp.scale()) n
UNION ALL SELECT 'user',  n, 'demo.user' || n,
                 (ARRAY['Nguyễn An','Trần Bình','Lê Chi','Phạm Dũng','Hoàng Giang','Vũ Hà','Đặng Khoa','Bùi Linh'])[1 + n % 8] || ' ' || n,
                 'USER' FROM generate_series(1, 30 * pg_temp.scale()) n;

INSERT INTO users (user_id, user_code, username, email, password_hash, full_name, phone, status, email_verified,
                   province_code, province_name, ward_code, ward_name, created_at)
SELECT pg_temp.did(a.kind, a.n),
       'DMU' || upper(left(a.kind, 1)) || lpad(a.n::text, 7, '0'),
       a.username, a.username || '@petrescue.demo',
       '$2a$10$y.fXK2Rk.lGsNVd8axBbsOj/K9JQ7toKWtEeReT6vTMtXqR5Bq002',
       a.full_name, '09' || lpad((a.n * 7919 % 100000000)::text, 8, '0'), 'ACTIVE', TRUE,
       p.province_code, p.province_name, p.ward_code, p.ward_name,
       now() - interval '200 days'
FROM demo_account a JOIN demo_place p ON p.idx = a.n % 3
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT pg_temp.did(a.kind, a.n), r.role_id
FROM demo_account a JOIN roles r ON r.code = a.role
ON CONFLICT DO NOTHING;

INSERT INTO user_reputations (user_id, score, level, updated_at)
SELECT pg_temp.did('user', n), (n * 37) % 400, 'NEW', now()
FROM generate_series(1, 30 * pg_temp.scale()) n
ON CONFLICT DO NOTHING;

-- public_id is unique, so all rows share these files. A staging database may already have one.
INSERT INTO media_files (media_id, uploader_id, public_id, resource_type, format, status, folder, created_at)
SELECT pg_temp.did('image', i.idx), pg_temp.did('admin', 1), i.public_id, 'image', 'jpg', 'PERMANENT', 'demo', now() - interval '200 days'
FROM demo_image i
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE demo_media ON COMMIT DROP AS
SELECT i.idx, m.media_id FROM demo_image i JOIN media_files m ON m.public_id = i.public_id;

-- ── Organizations: every fourth is still waiting for approval, every fifth is a vet ──

INSERT INTO organizations (organization_id, organization_code, name, type, description, street_address,
                           province_code, province_name, ward_code, ward_name, location, phone, email,
                           official_link, status, requested_by_user_id, created_at)
SELECT pg_temp.did('org', n),
       'DMO' || lpad(n::text, 8, '0'),
       (CASE WHEN n % 5 = 0 THEN 'Phòng khám thú y ' ELSE 'Trạm cứu hộ ' END) || 'Demo ' || n,
       CASE WHEN n % 5 = 0 THEN 'VET_CENTER' ELSE 'SHELTER' END,
       'Tổ chức dữ liệu mẫu số ' || n,
       (n * 3 % 300 + 1) || ' Đường Demo',
       p.province_code, p.province_name, p.ward_code, p.ward_name,
       ST_SetSRID(ST_MakePoint(p.lng + (n % 40) * 0.003, p.lat + (n % 37) * 0.003), 4326),
       '028' || lpad((n * 104729 % 10000000)::text, 7, '0'),
       'org' || n || '@petrescue.demo', 'https://example.org/demo/' || n,
       CASE WHEN n % 4 = 0 THEN 'PENDING' ELSE 'ACTIVE' END,
       pg_temp.did('owner', n),
       now() - make_interval(days => (n * 17 % 190))
FROM generate_series(1, 8 * pg_temp.scale()) n
JOIN demo_place p ON p.idx = n % 3
ON CONFLICT DO NOTHING;

INSERT INTO organization_members (organization_id, user_id, role, joined_at, status)
SELECT pg_temp.did('org', n), pg_temp.did('owner', n), 'OWNER', now() - interval '190 days', 'ACTIVE'
FROM generate_series(1, 8 * pg_temp.scale()) n
UNION ALL
SELECT pg_temp.did('org', n), pg_temp.did('staff', n), 'STAFF', now() - interval '180 days', 'ACTIVE'
FROM generate_series(1, 8 * pg_temp.scale()) n
ON CONFLICT DO NOTHING;

-- ── Pets: four in five live at an approved shelter, the rest with a person ───

CREATE TEMP TABLE demo_pet ON COMMIT DROP AS
SELECT n,
       n % 5 <> 0 AS sheltered,
       -- an organization whose number is not a multiple of 4, so it is ACTIVE
       (n % (8 * pg_temp.scale())) / 4 * 4 + 1 AS org_n
FROM generate_series(1, 60 * pg_temp.scale()) n;

CREATE TEMP TABLE demo_species ON COMMIT DROP AS
SELECT row_number() OVER (ORDER BY name) - 1 AS idx, species_id
FROM pet_species WHERE LOWER(name) IN ('dog', 'cat');

INSERT INTO pets (pet_id, pet_code, name, species_id, breed_id, age, gender, color, weight, description, status,
                  health_status, is_vaccinated, is_neutered, shelter_id, created_at)
SELECT pg_temp.did('pet', d.n),
       'DMP' || lpad(d.n::text, 8, '0'),
       (ARRAY['Mực','Vàng','Bông','Đốm','Mun','Nâu','Sữa','Tôm','Na','Bơ','Cam','Xám'])[1 + d.n % 12],
       s.species_id,
       (SELECT b.breed_id FROM pet_breeds b WHERE b.species_id = s.species_id ORDER BY b.name OFFSET d.n % 5 LIMIT 1),
       3 + d.n % 96,
       (ARRAY['MALE','FEMALE','UNKNOWN'])[1 + d.n % 3],
       (ARRAY['Đen','Vàng','Trắng','Nâu','Xám'])[1 + d.n % 5],
       2 + (d.n % 25),
       'Bé được cứu trong chương trình demo, hiền và thân thiện.',
       CASE WHEN NOT d.sheltered THEN 'ADOPTED' WHEN d.n % 7 = 0 THEN 'FOSTERING' ELSE 'AVAILABLE' END,
       (ARRAY['HEALTHY','HEALTHY','RECOVERING','UNDER_TREATMENT','SPECIAL_NEEDS'])[1 + d.n % 5],
       d.n % 2 = 0, d.n % 3 = 0,
       CASE WHEN d.sheltered THEN pg_temp.did('org', d.org_n) END,
       pg_temp.ago(d.n, 60 * pg_temp.scale())
FROM demo_pet d JOIN demo_species s ON s.idx = d.n % 2
ON CONFLICT DO NOTHING;

INSERT INTO pets_current_owner (pet_id, owner_type, owner_id)
SELECT pg_temp.did('pet', n),
       CASE WHEN sheltered THEN 'ORGANIZATION' ELSE 'USER' END,
       CASE WHEN sheltered THEN pg_temp.did('org', org_n) ELSE pg_temp.did('user', 1 + n % (30 * pg_temp.scale())) END
FROM demo_pet
ON CONFLICT DO NOTHING;

INSERT INTO pet_ownerships (pet_id, owner_type, owner_id, from_time)
SELECT o.pet_id, o.owner_type, o.owner_id, p.created_at
FROM pets_current_owner o JOIN pets p ON p.pet_id = o.pet_id
WHERE p.pet_code LIKE 'DMP%'
ON CONFLICT DO NOTHING;

INSERT INTO pet_media (media_id, pet_id, media_file_id, type, is_primary, created_at)
SELECT pg_temp.did('pet-media', n), pg_temp.did('pet', n), m.media_id, 'image', TRUE, now() - interval '190 days'
FROM generate_series(1, 60 * pg_temp.scale()) n JOIN demo_media m ON m.idx = n % 4
ON CONFLICT DO NOTHING;

-- ── Rescue cases: 35% new, 25% in progress, 15% rescued, 25% closed ──────────

CREATE TEMP TABLE demo_case ON COMMIT DROP AS
SELECT n,
       CASE WHEN n % 20 < 7 THEN 'REPORTED' WHEN n % 20 < 12 THEN 'IN_PROGRESS' WHEN n % 20 < 15 THEN 'RESCUED' ELSE 'CLOSED' END AS status,
       1 + n % (30 * pg_temp.scale()) AS reporter_n,
       1 + (n + 11) % (30 * pg_temp.scale()) AS helper_n,
       (n % (8 * pg_temp.scale())) / 4 * 4 + 1 AS org_n
FROM generate_series(1, 80 * pg_temp.scale()) n;

INSERT INTO rescue_cases (case_id, case_code, reported_by, organization_id, species, color, size, priority, description,
                          location, location_text, province_code, province_name, ward_code, ward_name, status,
                          contact_phone, needs_help, reported_at, resolved_at, created_at, updated_at)
SELECT pg_temp.did('case', d.n),
       'DMC' || lpad(d.n::text, 8, '0'),
       pg_temp.did('user', d.reporter_n),
       CASE WHEN d.status <> 'REPORTED' AND d.n % 2 = 0 THEN pg_temp.did('org', d.org_n) END,
       (ARRAY['Dog','Cat','Other'])[1 + d.n % 3],
       (ARRAY['Đen','Vàng','Trắng','Nâu'])[1 + d.n % 4],
       (ARRAY['SMALL','MEDIUM','LARGE'])[1 + d.n % 3],
       (ARRAY['CRITICAL','HIGH','NORMAL','NORMAL','LOW'])[1 + d.n % 5],
       (ARRAY['Bé bị thương ở chân, không đi được.','Bị bỏ rơi trong thùng giấy ven đường.',
              'Gầy yếu, cần được chăm sóc gấp.','Mắc kẹt trên mái nhà, người dân không tiếp cận được.'])[1 + d.n % 4],
       ST_SetSRID(ST_MakePoint(p.lng + (d.n % 60) * 0.002, p.lat + (d.n % 53) * 0.002), 4326),
       'Gần số ' || (d.n % 300 + 1) || ' Đường Demo, ' || p.ward_name,
       p.province_code, p.province_name, p.ward_code, p.ward_name,
       d.status,
       '09' || lpad((d.n * 6151 % 100000000)::text, 8, '0'),
       d.status = 'REPORTED' AND d.n % 17 = 0,
       pg_temp.ago(d.n, 80 * pg_temp.scale()),
       CASE WHEN d.status IN ('RESCUED', 'CLOSED') THEN pg_temp.ago(d.n, 80 * pg_temp.scale()) + interval '6 hours' END,
       pg_temp.ago(d.n, 80 * pg_temp.scale()),
       now()
FROM demo_case d JOIN demo_place p ON p.idx = d.n % 3
ON CONFLICT DO NOTHING;

INSERT INTO rescue_media (case_id, media_id)
SELECT pg_temp.did('case', d.n), m.media_id FROM demo_case d JOIN demo_media m ON m.idx = d.n % 4
ON CONFLICT DO NOTHING;

-- Someone is on the way to every case in progress. Long expiry: the demo should not end on its own.
INSERT INTO rescue_case_claims (claim_id, case_id, user_id, status, claimed_at, expires_at)
SELECT pg_temp.did('claim', n), pg_temp.did('case', n), pg_temp.did('user', helper_n), 'ACTIVE',
       now() - interval '1 hour', now() + interval '30 days'
FROM demo_case WHERE status = 'IN_PROGRESS' AND n % 4 <> 0
ON CONFLICT DO NOTHING;

-- Rescued and closed cases have a confirmed report; a quarter of those in progress wait for review.
INSERT INTO rescue_case_completions (completion_id, case_id, rescued_at, rescue_note, location_note, submitted_by, outcome,
                                     status, verified_by, verified_at, created_at)
SELECT pg_temp.did('completion', n), pg_temp.did('case', n), pg_temp.ago(n, 80 * pg_temp.scale()) + interval '5 hours',
       'Đã đưa bé về nơi an toàn.', 'Gần điểm được báo', pg_temp.did('user', helper_n),
       CASE status WHEN 'RESCUED' THEN 'TEMP_HOLDING' WHEN 'CLOSED' THEN 'RELEASED' ELSE 'TEMP_HOLDING' END,
       CASE WHEN status = 'IN_PROGRESS' THEN 'PENDING' ELSE 'APPROVED' END,
       CASE WHEN status <> 'IN_PROGRESS' THEN pg_temp.did('user', reporter_n) END,
       CASE WHEN status <> 'IN_PROGRESS' THEN pg_temp.ago(n, 80 * pg_temp.scale()) + interval '6 hours' END,
       pg_temp.ago(n, 80 * pg_temp.scale()) + interval '5 hours'
FROM demo_case
WHERE status IN ('RESCUED', 'CLOSED') OR (status = 'IN_PROGRESS' AND n % 4 = 0)
ON CONFLICT DO NOTHING;

INSERT INTO rescue_completion_media (completion_id, media_id)
SELECT pg_temp.did('completion', d.n), m.media_id
FROM demo_case d JOIN demo_media m ON m.idx = (d.n + 1) % 4
WHERE d.status IN ('RESCUED', 'CLOSED') OR (d.status = 'IN_PROGRESS' AND d.n % 4 = 0)
ON CONFLICT DO NOTHING;

-- ── Adoption: open applications for shelter pets, finished ones for adopted pets ──

INSERT INTO adoption_applications (application_id, adoption_code, pet_id, applicant_id, organization_id, status, note, experience,
                                   live_condition, decided_at, decided_by, reject_reason, ready_at, created_at, updated_at)
SELECT pg_temp.did('application', d.n),
       'DMA' || lpad(d.n::text, 8, '0'),
       pg_temp.did('pet', d.n),
       pg_temp.did('user', 1 + (d.n * 3) % (30 * pg_temp.scale())),
       pg_temp.did('org', d.org_n),
       s.status,
       'Gia đình tôi muốn nhận nuôi bé.',
       'Đã từng nuôi chó mèo 3 năm.',
       'Nhà có sân, có người ở nhà ban ngày.',
       CASE WHEN s.status <> 'PENDING' THEN pg_temp.ago(d.n, 60 * pg_temp.scale()) + interval '1 day' END,
       CASE WHEN s.status <> 'PENDING' THEN pg_temp.did('owner', d.org_n) END,
       CASE WHEN s.status = 'REJECTED' THEN 'Điều kiện chỗ ở chưa phù hợp.' END,
       CASE WHEN s.status = 'APPROVED' THEN now() + interval '7 days' END,
       pg_temp.ago(d.n, 60 * pg_temp.scale()),
       CASE WHEN s.status <> 'PENDING' THEN pg_temp.ago(d.n, 60 * pg_temp.scale()) + interval '1 day' END
FROM demo_pet d
CROSS JOIN LATERAL (SELECT (ARRAY['PENDING','APPROVED','REJECTED','PENDING','CANCELED'])[1 + d.n % 5] AS status) s
WHERE d.sheltered AND d.n % 3 = 0
ON CONFLICT DO NOTHING;

INSERT INTO adoption_applications (application_id, adoption_code, pet_id, applicant_id, organization_id, status, note, experience,
                                   live_condition, decided_at, decided_by, created_at, updated_at)
SELECT pg_temp.did('application', d.n),
       'DMA' || lpad(d.n::text, 8, '0'),
       pg_temp.did('pet', d.n),
       pg_temp.did('user', 1 + d.n % (30 * pg_temp.scale())),
       pg_temp.did('org', d.org_n),
       'COMPLETED',
       'Đã đón bé về nhà.', 'Đã từng nuôi chó mèo.', 'Nhà có sân.',
       pg_temp.ago(d.n, 60 * pg_temp.scale()),
       pg_temp.did('owner', d.org_n),
       pg_temp.ago(d.n, 60 * pg_temp.scale()) - interval '7 days',
       pg_temp.ago(d.n, 60 * pg_temp.scale())
FROM demo_pet d
WHERE NOT d.sheltered
ON CONFLICT DO NOTHING;

-- ── Social: friends and notifications for the first users ────────────────────

INSERT INTO friend_requests (request_id, requester_id, addressee_id, status, responded_at, created_at)
SELECT pg_temp.did('friend', n), pg_temp.did('user', n), pg_temp.did('user', n + 1),
       CASE WHEN n % 3 = 0 THEN 'PENDING' ELSE 'ACCEPTED' END,
       CASE WHEN n % 3 = 0 THEN NULL ELSE now() - interval '2 days' END,
       now() - interval '3 days'
FROM generate_series(1, 29 * pg_temp.scale()) n
ON CONFLICT DO NOTHING;

INSERT INTO notifications (notification_id, user_id, type, title, body, data, created_at)
SELECT pg_temp.did('notification', d.n), pg_temp.did('user', d.reporter_n), 'RESCUE_CLAIMED',
       'Có người nhận ca DMC' || lpad(d.n::text, 8, '0'), 'Có người đang đến giúp ca của bạn.',
       jsonb_build_object('caseId', pg_temp.did('case', d.n)), now() - interval '1 hour'
FROM demo_case d WHERE d.status = 'IN_PROGRESS'
ON CONFLICT DO NOTHING;

COMMIT;

-- Run this to see what is there.
SELECT 'users' AS what, COUNT(*) FROM users WHERE user_code LIKE 'DMU%'
UNION ALL SELECT 'organizations', COUNT(*) FROM organizations WHERE organization_code LIKE 'DMO%'
UNION ALL SELECT 'pets', COUNT(*) FROM pets WHERE pet_code LIKE 'DMP%'
UNION ALL SELECT 'rescue_cases', COUNT(*) FROM rescue_cases WHERE case_code LIKE 'DMC%'
UNION ALL SELECT 'adoptions', COUNT(*) FROM adoption_applications WHERE adoption_code LIKE 'DMA%';
