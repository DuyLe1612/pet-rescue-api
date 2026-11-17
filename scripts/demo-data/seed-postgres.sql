-- ============================================================================
-- Pet Rescue demo data (PostgreSQL) — for staging and load tests, NEVER production.
--
-- Not a Flyway migration: it runs only when someone runs it. Needs every migration applied
-- (V35 or later: the data follows its rules, catalog values in English, enum constants in
-- upper case, tag codes as slugs).
--
--   psql "$STAGING_DB_URL" -v ON_ERROR_STOP=1 -f scripts/demo-data/seed-postgres.sql
--
-- Safe to run again: ids are derived from a fixed text, so existing rows are left alone.
-- That also means a rerun does not refresh rows written by an older version of this script;
-- run cleanup-postgres.sql first to replace them. All accounts use the password in README.md.
--
-- What is stored follows the API's own rules: species and breeds are catalog names in English
-- ("Dog", "Vietnamese Native Dog"), enums are constants ("MALE", "SMALL"). Free text people
-- write (descriptions, colours, notes, addresses) is Vietnamese, as real users would write it.
--
-- Codes look like real ones with a D after the dash (R-D0001, P-D0001, U-DU0001), so they are
-- easy to spot and can never collide with the numeric codes the API hands out.
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
CREATE OR REPLACE FUNCTION pg_temp.code(prefix TEXT, n INT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE AS $$ SELECT prefix || CASE WHEN n < 10000 THEN lpad(n::text, 4, '0') ELSE n::text END $$;
-- A Vietnamese mobile number with a real carrier prefix.
CREATE OR REPLACE FUNCTION pg_temp.phone(seed INT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE AS $$
    SELECT (ARRAY['090','093','097','098','086','035','038','070','077','081','091','094'])[1 + seed % 12]
           || lpad((seed * 7919 % 10000000)::text, 7, '0') $$;

-- ── Places: real wards and streets after the 2025 reorganization ──────────────

CREATE TEMP TABLE demo_place (idx INT, province_code TEXT, province_name TEXT, city TEXT, ward_code TEXT, ward_name TEXT,
                              lat FLOAT8, lng FLOAT8, street1 TEXT, street2 TEXT) ON COMMIT DROP;
INSERT INTO demo_place VALUES
    ( 0, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '26929', 'Phường Bình Thạnh', 10.8050, 106.7100, 'Xô Viết Nghệ Tĩnh', 'Bạch Đằng'),
    ( 1, '1',  'Thành phố Hà Nội',      'Hà Nội',          '103',   'Phường Tây Hồ',     21.0700, 105.8200, 'Âu Cơ', 'Xuân Diệu'),
    ( 2, '48', 'Thành phố Đà Nẵng',     'Đà Nẵng',         '20242', 'Phường Hải Châu',   16.0680, 108.2210, 'Bạch Đằng', 'Lê Duẩn'),
    ( 3, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '26884', 'Phường Gò Vấp',     10.8380, 106.6650, 'Quang Trung', 'Nguyễn Văn Nghi'),
    ( 4, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '27073', 'Phường Phú Nhuận',  10.7990, 106.6800, 'Phan Đình Phùng', 'Phan Xích Long'),
    ( 5, '1',  'Thành phố Hà Nội',      'Hà Nội',          '145',   'Phường Long Biên',  21.0450, 105.8900, 'Nguyễn Văn Cừ', 'Ngọc Lâm'),
    ( 6, '48', 'Thành phố Đà Nẵng',     'Đà Nẵng',         '20263', 'Phường Sơn Trà',    16.0830, 108.2400, 'Ngô Quyền', 'Võ Văn Kiệt'),
    ( 7, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '26824', 'Phường Thủ Đức',    10.8500, 106.7550, 'Võ Văn Ngân', 'Kha Vạn Cân'),
    ( 8, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '26740', 'Phường Sài Gòn',    10.7769, 106.7009, 'Lê Lợi', 'Đồng Khởi'),
    ( 9, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '26743', 'Phường Bến Thành',  10.7721, 106.6983, 'Lê Thánh Tôn', 'Phạm Ngũ Lão'),
    (10, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '26737', 'Phường Tân Định',   10.7915, 106.6900, 'Hai Bà Trưng', 'Trần Quang Khải'),
    (11, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '27154', 'Phường Bàn Cờ',     10.7725, 106.6810, 'Nguyễn Đình Chiểu', 'Cách Mạng Tháng Tám'),
    (12, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '27004', 'Phường Tân Bình',   10.8010, 106.6530, 'Cộng Hòa', 'Hoàng Văn Thụ'),
    (13, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '26944', 'Phường Gia Định',   10.8030, 106.6950, 'Phan Đăng Lưu', 'Nơ Trang Long'),
    (14, '79', 'Thành phố Hồ Chí Minh', 'TP. Hồ Chí Minh', '27343', 'Phường Chợ Lớn',    10.7520, 106.6600, 'Hải Thượng Lãn Ông', 'Châu Văn Liêm'),
    (15, '1',  'Thành phố Hà Nội',      'Hà Nội',          '4',     'Phường Ba Đình',    21.0350, 105.8340, 'Đội Cấn', 'Kim Mã'),
    (16, '1',  'Thành phố Hà Nội',      'Hà Nội',          '70',    'Phường Hoàn Kiếm',  21.0285, 105.8520, 'Hàng Bài', 'Đinh Tiên Hoàng'),
    (17, '1',  'Thành phố Hà Nội',      'Hà Nội',          '166',   'Phường Cầu Giấy',   21.0330, 105.7950, 'Xuân Thủy', 'Trần Thái Tông'),
    (18, '1',  'Thành phố Hà Nội',      'Hà Nội',          '235',   'Phường Đống Đa',    21.0180, 105.8290, 'Tôn Đức Thắng', 'Xã Đàn'),
    (19, '1',  'Thành phố Hà Nội',      'Hà Nội',          '256',   'Phường Hai Bà Trưng', 21.0090, 105.8580, 'Bạch Mai', 'Phố Huế'),
    (20, '1',  'Thành phố Hà Nội',      'Hà Nội',          '367',   'Phường Thanh Xuân', 20.9950, 105.8100, 'Nguyễn Trãi', 'Khương Đình'),
    (21, '48', 'Thành phố Đà Nẵng',     'Đà Nẵng',         '20209', 'Phường Thanh Khê',  16.0640, 108.1900, 'Điện Biên Phủ', 'Hà Huy Tập'),
    (22, '48', 'Thành phố Đà Nẵng',     'Đà Nẵng',         '20285', 'Phường Ngũ Hành Sơn', 16.0000, 108.2500, 'Lê Văn Hiến', 'Trường Sa'),
    (23, '48', 'Thành phố Đà Nẵng',     'Đà Nẵng',         '20257', 'Phường Hòa Cường',  16.0400, 108.2150, 'Núi Thành', '2 Tháng 9');

-- ── Photos: Unsplash (free licence). The API returns a full URL public id as is ──
-- kind: pet-* for adoption profiles, case-* for street shots of rescue cases.

CREATE TEMP TABLE demo_photo (kind TEXT, idx INT, photo_id TEXT, color TEXT, breed TEXT) ON COMMIT DROP;
INSERT INTO demo_photo VALUES
    ('pet-dog',  0, '1552053831-71594a27632d',  'Vàng',      'Labrador Retriever'),
    ('pet-dog',  1, '1601979031925-424e53b6caaa', 'Trắng nâu', 'Mixed Breed'),
    ('pet-dog',  2, '1561037404-61cd46aa615b',  'Đen trắng', 'Vietnamese Native Dog'),
    ('pet-dog',  3, '1534361960057-19889db9621e', 'Trắng',    'Shih Tzu'),
    ('pet-dog',  4, '1503256207526-0d5d80fa2f47', 'Đen trắng', 'Mixed Breed'),
    ('pet-dog',  5, '1537151608828-ea2b11777ee8', 'Nâu trắng', 'Vietnamese Native Dog'),
    ('pet-dog',  6, '1600804340584-c7db2eacf0bf', 'Vàng nhạt', 'Golden Retriever'),
    ('pet-dog',  7, '1518020382113-a7e8fc38eac9', 'Đen',      'Vietnamese Native Dog'),
    ('pet-dog',  8, '1537151625747-768eb6cf92b2', 'Nâu trắng', 'Vietnamese Native Dog'),
    ('pet-dog',  9, '1560807707-8cc77767d783',  'Trắng nâu', 'Mixed Breed'),
    ('pet-dog', 10, '1591160690555-5debfba289f0', 'Vàng',     'Golden Retriever'),
    ('pet-dog', 11, '1504826260979-242151ee45b7', 'Nâu',      'Vietnamese Native Dog'),
    ('pet-dog', 12, '1760615302825-900295f63a2c', 'Vàng',     'Golden Retriever'),
    ('pet-dog', 13, '1629555256545-e29db1b05f86', 'Nâu',      'Vietnamese Native Dog'),
    ('pet-dog', 14, '1507543139012-85b5900144e4', 'Nâu trắng', 'Vietnamese Native Dog'),
    ('pet-dog', 15, '1700893945560-f1e4f3513e33', 'Nâu trắng', 'Vietnamese Native Dog'),
    ('pet-dog', 16, '1636772611113-d44a76913388', 'Trắng',    'Mixed Breed'),
    ('pet-dog', 17, '1552036354-d0ee14b417e0',  'Trắng nâu', 'Vietnamese Native Dog'),
    ('pet-cat',  0, '1592194996308-7b43878e84a6', 'Trắng nâu', 'Mixed Breed'),
    ('pet-cat',  1, '1573865526739-10659fec78a5', 'Cam trắng', 'Domestic Shorthair'),
    ('pet-cat',  2, '1574144611937-0df059b5ef3e', 'Xám mướp',  'Domestic Shorthair'),
    ('pet-cat',  3, '1561948955-570b270e7c36',  'Trắng',     'Mixed Breed'),
    ('pet-cat',  4, '1560114928-40f1f1eb26a0',  'Cam mướp',  'Domestic Shorthair'),
    ('pet-cat',  5, '1472491235688-bdc81a63246e', 'Trắng xám', 'Domestic Shorthair'),
    ('pet-cat',  6, '1529778873920-4da4926a72c2', 'Nâu mướp',  'Domestic Shorthair'),
    ('pet-cat',  7, '1478098711619-5ab0b478d6e6', 'Nâu mướp',  'Domestic Shorthair'),
    ('pet-cat',  8, '1595433707802-6b2626ef1c91', 'Nâu đen',   'Domestic Shorthair'),
    ('pet-cat',  9, '1533743983669-94fa5c4338ec', 'Trắng xám', 'Mixed Breed'),
    ('pet-cat', 10, '1555685812-4b943f1cb0eb',  'Nâu mướp',  'Domestic Shorthair'),
    ('pet-cat', 11, '1494256997604-768d1f608cac', 'Xám mướp',  'Scottish Fold'),
    ('pet-cat', 12, '1492370284958-c20b15c692d2', 'Xám xanh',  'Russian Blue'),
    ('pet-cat', 13, '1757430757969-3d2b92025097', 'Nâu mướp',  'Domestic Shorthair'),
    ('pet-cat', 14, '1763839361290-4d711e42ab5f', 'Tam thể',   'Domestic Shorthair'),
    ('pet-cat', 15, '1530991671072-ac4f81c2c3c1', 'Tam thể',   'Domestic Shorthair'),
    ('case-dog',  0, '1770185584605-95e864143593', 'Vện',      NULL),
    ('case-dog',  1, '1573828260583-ec780bbe0c1a', 'Nâu',      NULL),
    ('case-dog',  2, '1785472895235-41a813f52160', 'Trắng đen', NULL),
    ('case-dog',  3, '1629555256341-09e5e9871647', 'Nâu',      NULL),
    ('case-dog',  4, '1759289615419-509c6da47a23', 'Nâu',      NULL),
    ('case-dog',  5, '1558623535-33cc88178982',  'Trắng',    NULL),
    ('case-dog',  6, '1764306737125-8f632ef29ea1', 'Vàng',     NULL),
    ('case-dog',  7, '1709497083548-b6669f390b64', 'Vàng',     NULL),
    ('case-dog',  8, '1444212477490-ca407925329e', 'Nâu',      NULL),
    ('case-dog',  9, '1553434133-96822a8e94af',  'Vàng',     NULL),
    ('case-dog', 10, '1516280287949-2747a3304a2f', 'Nâu',      NULL),
    ('case-cat',  0, '1604675223954-b1aabd668078', 'Cam trắng', NULL),
    ('case-cat',  1, '1760618813526-66e940f1d84b', 'Đen',      NULL),
    ('case-cat',  2, '1503362516536-635096dd5a80', 'Nâu mướp', NULL),
    ('case-cat',  3, '1609779361684-8196b3a0abf1', 'Cam trắng', NULL),
    ('case-cat',  4, '1761731828949-a78d41e0057a', 'Mướp',     NULL),
    ('case-cat',  5, '1622273413883-265d478feda3', 'Cam',      NULL),
    ('case-cat',  6, '1498579687545-d5a4fffb0a9e', 'Xám mướp', NULL),
    ('case-cat',  7, '1759239432958-34a6ae1062fc', 'Tam thể',  NULL),
    ('case-cat',  8, '1765044312946-d4ebfb674608', 'Mướp',     NULL),
    ('case-cat',  9, '1637199809036-cfda31b63399', 'Xám',      NULL),
    ('case-cat', 10, '1490481920145-fc78891bbb99', 'Nâu mướp', NULL),
    ('case-cat', 11, '1650036786401-30f469106781', 'Mướp',     NULL);

-- ── Accounts: usernames and emails stay fixed so the README logins keep working ──

CREATE TEMP TABLE demo_account ON COMMIT DROP AS
SELECT 'admin' AS kind, 1 AS n, 'demo.admin' AS username, 'Lê Quang Hải' AS full_name, 'ADMIN' AS role, 'U-DA' AS code_prefix
UNION ALL SELECT 'owner', n, 'demo.owner' || n,
       (ARRAY['Trần Thị Hồng Nhung','Nguyễn Văn Hậu','Lê Minh Tâm','Phạm Hữu Lộc','Đoàn Thanh Bình',
              'Võ Thị Kim Oanh','Đặng Quang Vinh','Hoàng Thu Thủy'])[1 + (n - 1) % 8],
       'MEMBER', 'U-DO' FROM generate_series(1, 8 * pg_temp.scale()) n
UNION ALL SELECT 'staff', n, 'demo.staff' || n,
       (ARRAY['Nguyễn Thanh Tâm','Trần Văn Khải','Lê Ngọc Yến','Phạm Minh Đức','Bùi Thị Hạnh',
              'Hồ Quốc Bảo','Dương Thùy Dung','Ngô Trọng Nghĩa'])[1 + (n - 1) % 8],
       'MEMBER', 'U-DS' FROM generate_series(1, 8 * pg_temp.scale()) n
UNION ALL SELECT 'user', n, 'demo.user' || n,
       (ARRAY['Nguyễn Minh Anh','Trần Gia Bảo','Lê Thu Hà','Phạm Quốc Huy','Hoàng Ngọc Lan','Vũ Đức Thịnh',
              'Đặng Thảo Vy','Bùi Tuấn Kiệt','Đỗ Khánh Linh','Ngô Hải Đăng','Dương Mỹ Duyên','Lý Hoàng Nam',
              'Phan Bảo Ngọc','Võ Thanh Tùng','Trịnh Diệu Linh','Mai Anh Khoa','Hồ Thanh Trúc','Lâm Gia Huy',
              'Tạ Minh Châu','Châu Ngọc Hân','Nguyễn Đức Long','Trần Phương Thảo','Lê Hoàng Phúc','Phạm Thu Trang',
              'Huỳnh Tấn Phát','Đinh Ngọc Ánh','Cao Minh Quân','Vương Bích Ngọc','Kiều Anh Tú','Lưu Quỳnh Như'])[1 + (n - 1) % 30],
       'USER', 'U-DU' FROM generate_series(1, 30 * pg_temp.scale()) n;

INSERT INTO users (user_id, user_code, username, email, password_hash, full_name, phone, gender, status, email_verified,
                   province_code, province_name, ward_code, ward_name, created_at)
SELECT pg_temp.did(a.kind, a.n),
       pg_temp.code(a.code_prefix, a.n),
       a.username, a.username || '@petrescue.demo',
       '$2a$10$y.fXK2Rk.lGsNVd8axBbsOj/K9JQ7toKWtEeReT6vTMtXqR5Bq002',
       a.full_name, pg_temp.phone(a.n * 13 + length(a.kind)),
       -- Vietnamese given names do not reliably tell gender; alternate, and leave some unset.
       (ARRAY['FEMALE','MALE','FEMALE','MALE',NULL])[1 + a.n % 5],
       'ACTIVE', TRUE,
       p.province_code, p.province_name, p.ward_code, p.ward_name,
       now() - interval '200 days'
FROM demo_account a JOIN demo_place p ON p.idx = a.n % 24
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT pg_temp.did(a.kind, a.n), r.role_id
FROM demo_account a JOIN roles r ON r.code = a.role
ON CONFLICT DO NOTHING;

INSERT INTO user_reputations (user_id, score, level, updated_at)
SELECT pg_temp.did('user', n), (n * 37) % 400, 'NEW', now()
FROM generate_series(1, 30 * pg_temp.scale()) n
ON CONFLICT DO NOTHING;

-- One file row per photo; public_id is the photo URL (unique per photo).
INSERT INTO media_files (media_id, uploader_id, public_id, resource_type, format, status, folder, created_at)
SELECT pg_temp.did('photo-' || f.kind, f.idx), pg_temp.did('admin', 1),
       'https://images.unsplash.com/photo-' || f.photo_id || '?w=1200&q=80&auto=format&fit=crop',
       'image', 'jpg', 'PERMANENT', 'demo', now() - interval '200 days'
FROM demo_photo f
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE demo_media ON COMMIT DROP AS
SELECT f.kind, f.idx, f.color, f.breed, m.media_id
FROM demo_photo f
JOIN media_files m ON m.public_id = 'https://images.unsplash.com/photo-' || f.photo_id || '?w=1200&q=80&auto=format&fit=crop';

-- ── Organizations: 4 and 8 still wait for approval, 5 is a vet clinic ───────

CREATE TEMP TABLE demo_org_base (n INT, place_idx INT, type TEXT, name TEXT, slug TEXT, street TEXT, description TEXT) ON COMMIT DROP;
INSERT INTO demo_org_base VALUES
    (1, 0, 'SHELTER',    'Trạm cứu hộ Mái Ấm Sài Gòn', 'maiamsaigon', '112 Xô Viết Nghệ Tĩnh',
        'Nhóm tình nguyện cứu chó mèo bị bỏ rơi ở Bình Thạnh từ 2019. Trạm đang chăm khoảng 40 bé, nhận báo ca 24/7.'),
    (2, 1, 'SHELTER',    'Nhà Tạm Mèo Hà Nội', 'nhatammeo', '18 ngõ 52 Âu Cơ',
        'Nhà tạm cho mèo hoang và mèo bị bỏ rơi ở Tây Hồ. Mèo được triệt sản, tiêm phòng trước khi tìm nhà mới.'),
    (3, 2, 'SHELTER',    'Nhóm Cứu hộ Chân Nhỏ Đà Nẵng', 'channho', '45 Lê Duẩn',
        'Nhóm sinh viên và người đi làm ở Đà Nẵng, cứu hộ chó mèo bị tai nạn và tìm gia đình nhận nuôi.'),
    (4, 3, 'SHELTER',    'Trạm cứu hộ Bốn Chân Gò Vấp', 'bonchan', '307 Quang Trung',
        'Trạm mới mở ở Gò Vấp, đang chờ duyệt hồ sơ tổ chức.'),
    (5, 4, 'VET_CENTER', 'Phòng khám thú y An Tâm', 'antam', '86 Phan Xích Long',
        'Phòng khám thú y nhận điều trị giá ưu đãi cho thú được cứu hộ và lưu chuồng tạm cho các trạm.'),
    (6, 5, 'SHELTER',    'Mái ấm Thú Hoang Long Biên', 'thuhoang', '9 Ngọc Lâm',
        'Mái ấm nhỏ ở Long Biên, chuyên nhận chó lớn và chó bị thương nặng.'),
    (7, 6, 'SHELTER',    'Trạm cứu hộ Biển Xanh Sơn Trà', 'bienxanh', '210 Ngô Quyền',
        'Trạm cứu hộ gần biển Sơn Trà, phối hợp với ban quản lý chợ để đưa chó mèo lang thang về chăm sóc.'),
    (8, 7, 'SHELTER',    'Nhà Mèo Thủ Đức', 'nhameothuduc', '24 Kha Vạn Cân',
        'Nhóm cứu hộ mèo khu Thủ Đức, đang chờ duyệt hồ sơ tổ chức.');

INSERT INTO organizations (organization_id, organization_code, name, type, description, street_address,
                           province_code, province_name, ward_code, ward_name, location, phone, email,
                           official_link, status, requested_by_user_id, created_at)
SELECT pg_temp.did('org', g.n),
       pg_temp.code('O-D', g.n),
       b.name || CASE WHEN g.n > 8 THEN ' (cơ sở ' || ((g.n - 1) / 8 + 1) || ')' ELSE '' END,
       b.type, b.description, b.street,
       p.province_code, p.province_name, p.ward_code, p.ward_name,
       ST_SetSRID(ST_MakePoint(p.lng + (g.n % 7) * 0.004, p.lat + (g.n % 5) * 0.004), 4326),
       CASE p.province_code WHEN '79' THEN '028' WHEN '1' THEN '024' ELSE '0236' END
           || lpad((g.n * 104729 % 10000000)::text, 7, '0'),
       b.slug || CASE WHEN g.n > 8 THEN g.n::text ELSE '' END || '@petrescue.demo',
       NULL,
       CASE WHEN g.n % 4 = 0 THEN 'PENDING' ELSE 'ACTIVE' END,
       pg_temp.did('owner', g.n),
       now() - make_interval(days => (g.n * 17 % 190))
FROM generate_series(1, 8 * pg_temp.scale()) AS g(n)
JOIN demo_org_base b ON b.n = 1 + (g.n - 1) % 8
JOIN demo_place p ON p.idx = b.place_idx
ON CONFLICT DO NOTHING;

INSERT INTO organization_members (organization_id, user_id, role, joined_at, status)
SELECT pg_temp.did('org', n), pg_temp.did('owner', n), 'OWNER', now() - interval '190 days', 'ACTIVE'
FROM generate_series(1, 8 * pg_temp.scale()) n
UNION ALL
SELECT pg_temp.did('org', n), pg_temp.did('staff', n), CASE WHEN n % 5 = 0 THEN 'VET' ELSE 'STAFF' END,
       now() - interval '180 days', 'ACTIVE'
FROM generate_series(1, 8 * pg_temp.scale()) n
ON CONFLICT DO NOTHING;

-- ── Pets: four in five live at an approved organization, the rest with a person ──

CREATE TEMP TABLE demo_pet ON COMMIT DROP AS
SELECT n,
       n % 5 <> 0 AS sheltered,
       CASE WHEN n % 2 = 1 THEN 'Dog' ELSE 'Cat' END AS species,
       CASE WHEN n % 2 = 1 THEN 'pet-dog' ELSE 'pet-cat' END AS photo_kind,
       -- the k-th organization whose number is not a multiple of 4, so it is ACTIVE
       ((n - 1) % (6 * pg_temp.scale())) + ((n - 1) % (6 * pg_temp.scale())) / 3 + 1 AS org_n,
       (n + 1) / 2 - 1 AS within_species
FROM generate_series(1, 60 * pg_temp.scale()) n;

ALTER TABLE demo_pet ADD COLUMN photo_idx INT, ADD COLUMN name TEXT, ADD COLUMN age INT, ADD COLUMN health TEXT;
UPDATE demo_pet SET
    photo_idx = within_species % (CASE species WHEN 'Dog' THEN 18 ELSE 16 END),
    name = CASE species
        WHEN 'Dog' THEN (ARRAY['Lu','Bin','Cún','Mít','Bắp','Gạo','Ki','Lucky','Milo','Tô','Bánh Bao','Xoài','Chè',
                               'Sắn','Bơ','Su','Lạc','Mập','Tí','Cốm','Đậu','Rô','Bo','Bi','Cà Rốt','Tũn','Pi','Ken','Mon','Bột'])[1 + within_species % 30]
        ELSE (ARRAY['Miu','Mochi','Na','Kiwi','Tôm','Muối','Mây','Nấm','Mimi','Bé Bự','Hạt Dẻ','Kẹo','Sushi','Chanh',
                    'Ốc','Vừng','Bon','Rêu','Khoai','Cúc','Lily','Tofu','Bánh Quy','Mỡ','Pate','Sò','Nhím','Leo','Bơ Sữa','Bông'])[1 + within_species % 30]
    END,
    age = (ARRAY[3, 5, 8, 12, 18, 24, 30, 36, 48, 60])[1 + n % 10],
    health = (ARRAY['HEALTHY','HEALTHY','HEALTHY','RECOVERING','UNDER_TREATMENT','SPECIAL_NEEDS'])[1 + n % 6];

INSERT INTO pets (pet_id, pet_code, name, species_id, breed_id, age, gender, color, weight, description, status,
                  health_status, is_vaccinated, is_neutered, shelter_id, created_at)
SELECT pg_temp.did('pet', d.n),
       pg_temp.code('P-D', d.n),
       d.name,
       s.species_id,
       (SELECT b.breed_id FROM pet_breeds b WHERE b.species_id = s.species_id AND b.name = m.breed AND b.is_deleted = FALSE LIMIT 1),
       d.age,
       (ARRAY['MALE','FEMALE','MALE','FEMALE','UNKNOWN'])[1 + d.n % 5],
       m.color,
       CASE d.species WHEN 'Dog' THEN round(LEAST(25, 2.5 + d.age * 0.35)::numeric, 1)
                      ELSE round(LEAST(5.5, 0.8 + d.age * 0.12)::numeric, 1) END,
       CASE d.health
           WHEN 'UNDER_TREATMENT' THEN format('%s đang được điều trị viêm da, cần người nhận nuôi kiên nhẫn bôi thuốc thêm khoảng hai tuần. Bé ăn tốt và rất thích được vuốt ve.', d.name)
           WHEN 'SPECIAL_NEEDS' THEN format('%s bị mờ một bên mắt sau khi được cứu nên cần gia đình chăm kỹ hơn bình thường. Bé vẫn chơi đùa vui vẻ và đã quen đi vệ sinh đúng chỗ.', d.name)
           WHEN 'RECOVERING' THEN format('%s vừa qua đợt điều trị sau khi được cứu và đang hồi phục tốt. Trạm sẽ gửi kèm lịch tái khám cho gia đình nhận nuôi.', d.name)
           ELSE (ARRAY[
               format('%s được tìm thấy trong thùng các-tông trước cổng chợ khi mới khoảng một tháng tuổi. Bé đã quen sống trong nhà, thích được bế và ngủ cạnh người.', d.name),
               format('%s được người dân báo khi đang lang thang ở bãi giữ xe. Sau ba tuần chăm sóc, bé đã tăng cân và rất quấn người.', d.name),
               format('%s hiền, thích chơi với trẻ con và đã biết đi vệ sinh đúng chỗ. Phù hợp với gia đình có sân nhỏ.', d.name),
               format('%s bị bỏ lại khi chủ cũ chuyển nhà. Lúc đầu bé hơi nhút nhát nhưng chỉ vài ngày là thân.', d.name),
               format('%s năng động, cần được dắt đi dạo hoặc chơi mỗi ngày. Trạm ưu tiên gia đình đã có kinh nghiệm nuôi.', d.name),
               format('%s ăn được cả hạt lẫn cơm, sống hòa thuận với chó mèo khác trong trạm.', d.name),
               format('%s thích nằm phơi nắng và được chải lông, ít khi kêu. Hợp với người ở căn hộ.', d.name),
               format('%s được một bạn sinh viên nhặt về từ công trình xây dựng. Bé tò mò, hay theo chân người.', d.name)
           ])[1 + d.n % 8]
       END,
       CASE WHEN NOT d.sheltered THEN 'ADOPTED' WHEN d.n % 7 = 0 THEN 'FOSTERING' ELSE 'AVAILABLE' END,
       d.health,
       d.health = 'HEALTHY' OR d.n % 3 <> 0,
       d.age >= 6 AND d.n % 2 = 0,
       CASE WHEN d.sheltered THEN pg_temp.did('org', d.org_n) END,
       pg_temp.ago(d.n, 60 * pg_temp.scale())
FROM demo_pet d
JOIN pet_species s ON s.name = d.species AND s.is_deleted = FALSE
JOIN demo_media m ON m.kind = d.photo_kind AND m.idx = d.photo_idx
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
WHERE p.pet_code LIKE 'P-D%'
ON CONFLICT DO NOTHING;

INSERT INTO pet_media (media_id, pet_id, media_file_id, type, is_primary, created_at)
SELECT pg_temp.did('pet-media', d.n), pg_temp.did('pet', d.n), m.media_id, 'image', TRUE, now() - interval '190 days'
FROM demo_pet d JOIN demo_media m ON m.kind = d.photo_kind AND m.idx = d.photo_idx
ON CONFLICT DO NOTHING;

-- Every vaccinated pet has its shots on record; pets under treatment have a visit.
INSERT INTO pet_medical_records (record_id, pet_id, description, vaccine, diagnosis, record_date, record_type, created_by, created_at)
SELECT pg_temp.did('vaccine', d.n), pg_temp.did('pet', d.n),
       'Tiêm phòng định kỳ tại trạm',
       CASE d.species WHEN 'Dog' THEN 'Vaccine 7 bệnh và vaccine dại' ELSE 'Vaccine 4 bệnh và vaccine dại' END,
       NULL,
       pg_temp.ago(d.n, 60 * pg_temp.scale()) + interval '10 days', 'VACCINATION',
       pg_temp.did('staff', d.org_n), pg_temp.ago(d.n, 60 * pg_temp.scale()) + interval '10 days'
FROM demo_pet d
WHERE d.sheltered AND (d.health = 'HEALTHY' OR d.n % 3 <> 0)
ON CONFLICT DO NOTHING;

INSERT INTO pet_medical_records (record_id, pet_id, description, vaccine, diagnosis, record_date, record_type, created_by, created_at)
SELECT pg_temp.did('treatment', d.n), pg_temp.did('pet', d.n),
       CASE d.health WHEN 'UNDER_TREATMENT' THEN 'Bôi thuốc và tắm thuốc hai lần mỗi tuần'
                     WHEN 'SPECIAL_NEEDS' THEN 'Khám mắt, nhỏ thuốc hằng ngày'
                     ELSE 'Truyền dịch, bổ sung dinh dưỡng sau khi cứu' END,
       NULL,
       CASE d.health WHEN 'UNDER_TREATMENT' THEN 'Viêm da do nấm'
                     WHEN 'SPECIAL_NEEDS' THEN 'Sẹo giác mạc mắt trái'
                     ELSE 'Suy dinh dưỡng nhẹ' END,
       pg_temp.ago(d.n, 60 * pg_temp.scale()) + interval '3 days', 'TREATMENT',
       pg_temp.did('staff', d.org_n), pg_temp.ago(d.n, 60 * pg_temp.scale()) + interval '3 days'
FROM demo_pet d
WHERE d.sheltered AND d.health <> 'HEALTHY'
ON CONFLICT DO NOTHING;

-- ── Rescue cases: 35% new, 25% in progress, 15% rescued, 25% closed ──────────

CREATE TEMP TABLE demo_case ON COMMIT DROP AS
SELECT n,
       CASE WHEN n % 20 < 7 THEN 'REPORTED' WHEN n % 20 < 12 THEN 'IN_PROGRESS' WHEN n % 20 < 15 THEN 'RESCUED' ELSE 'CLOSED' END AS status,
       CASE WHEN n % 2 = 1 THEN 'Dog' ELSE 'Cat' END AS species,
       CASE WHEN n % 2 = 1 THEN 'case-dog' ELSE 'case-cat' END AS photo_kind,
       ((n + 1) / 2 - 1) % (CASE WHEN n % 2 = 1 THEN 11 ELSE 12 END) AS photo_idx,
       1 + n % (30 * pg_temp.scale()) AS reporter_n,
       1 + (n + 11) % (30 * pg_temp.scale()) AS helper_n,
       ((n - 1) % (6 * pg_temp.scale())) + ((n - 1) % (6 * pg_temp.scale())) / 3 + 1 AS org_n,
       (ARRAY['CRITICAL','HIGH','NORMAL','NORMAL','LOW'])[1 + n % 5] AS priority,
       n % 24 AS place_idx
FROM generate_series(1, 80 * pg_temp.scale()) n;

INSERT INTO rescue_cases (case_id, case_code, reported_by, organization_id, species, color, size, priority, description,
                          location, location_text, province_code, province_name, ward_code, ward_name, status,
                          contact_phone, needs_help, reported_at, resolved_at, created_at, updated_at)
SELECT pg_temp.did('case', d.n),
       pg_temp.code('R-D', d.n),
       pg_temp.did('user', d.reporter_n),
       CASE WHEN d.status <> 'REPORTED' AND d.n % 2 = 0 THEN pg_temp.did('org', d.org_n) END,
       d.species,
       m.color,
       CASE WHEN d.species = 'Cat' THEN (ARRAY['SMALL','SMALL','MEDIUM'])[1 + d.n % 3]
            ELSE (ARRAY['SMALL','MEDIUM','LARGE'])[1 + d.n % 3] END,
       d.priority,
       CASE d.priority
           WHEN 'CRITICAL' THEN (ARRAY[
               'Bé bị xe tông, chân sau chảy máu nhiều, nằm im bên lề đường. Mình đang đứng trông, cần người đến gấp.',
               'Bé mắc kẹt trong cống thoát nước sau mưa, kêu yếu dần. Người dân không kéo lên được.',
               'Bé bị dây thép quấn cổ, vết thương đã nhiễm trùng, thở rất mệt.'])[1 + d.n % 3]
           WHEN 'HIGH' THEN (ARRAY[
               'Bé gầy trơ xương, mắt có ghèn, nằm ở góc chợ từ sáng không chịu ăn.',
               'Một mẹ và ba bé con bị bỏ trong thùng giấy trước cửa nhà, trời sắp mưa.',
               'Bé bị thương ở chân trước, đi cà nhắc, hay bị chó khác trong hẻm đuổi.'])[1 + d.n % 3]
           WHEN 'NORMAL' THEN (ARRAY[
               'Bé đi lạc mấy hôm nay quanh khu này, có đeo vòng cổ đỏ, khá thân người. Có thể là thú nhà ai bị lạc.',
               'Bé hay nằm trước cửa hàng tạp hóa, chủ quán cho ăn nhưng không nuôi được. Bé hiền, cần tìm nhà.',
               'Bé bị ghẻ nhẹ ở lưng, vẫn ăn uống bình thường, cần người đưa đi khám.'])[1 + d.n % 3]
           ELSE (ARRAY[
               'Bé khỏe, chỉ là thú lang thang trong khuôn viên chung cư. Ban quản lý muốn tìm nơi nhận về.',
               'Bé hay ghé xin ăn ở quán cơm, mình muốn nhờ trạm triệt sản rồi thả lại.'])[1 + d.n % 2]
       END,
       ST_SetSRID(ST_MakePoint(p.lng + ((d.n * 37) % 41 - 20) * 0.0006, p.lat + ((d.n * 53) % 37 - 18) * 0.0006), 4326),
       (ARRAY['Trước số ' || (d.n * 7 % 280 + 2) || ' ' || p.street1,
              'Hẻm ' || (d.n * 11 % 300 + 10) || ' ' || p.street2,
              'Gần ngã tư ' || p.street1 || ' - ' || p.street2,
              'Bãi giữ xe cạnh số ' || (d.n * 5 % 200 + 1) || ' ' || p.street2])[1 + d.n % 4],
       p.province_code, p.province_name, p.ward_code, p.ward_name,
       d.status,
       pg_temp.phone(d.n * 31 + 7),
       d.status = 'REPORTED' AND d.priority IN ('CRITICAL', 'HIGH'),
       pg_temp.ago(d.n, 80 * pg_temp.scale()),
       CASE WHEN d.status IN ('RESCUED', 'CLOSED') THEN pg_temp.ago(d.n, 80 * pg_temp.scale()) + interval '6 hours' END,
       pg_temp.ago(d.n, 80 * pg_temp.scale()),
       now()
FROM demo_case d
JOIN demo_place p ON p.idx = d.place_idx
JOIN demo_media m ON m.kind = d.photo_kind AND m.idx = d.photo_idx
ON CONFLICT DO NOTHING;

INSERT INTO rescue_media (case_id, media_id)
SELECT pg_temp.did('case', d.n), m.media_id
FROM demo_case d JOIN demo_media m ON m.kind = d.photo_kind AND m.idx = d.photo_idx
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
SELECT pg_temp.did('completion', d.n), pg_temp.did('case', d.n), pg_temp.ago(d.n, 80 * pg_temp.scale()) + interval '5 hours',
       CASE d.status
           WHEN 'CLOSED' THEN (ARRAY[
               'Đã đưa bé đi khám, vết thương ngoài da. Bé đã được triệt sản và thả lại khu cũ theo ý người báo.',
               'Bé khỏe sau một tuần chăm sóc, đã thả lại khuôn viên có người cho ăn hằng ngày.'])[1 + d.n % 2]
           ELSE (ARRAY[
               'Đã bế bé lên xe và đưa về trạm, bác sĩ đang kiểm tra. Bé tỉnh, uống được nước.',
               'Đã cứu được bé, tạm giữ ở nhà mình chờ trạm sắp xếp chỗ.',
               'Bé đã được đưa ra khỏi chỗ kẹt, chân hơi trầy, đang theo dõi thêm.'])[1 + d.n % 3]
       END,
       'Tại vị trí được báo',
       pg_temp.did('user', d.helper_n),
       CASE d.status WHEN 'CLOSED' THEN 'RELEASED' ELSE 'TEMP_HOLDING' END,
       CASE WHEN d.status = 'IN_PROGRESS' THEN 'PENDING' ELSE 'APPROVED' END,
       CASE WHEN d.status <> 'IN_PROGRESS' THEN pg_temp.did('user', d.reporter_n) END,
       CASE WHEN d.status <> 'IN_PROGRESS' THEN pg_temp.ago(d.n, 80 * pg_temp.scale()) + interval '6 hours' END,
       pg_temp.ago(d.n, 80 * pg_temp.scale()) + interval '5 hours'
FROM demo_case d
WHERE d.status IN ('RESCUED', 'CLOSED') OR (d.status = 'IN_PROGRESS' AND d.n % 4 = 0)
ON CONFLICT DO NOTHING;

INSERT INTO rescue_completion_media (completion_id, media_id)
SELECT pg_temp.did('completion', d.n), m.media_id
FROM demo_case d JOIN demo_media m ON m.kind = d.photo_kind AND m.idx = d.photo_idx
WHERE d.status IN ('RESCUED', 'CLOSED') OR (d.status = 'IN_PROGRESS' AND d.n % 4 = 0)
ON CONFLICT DO NOTHING;

-- ── Adoption: open applications for shelter pets, finished ones for adopted pets ──

INSERT INTO adoption_applications (application_id, adoption_code, pet_id, applicant_id, organization_id, status, note, experience,
                                   live_condition, decided_at, decided_by, reject_reason, ready_at, created_at, updated_at)
SELECT pg_temp.did('application', d.n),
       pg_temp.code('A-D', d.n),
       pg_temp.did('pet', d.n),
       pg_temp.did('user', 1 + (d.n * 3) % (30 * pg_temp.scale())),
       pg_temp.did('org', d.org_n),
       s.status,
       (ARRAY[format('Mình rất thích %s từ lần đầu thấy ảnh. Mình làm việc ở nhà nên có thời gian chăm bé.', d.name),
              format('Nhà mình có hai con nhỏ rất mong được nuôi %s, cả nhà đã bàn kỹ.', d.name),
              format('Mình muốn nhận %s về làm bạn với bé cũ ở nhà, bé cũ rất hiền.', d.name)])[1 + d.n % 3],
       (ARRAY['Đã nuôi chó ta 5 năm, bé mất vì già năm ngoái.',
              'Lần đầu nuôi, đã tìm hiểu cách chăm và có phòng khám quen gần nhà.',
              'Đang nuôi một bé mèo đã triệt sản và tiêm đủ.'])[1 + d.n % 3],
       (ARRAY['Nhà phố có sân trước, cổng luôn đóng.',
              'Căn hộ chung cư cho phép nuôi thú, có lưới an toàn ở ban công.',
              'Nhà vườn ở ngoại thành, có người ở nhà cả ngày.'])[1 + d.n % 3],
       CASE WHEN s.status <> 'PENDING' THEN pg_temp.ago(d.n, 60 * pg_temp.scale()) + interval '1 day' END,
       CASE WHEN s.status <> 'PENDING' THEN pg_temp.did('owner', d.org_n) END,
       CASE WHEN s.status = 'REJECTED' THEN 'Gia đình đi làm cả ngày, bé còn nhỏ cần người ở nhà nhiều hơn.' END,
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
       pg_temp.code('A-D', d.n),
       pg_temp.did('pet', d.n),
       pg_temp.did('user', 1 + d.n % (30 * pg_temp.scale())),
       pg_temp.did('org', d.org_n),
       'COMPLETED',
       format('Đã đón %s về nhà, bé quen chỗ mới sau vài ngày.', d.name),
       'Đã từng nuôi chó mèo.',
       'Nhà phố có sân nhỏ.',
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
       'Có người nhận ca ' || pg_temp.code('R-D', d.n), 'Có người đang trên đường đến giúp ca bạn báo.',
       jsonb_build_object('caseId', pg_temp.did('case', d.n)), now() - interval '1 hour'
FROM demo_case d WHERE d.status = 'IN_PROGRESS'
ON CONFLICT DO NOTHING;

COMMIT;

-- Run this to see what is there.
SELECT 'users' AS what, COUNT(*) FROM users WHERE user_code LIKE 'U-D%'
UNION ALL SELECT 'organizations', COUNT(*) FROM organizations WHERE organization_code LIKE 'O-D%'
UNION ALL SELECT 'pets', COUNT(*) FROM pets WHERE pet_code LIKE 'P-D%'
UNION ALL SELECT 'rescue_cases', COUNT(*) FROM rescue_cases WHERE case_code LIKE 'R-D%'
UNION ALL SELECT 'adoptions', COUNT(*) FROM adoption_applications WHERE adoption_code LIKE 'A-D%'
UNION ALL SELECT 'medical_records', COUNT(*) FROM pet_medical_records r JOIN pets p ON p.pet_id = r.pet_id WHERE p.pet_code LIKE 'P-D%';
