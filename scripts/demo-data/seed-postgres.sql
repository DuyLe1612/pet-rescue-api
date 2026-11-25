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

-- A photo for every centre, so people recognise it on the map and its page. Cat houses get cats.
-- Separate from the insert so centres seeded before this line get one too.
UPDATE organizations o
SET image_url = 'https://images.unsplash.com/photo-' || f.photo_id || '?w=1200&q=80&auto=format&fit=crop'
FROM generate_series(1, 8 * pg_temp.scale()) AS g(n)
JOIN demo_org_base b ON b.n = 1 + (g.n - 1) % 8
JOIN demo_photo f ON f.kind = CASE WHEN b.n IN (2, 8) THEN 'pet-cat' ELSE 'pet-dog' END
                 AND f.idx = (g.n - 1) % 8
WHERE o.organization_id = pg_temp.did('org', g.n)
  AND o.image_url IS NULL;

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

-- ══ Every feature has data ═════════════════════════════════════════════════════
-- The scenarios in README "Kịch bản demo" run on three accounts: demo.user1, demo.owner1
-- (organization 1, Trạm cứu hộ Mái Ấm Sài Gòn, in TP.HCM) and demo.admin. Organization 1 has
-- pets 1, 7, 13, 19, 31, 37, 43 and 49 (all dogs). Rows made only for a scenario use number 9001
-- and up, so they never collide with the generated ones.

-- How each pet came to its owner ("Thú của tôi" shows it).
UPDATE pets_current_owner o
SET acquisition_type = CASE WHEN d.sheltered THEN 'ORG_INTAKE' ELSE 'ADOPTED' END,
    adoption_application_id = CASE WHEN d.sheltered THEN NULL ELSE pg_temp.did('application', d.n) END
FROM demo_pet d
WHERE o.pet_id = pg_temp.did('pet', d.n) AND o.acquisition_type IS NULL;

UPDATE pet_ownerships w
SET acquisition_type = o.acquisition_type, adoption_application_id = o.adoption_application_id
FROM pets_current_owner o
WHERE w.pet_id = o.pet_id AND w.owner_id = o.owner_id AND w.acquisition_type IS NULL
  AND w.pet_id IN (SELECT pg_temp.did('pet', n) FROM demo_pet);

UPDATE adoption_applications SET completed_at = COALESCE(completed_at, decided_at, updated_at)
WHERE adoption_code LIKE 'A-D%' AND status = 'COMPLETED';

-- ── Matching: traits for every pet, adopter profiles for users 1–20 (21–30 have none) ──

INSERT INTO pet_traits (pet_id, friendly_people, friendly_children, fear_strangers, fear_noise, energy, good_with_dogs,
                        good_with_cats, alone_minutes, house_trained, bite_history, bite_verified, size, noise_level,
                        monthly_cost, care_level, source_role, note, updated_by, updated_at)
SELECT pg_temp.did('pet', d.n),
       3 + d.n % 3,
       CASE WHEN d.n % 4 = 0 THEN 2 ELSE 4 END,
       1 + d.n % 3,
       1 + (d.n + 1) % 3,
       1 + d.n % 5,
       CASE WHEN d.species = 'Dog' THEN 4 ELSE 2 + d.n % 3 END,
       CASE WHEN d.species = 'Cat' THEN 5 ELSE 2 + d.n % 3 END,
       (ARRAY[120, 240, 360, 480])[1 + d.n % 4],
       2 + d.n % 4,
       CASE WHEN d.n % 13 = 0 THEN 'MINOR' ELSE 'NONE' END,
       FALSE,
       CASE WHEN d.species = 'Cat' THEN 'SMALL' ELSE (ARRAY['SMALL','MEDIUM','LARGE'])[1 + d.n % 3] END,
       1 + d.n % 4,
       CASE WHEN d.species = 'Dog' THEN 600000 + (d.n % 4) * 150000 ELSE 400000 + (d.n % 3) * 100000 END,
       CASE WHEN d.health IN ('UNDER_TREATMENT', 'SPECIAL_NEEDS') THEN 'HIGH' WHEN d.health = 'RECOVERING' THEN 'MEDIUM' ELSE 'LOW' END,
       'STAFF',
       format('%s quan sát được sau %s ngày ở trạm.', d.name, 14 + d.n % 20),
       pg_temp.did('staff', d.org_n),
       now() - interval '20 days'
FROM demo_pet d
ON CONFLICT DO NOTHING;

-- Pet 13 suits a calm home with older children; pet 31 is a large dog.
UPDATE pet_traits SET friendly_children = 5, energy = 3, alone_minutes = 360, size = 'MEDIUM', good_with_cats = 4,
                      note = 'Hiền, quen trẻ lớn, ở nhà một mình được nửa ngày.'
WHERE pet_id = pg_temp.did('pet', 13);
UPDATE pet_traits SET size = 'LARGE', energy = 5, alone_minutes = 120 WHERE pet_id = pg_temp.did('pet', 31);

INSERT INTO adopter_profiles (user_id, housing_type, building_rule, has_children_under_5, has_children_5_to_8,
                              has_children_over_8, allergy_dog, allergy_cat, has_dog, has_cat, hours_away_per_day,
                              activity_level, monthly_budget, emergency_vet_budget, adjustment_weeks_expected,
                              intolerable_behaviors, return_circumstances, relocation_plan, experience_level,
                              willing_training, province_code, consent_at, created_at, updated_at)
SELECT pg_temp.did('user', n),
       (ARRAY['HOUSE','APARTMENT','HOUSE','APARTMENT','FARM'])[1 + n % 5],
       (ARRAY['ALLOWED','ALLOWED','SMALL_ONLY','ALLOWED','UNKNOWN'])[1 + n % 5],
       n % 6 = 0, n % 5 = 0, n % 4 = 0,
       FALSE, n % 9 = 0,
       n % 3 = 0, n % 4 = 1,
       (ARRAY[2, 4, 6, 8, 10])[1 + n % 5],
       1 + n % 5,
       (ARRAY[500000, 1000000, 1500000, 2000000])[1 + n % 4],
       (ARRAY[1000000, 2000000, 5000000])[1 + n % 3],
       (ARRAY[2, 4, 8])[1 + n % 3],
       CASE WHEN n % 2 = 0 THEN 'Cắn người' ELSE 'Cắn phá đồ đạc liên tục' END,
       'Chỉ khi cả nhà phải đi nước ngoài dài hạn.',
       'Mang bé theo khi chuyển nhà.',
       1 + n % 5,
       n % 3 <> 1,
       (SELECT province_code FROM demo_place WHERE idx = n % 24),
       now() - interval '30 days', now() - interval '30 days', now() - interval '5 days'
FROM generate_series(1, 20 * pg_temp.scale()) n
ON CONFLICT DO NOTHING;

-- user1: house in HCMC, works from home, experienced. user3 is allergic to dogs; user4's building allows small pets only.
UPDATE adopter_profiles SET housing_type = 'HOUSE', building_rule = 'ALLOWED', has_children_under_5 = FALSE,
                            has_children_5_to_8 = FALSE, has_children_over_8 = TRUE, allergy_dog = FALSE, allergy_cat = FALSE,
                            has_dog = FALSE, has_cat = TRUE, hours_away_per_day = 3, activity_level = 3,
                            monthly_budget = 2000000, emergency_vet_budget = 5000000, adjustment_weeks_expected = 8,
                            experience_level = 4, willing_training = TRUE, province_code = '79'
WHERE user_id = pg_temp.did('user', 1);
UPDATE adopter_profiles SET housing_type = 'HOUSE', building_rule = 'ALLOWED' WHERE user_id = pg_temp.did('user', 2);
UPDATE adopter_profiles SET allergy_dog = TRUE WHERE user_id = pg_temp.did('user', 3);
UPDATE adopter_profiles SET housing_type = 'APARTMENT', building_rule = 'SMALL_ONLY' WHERE user_id = pg_temp.did('user', 4);

-- ── Applications at organization 1. Their match is filled in by the backend's backfill job ──
-- (MatchBackfillWorker, a few seconds after start and every 5 minutes), with the same rules as on submit.

INSERT INTO adoption_applications (application_id, adoption_code, pet_id, applicant_id, organization_id, status, note,
                                   experience, live_condition, decided_at, decided_by, ready_at, completed_at,
                                   created_at, updated_at)
SELECT pg_temp.did('application', a.n), pg_temp.code('A-D', a.n), pg_temp.did('pet', a.pet_n),
       pg_temp.did('user', a.user_n), pg_temp.did('org', 1), a.status, a.note,
       'Đã nuôi chó mèo vài năm.', a.home,
       CASE WHEN a.status <> 'PENDING' THEN now() - a.age + interval '1 day' END,
       CASE WHEN a.status <> 'PENDING' THEN pg_temp.did('owner', 1) END,
       CASE WHEN a.status = 'APPROVED' THEN now() + interval '3 days' END,
       CASE WHEN a.status = 'COMPLETED' THEN a.done END,
       now() - a.age, now() - interval '1 hour'
FROM (VALUES
    (9001, 13, 1,  'PENDING',   interval '2 days',  NULL::timestamptz, 'Nhà mình có sân và một bé mèo đã quen chó. Mình làm việc ở nhà nên có thời gian dắt bé đi dạo.', 'Nhà phố có sân, con lớn 10 tuổi.'),
    (9002, 13, 2,  'PENDING',   interval '1 day',   NULL, 'Mình muốn nhận bé để làm bạn với con trai.', 'Căn hộ chung cư.'),
    (9003, 19, 3,  'PENDING',   interval '3 days',  NULL, 'Mình rất thích chó dù hơi dị ứng lông, sẽ uống thuốc.', 'Nhà phố.'),
    (9004, 31, 4,  'PENDING',   interval '4 days',  NULL, 'Mình muốn nuôi một bé chó lớn để giữ nhà.', 'Căn hộ chung cư, ban quản lý chỉ cho nuôi thú nhỏ.'),
    (9005, 37, 5,  'APPROVED',  interval '5 days',  NULL, 'Mình đã chuẩn bị chuồng và đồ ăn cho bé.', 'Nhà vườn.'),
    (9006, 43, 21, 'PENDING',   interval '6 hours', NULL, 'Mình mới tìm hiểu, muốn nhận bé về nuôi.', 'Nhà thuê.'),
    (9007, 1,  1,  'COMPLETED', interval '18 days', now() - interval '16 days', 'Mình đã sẵn sàng đón bé.', 'Nhà phố có sân.'),
    (9008, 49, 6,  'COMPLETED', interval '22 days', now() - interval '20 days', 'Gia đình mình rất mong có bé.', 'Căn hộ.'),
    (9009, 7,  7,  'COMPLETED', interval '45 days', now() - interval '40 days', 'Mình nhận bé làm bạn.', 'Nhà phố.')
) AS a(n, pet_n, user_n, status, age, done, note, home)
ON CONFLICT DO NOTHING;

-- Pets 1 and 49 went home with their adopters; pet 7 came back to the centre after a reclaim.
UPDATE pets SET status = 'ADOPTED' WHERE pet_id IN (pg_temp.did('pet', 1), pg_temp.did('pet', 49));
UPDATE pets_current_owner o SET owner_type = 'USER', owner_id = v.user_id, acquisition_type = 'ADOPTED',
                                adoption_application_id = v.app_id
FROM (VALUES (pg_temp.did('pet', 1), pg_temp.did('user', 1), pg_temp.did('application', 9007)),
             (pg_temp.did('pet', 49), pg_temp.did('user', 6), pg_temp.did('application', 9008))) AS v(pet_id, user_id, app_id)
WHERE o.pet_id = v.pet_id;
UPDATE pet_ownerships SET to_time = now() - interval '16 days'
WHERE pet_id = pg_temp.did('pet', 1) AND owner_type = 'ORGANIZATION' AND to_time IS NULL;
UPDATE pet_ownerships SET to_time = now() - interval '20 days'
WHERE pet_id = pg_temp.did('pet', 49) AND owner_type = 'ORGANIZATION' AND to_time IS NULL;
INSERT INTO pet_ownerships (pet_id, owner_type, owner_id, from_time, to_time, acquisition_type, adoption_application_id)
SELECT v.pet_id, 'USER', v.user_id, v.from_time, v.to_time, 'ADOPTED', v.app_id
FROM (VALUES (pg_temp.did('pet', 1), pg_temp.did('user', 1), now() - interval '16 days', NULL::timestamptz, pg_temp.did('application', 9007)),
             (pg_temp.did('pet', 49), pg_temp.did('user', 6), now() - interval '20 days', NULL, pg_temp.did('application', 9008)),
             (pg_temp.did('pet', 7), pg_temp.did('user', 7), now() - interval '40 days', now() - interval '10 days', pg_temp.did('application', 9009))
     ) AS v(pet_id, user_id, from_time, to_time, app_id)
WHERE NOT EXISTS (SELECT 1 FROM pet_ownerships w WHERE w.pet_id = v.pet_id AND w.owner_id = v.user_id);

-- ── After adoption: check-ins, alerts, the reclaim and its outcome ──

INSERT INTO adoption_followups (followup_id, application_id, pet_id, adopter_id, organization_id, milestone, status, due_at,
                                sent_at, responded_at, expires_at, answers, still_has_pet, severity, created_at)
SELECT pg_temp.did('followup', f.n), pg_temp.did('application', f.app_n), pg_temp.did('pet', f.pet_n),
       pg_temp.did('user', f.user_n), pg_temp.did('org', 1), f.milestone, f.status, f.due,
       CASE WHEN f.status IN ('SENT', 'RESPONDED') THEN f.due END,
       CASE WHEN f.status = 'RESPONDED' THEN f.due + interval '5 hours' END,
       CASE WHEN f.status = 'SENT' THEN now() + interval '6 days' END,
       f.answers::jsonb,
       CASE WHEN f.answers IS NOT NULL THEN TRUE END,
       f.severity,
       now() - interval '16 days'
FROM (VALUES
    -- user1 and pet 1: day 2 answered, all well; week 2 waiting for an answer in the app
    (9001, 9007, 1, 1, 'DAY_2',   'RESPONDED', now() - interval '14 days', '{"stillHasPet":true,"wellbeing":"NORMAL","fear":"SOME","incident":"NONE","support":"NONE","note":"Bé ăn ngủ tốt, hơi sợ tiếng xe máy."}', 'GREEN'),
    (9002, 9007, 1, 1, 'WEEK_2',  'SENT',      now() - interval '2 days',  NULL, NULL),
    (9003, 9007, 1, 1, 'MONTH_3', 'SCHEDULED', now() + interval '74 days', NULL, NULL),
    (9004, 9007, 1, 1, 'MONTH_6', 'SCHEDULED', now() + interval '164 days', NULL, NULL),
    -- user6 and pet 49: worried on day 2, thinking of giving the pet back at week 2
    (9005, 9008, 49, 6, 'DAY_2',  'RESPONDED', now() - interval '18 days', '{"stillHasPet":true,"wellbeing":"WORRIED","fear":"MUCH","incident":"NONE","support":"ADVICE","note":"Bé tru cả đêm, không chịu ăn."}', 'YELLOW'),
    (9006, 9008, 49, 6, 'WEEK_2', 'RESPONDED', now() - interval '6 days',  '{"stillHasPet":true,"wellbeing":"WORRIED","fear":"MUCH","incident":"NO_INJURY","support":"CONSIDERING_RETURN","note":"Bé gầm gừ với con nhỏ, nhà mình đang cân nhắc trả bé."}', 'RED'),
    (9007, 9008, 49, 6, 'MONTH_3','SCHEDULED', now() + interval '70 days', NULL, NULL),
    -- pet 7 was reclaimed: what was left is cancelled
    (9008, 9009, 7, 7, 'DAY_2',   'RESPONDED', now() - interval '38 days', '{"stillHasPet":true,"wellbeing":"ALARMING","fear":"MUCH","incident":"INJURY","support":"NONE","note":"Bé bị nhốt ngoài ban công."}', 'RED'),
    (9009, 9009, 7, 7, 'WEEK_2',  'CANCELLED', now() - interval '26 days', NULL, NULL)
) AS f(n, app_n, pet_n, user_n, milestone, status, due, answers, severity)
ON CONFLICT DO NOTHING;

INSERT INTO followup_alerts (alert_id, followup_id, application_id, organization_id, severity, reason_codes, status,
                             assigned_to, first_action_at, resolved_at, resolution, note, created_at)
VALUES
    (pg_temp.did('alert', 9001), pg_temp.did('followup', 9006), pg_temp.did('application', 9008), pg_temp.did('org', 1),
     'RED', 'CONSIDERING_RETURN,BEHAVIOR', 'OPEN', NULL, NULL, NULL, NULL, NULL, now() - interval '6 days'),
    (pg_temp.did('alert', 9002), pg_temp.did('followup', 9005), pg_temp.did('application', 9008), pg_temp.did('org', 1),
     'YELLOW', 'PET_HEALTH_WORRIED', 'IN_PROGRESS', pg_temp.did('staff', 1), now() - interval '17 days', NULL, NULL,
     'Đã gọi điện hướng dẫn cho bé ăn pate trộn cơm.', now() - interval '18 days'),
    (pg_temp.did('alert', 9003), pg_temp.did('followup', 9008), pg_temp.did('application', 9009), pg_temp.did('org', 1),
     'RED', 'BITE_INJURY,PET_HEALTH', 'RESOLVED', pg_temp.did('owner', 1), now() - interval '37 days', now() - interval '10 days',
     'RECLAIMED', 'Đã đến nhà kiểm tra và thu hồi bé.', now() - interval '38 days')
ON CONFLICT DO NOTHING;

INSERT INTO reclaim_logs (reclaim_id, application_id, pet_id, organization_id, user_id, reclaimed_by, reason, proof_id,
                          reputation_delta, reclaimed_at, created_at, created_by)
VALUES (pg_temp.did('reclaim', 9001), pg_temp.did('application', 9009), pg_temp.did('pet', 7), pg_temp.did('org', 1),
        pg_temp.did('user', 7), pg_temp.did('owner', 1),
        'Bé bị nhốt ngoài ban công và bị thương ở chân; người nhận nuôi không hợp tác sau hai lần nhắc.',
        (SELECT media_id FROM demo_media WHERE kind = 'case-dog' AND idx = 0), -300,
        now() - interval '10 days', now() - interval '10 days', pg_temp.did('owner', 1))
ON CONFLICT DO NOTHING;

INSERT INTO adoption_outcomes (outcome_id, application_id, pet_id, outcome_type, reason_code, initiated_by, occurred_at,
                               days_since_completed, note)
VALUES (pg_temp.did('outcome', 9001), pg_temp.did('application', 9009), pg_temp.did('pet', 7), 'RECLAIMED', 'OTHER',
        pg_temp.did('owner', 1), now() - interval '10 days', 30, 'Trạm thu hồi vì bé bị nhốt và bị thương.')
ON CONFLICT DO NOTHING;

INSERT INTO reputation_logs (log_id, user_id, delta, reason, reference_type, reference_id, created_at, created_by)
VALUES (pg_temp.did('reputation', 9001), pg_temp.did('user', 7), -300, 'RECLAIM', 'ADOPTION', pg_temp.did('application', 9009),
        now() - interval '10 days', pg_temp.did('owner', 1)),
       (pg_temp.did('reputation', 9002), pg_temp.did('user', 1), 50, 'RESCUE_COMPLETED', 'RESCUE_CASE', pg_temp.did('case', 19),
        now() - interval '30 days', NULL),
       (pg_temp.did('reputation', 9003), pg_temp.did('user', 1), 30, 'ADOPTION_COMPLETED', 'ADOPTION', pg_temp.did('application', 9007),
        now() - interval '16 days', NULL)
ON CONFLICT DO NOTHING;

-- ── Rescue: updates, a report waiting for the reporter, a disputed report, flags, AI triage ──

-- Case 30 was reported by user1; user12 is on it, posted progress and sent the result for user1 to confirm.
INSERT INTO rescue_case_updates (update_id, case_id, author_id, note, created_at)
VALUES (pg_temp.did('case-update', 9001), pg_temp.did('case', 30), pg_temp.did('user', 12),
        'Mình đã tới nơi, bé trốn dưới gầm xe, đang dụ bé ra bằng pate.', now() - interval '50 minutes'),
       (pg_temp.did('case-update', 9002), pg_temp.did('case', 30), pg_temp.did('user', 12),
        'Đã bế được bé, chân trước bị trầy. Đang chở bé tới phòng khám An Tâm.', now() - interval '30 minutes'),
       -- user1 is on case 49 and posted a first update
       (pg_temp.did('case-update', 9003), pg_temp.did('case', 49), pg_temp.did('user', 1),
        'Mình đang trên đường tới, khoảng 15 phút nữa.', now() - interval '40 minutes')
ON CONFLICT DO NOTHING;
INSERT INTO rescue_case_update_media (update_id, media_id, position)
SELECT pg_temp.did('case-update', 9002), media_id, 0 FROM demo_media WHERE kind = 'case-cat' AND idx = 2
ON CONFLICT DO NOTHING;

INSERT INTO rescue_case_completions (completion_id, case_id, rescued_at, rescue_note, location_note, submitted_by, outcome,
                                     status, created_at)
VALUES (pg_temp.did('completion', 9001), pg_temp.did('case', 30), now() - interval '20 minutes',
        'Bé đã được khám, chỉ trầy ngoài da. Mình giao bé cho Trạm Mái Ấm Sài Gòn chăm tiếp.', 'Phòng khám An Tâm',
        pg_temp.did('user', 12), 'HANDED_TO_ORG', 'PENDING', now() - interval '20 minutes')
ON CONFLICT DO NOTHING;
INSERT INTO rescue_completion_media (completion_id, media_id)
SELECT pg_temp.did('completion', 9001), media_id FROM demo_media WHERE kind = 'case-cat' AND idx = 2
ON CONFLICT DO NOTHING;

-- The reporter of case 8 says the rescue never happened: admins settle it.
UPDATE rescue_case_completions SET status = 'DISPUTED', escalated_at = now() - interval '1 day',
       reject_reason = 'Mình quay lại chỗ cũ vẫn thấy bé nằm đó, chưa ai đưa bé đi cả.'
WHERE completion_id = pg_temp.did('completion', 8) AND status = 'PENDING';

-- Case 79: user1 rescued the dog and kept it ("Thú của tôi").
UPDATE rescue_case_completions SET outcome = 'KEPT_BY_RESCUER' WHERE completion_id = pg_temp.did('completion', 79);

-- Flags: case 2 looks false to three people; user1 said case 1 is already handled.
INSERT INTO rescue_case_flags (case_id, user_id, reason, note, created_at)
VALUES (pg_temp.did('case', 2), pg_temp.did('user', 2), 'FALSE_REPORT', 'Mình ở gần đó, không thấy bé nào.', now() - interval '3 hours'),
       (pg_temp.did('case', 2), pg_temp.did('user', 3), 'FALSE_REPORT', NULL, now() - interval '2 hours'),
       (pg_temp.did('case', 2), pg_temp.did('user', 4), 'DUPLICATE', 'Trùng với một ca khác cùng hẻm.', now() - interval '1 hour'),
       (pg_temp.did('case', 1), pg_temp.did('user', 1), 'ALREADY_HANDLED', 'Chiều qua đã có người đưa bé đi.', now() - interval '5 hours')
ON CONFLICT DO NOTHING;

-- What the AI read from every open case: the queue and the case detail show it.
INSERT INTO rescue_case_triage (case_id, status, attempts, model, prompt_version, ai_urgency, needs_rescue, condition_summary,
                                species, province, missing_fields, latency_ms, analyzed_at, created_at, updated_at)
SELECT pg_temp.did('case', d.n), 'ANALYZED', 1, 'openrouter/google/gemini-2.5-flash-lite', 2,
       CASE WHEN d.n % 9 = 0 AND d.priority = 'NORMAL' THEN 'HIGH' ELSE d.priority END,
       TRUE,
       CASE d.priority
           WHEN 'CRITICAL' THEN 'Bé bị thương nặng, chảy máu hoặc khó thở, cần đến ngay.'
           WHEN 'HIGH' THEN 'Bé yếu, gầy hoặc bị thương ở chân, cần giúp trong ngày.'
           WHEN 'NORMAL' THEN 'Bé có vấn đề nhẹ về da hoặc đi lạc, cần người đưa đi khám.'
           ELSE 'Bé khỏe, cần tìm nơi nhận nuôi hoặc triệt sản.'
       END,
       d.species, p.province_name,
       CASE WHEN d.n % 6 = 0 THEN 'CONTACT_PHONE' END,
       900 + (d.n * 37) % 1200,
       pg_temp.ago(d.n, 80 * pg_temp.scale()) + interval '2 minutes',
       pg_temp.ago(d.n, 80 * pg_temp.scale()), now()
FROM demo_case d JOIN demo_place p ON p.idx = d.place_idx
WHERE d.status IN ('REPORTED', 'IN_PROGRESS')
ON CONFLICT DO NOTHING;

-- Organization 1 has taken a case near the centre (rescue queue → "Ca cứu hộ đang nhận").
INSERT INTO rescue_cases (case_id, case_code, reported_by, organization_id, species, color, size, priority, description,
                          location, location_text, province_code, province_name, ward_code, ward_name, status,
                          contact_phone, needs_help, reported_at, created_at, updated_at)
VALUES (pg_temp.did('case', 9001), pg_temp.code('R-D', 9001), pg_temp.did('user', 9), pg_temp.did('org', 1), 'Dog', 'Vàng',
        'MEDIUM', 'HIGH', 'Bé bị kẹt chân vào hàng rào sắt, kêu suốt từ sáng.',
        ST_SetSRID(ST_MakePoint(106.7150, 10.8040), 4326), 'Hẻm 120 Xô Viết Nghệ Tĩnh', '79', 'Thành phố Hồ Chí Minh',
        '26929', 'Phường Bình Thạnh', 'IN_PROGRESS', pg_temp.phone(9001), FALSE,
        now() - interval '3 hours', now() - interval '3 hours', now())
ON CONFLICT DO NOTHING;
INSERT INTO rescue_media (case_id, media_id)
SELECT pg_temp.did('case', 9001), media_id FROM demo_media WHERE kind = 'case-dog' AND idx = 3
ON CONFLICT DO NOTHING;
INSERT INTO rescue_case_claims (claim_id, case_id, user_id, organization_id, status, claimed_at, expires_at)
VALUES (pg_temp.did('claim', 9001), pg_temp.did('case', 9001), pg_temp.did('staff', 1), pg_temp.did('org', 1), 'ACTIVE',
        now() - interval '2 hours', NULL)
ON CONFLICT DO NOTHING;

-- ── Lost and found: user1's own cat is lost; user8 found a cat like it nearby ──

INSERT INTO pets (pet_id, pet_code, name, species_id, age, gender, color, weight, description, status, health_status,
                  is_vaccinated, is_neutered, visibility, created_at)
SELECT pg_temp.did('pet', 9001), pg_temp.code('P-D', 9001), 'Mực', s.species_id, 24, 'MALE', 'Đen tuyền', 4.2,
       'Mèo nhà mình nuôi từ nhỏ, có vòng cổ đỏ và đốm trắng ở ngực.', 'LOST', 'HEALTHY', TRUE, TRUE, 'PRIVATE',
       now() - interval '200 days'
FROM pet_species s WHERE s.name = 'Cat' AND s.is_deleted = FALSE
ON CONFLICT DO NOTHING;
-- the dog user1 rescued from case 79 and kept
INSERT INTO pets (pet_id, pet_code, name, species_id, age, gender, color, weight, description, status, health_status,
                  is_vaccinated, is_neutered, visibility, rescue_case_id, created_at)
SELECT pg_temp.did('pet', 9002), pg_temp.code('P-D', 9002), 'Gấu', s.species_id, 14, 'FEMALE', 'Nâu', 9.5,
       'Bé được mình cứu ở bãi giữ xe rồi giữ lại nuôi luôn.', 'ADOPTED', 'RECOVERING', TRUE, FALSE, 'PUBLIC',
       pg_temp.did('case', 79), now() - interval '30 days'
FROM pet_species s WHERE s.name = 'Dog' AND s.is_deleted = FALSE
ON CONFLICT DO NOTHING;
-- the breed of the photo each one wears
UPDATE pets p SET breed_id = (SELECT b.breed_id FROM pet_breeds b
                              WHERE b.species_id = p.species_id AND b.name = m.breed AND b.is_deleted = FALSE LIMIT 1)
FROM (VALUES (9001, 'pet-cat', 5), (9002, 'pet-dog', 9)) AS v(n, kind, idx)
JOIN demo_media m ON m.kind = v.kind AND m.idx = v.idx
WHERE p.pet_id = pg_temp.did('pet', v.n) AND p.breed_id IS NULL;
INSERT INTO pets_current_owner (pet_id, owner_type, owner_id, acquisition_type)
VALUES (pg_temp.did('pet', 9001), 'USER', pg_temp.did('user', 1), 'SELF_REGISTERED'),
       (pg_temp.did('pet', 9002), 'USER', pg_temp.did('user', 1), 'SELF_REGISTERED')
ON CONFLICT DO NOTHING;
INSERT INTO pet_ownerships (pet_id, owner_type, owner_id, from_time, acquisition_type)
SELECT v.pet_id, 'USER', pg_temp.did('user', 1), v.from_time, 'SELF_REGISTERED'
FROM (VALUES (pg_temp.did('pet', 9001), now() - interval '200 days'), (pg_temp.did('pet', 9002), now() - interval '30 days')) AS v(pet_id, from_time)
WHERE NOT EXISTS (SELECT 1 FROM pet_ownerships w WHERE w.pet_id = v.pet_id);
INSERT INTO pet_media (media_id, pet_id, media_file_id, type, is_primary, created_at)
SELECT pg_temp.did('pet-media', v.n), pg_temp.did('pet', v.n), m.media_id, 'image', TRUE, now() - interval '30 days'
FROM (VALUES (9001, 'pet-cat', 5), (9002, 'pet-dog', 9)) AS v(n, kind, idx)
JOIN demo_media m ON m.kind = v.kind AND m.idx = v.idx
ON CONFLICT DO NOTHING;

INSERT INTO rescue_cases (case_id, case_code, reported_by, pet_id, species, color, size, priority, description, location,
                          location_text, province_code, province_name, ward_code, ward_name, status, contact_phone,
                          needs_help, case_type, last_seen_at, holding_status, distinctive_marks, pet_status_before_lost,
                          reported_at, created_at, updated_at)
VALUES (pg_temp.did('case', 9002), pg_temp.code('R-D', 9002), pg_temp.did('user', 1), pg_temp.did('pet', 9001), 'Cat',
        'Đen tuyền', 'SMALL', 'NORMAL', 'Mực đi lạc từ tối hôm kia, sợ người lạ, có vòng cổ đỏ.',
        ST_SetSRID(ST_MakePoint(106.7080, 10.8060), 4326), 'Gần chợ Bà Chiểu', '79', 'Thành phố Hồ Chí Minh', '26929',
        'Phường Bình Thạnh', 'REPORTED', pg_temp.phone(1), FALSE, 'LOST', now() - interval '2 days', NULL,
        'Vòng cổ đỏ, đốm trắng ở ngực', 'ADOPTED', now() - interval '2 days', now() - interval '2 days', now()),
       (pg_temp.did('case', 9003), pg_temp.code('R-D', 9003), pg_temp.did('user', 8), NULL, 'Cat', 'Đen', 'SMALL',
        'NORMAL', 'Mình nhặt được một bé mèo đen đeo vòng cổ đỏ, rất sợ người, đang giữ ở nhà.',
        ST_SetSRID(ST_MakePoint(106.7150, 10.8120), 4326), 'Hẻm 45 Bạch Đằng', '79', 'Thành phố Hồ Chí Minh', '26929',
        'Phường Bình Thạnh', 'REPORTED', pg_temp.phone(8), FALSE, 'FOUND', NULL, 'AT_HOME', 'Vòng cổ đỏ', NULL,
        now() - interval '1 day', now() - interval '1 day', now())
ON CONFLICT DO NOTHING;
INSERT INTO rescue_media (case_id, media_id)
SELECT v.case_id, m.media_id
FROM (VALUES (pg_temp.did('case', 9002), 'pet-cat', 5), (pg_temp.did('case', 9003), 'case-cat', 4)) AS v(case_id, kind, idx)
JOIN demo_media m ON m.kind = v.kind AND m.idx = v.idx
ON CONFLICT DO NOTHING;
INSERT INTO case_match_suggestions (suggestion_id, lost_case_id, other_case_id, distance_m, score, reasons, status, created_at)
VALUES (pg_temp.did('match', 9001), pg_temp.did('case', 9002), pg_temp.did('case', 9003), 950, 82,
        E'Cùng loài mèo, màu đen\nCùng có vòng cổ đỏ\nCách nơi lạc khoảng 950 m\nTìm thấy một ngày sau khi lạc',
        'PENDING', now() - interval '20 hours')
ON CONFLICT DO NOTHING;

-- ── Notifications for the scenario accounts ──

INSERT INTO notifications (notification_id, user_id, type, title, body, data, created_at)
VALUES
    (pg_temp.did('notification', 9001), pg_temp.did('user', 1), 'RESCUE_COMPLETION_SUBMITTED',
     'Ca R-D0030 có báo cáo hoàn tất', 'Người cứu đã gửi kết quả, hãy xác nhận giúp.',
     jsonb_build_object('caseId', pg_temp.did('case', 30)), now() - interval '20 minutes'),
    (pg_temp.did('notification', 9002), pg_temp.did('user', 1), 'RESCUE_MATCH_SUGGESTED',
     'Có thể đã tìm thấy Mực', 'Một bé mèo giống Mực được báo tìm thấy cách khoảng 950 m.',
     jsonb_build_object('caseId', pg_temp.did('case', 9002), 'suggestionId', pg_temp.did('match', 9001)), now() - interval '20 hours'),
    (pg_temp.did('notification', 9003), pg_temp.did('user', 1), 'FOLLOWUP_DUE',
     'Bé Lu thế nào sau hai tuần?', 'Trạm Mái Ấm Sài Gòn muốn hỏi thăm bé.',
     jsonb_build_object('followupId', pg_temp.did('followup', 9002)), now() - interval '2 days'),
    (pg_temp.did('notification', 9004), pg_temp.did('user', 1), 'RESCUE_CASE_UPDATED',
     'Ca R-D0030 có cập nhật', 'Người cứu đã bế được bé và đang chở đi khám.',
     jsonb_build_object('caseId', pg_temp.did('case', 30)), now() - interval '30 minutes'),
    (pg_temp.did('notification', 9005), pg_temp.did('owner', 1), 'ADOPTION_SUBMITTED',
     'Đơn nhận nuôi mới cho bé', 'Có 5 đơn đang chờ trạm duyệt.',
     jsonb_build_object('applicationId', pg_temp.did('application', 9001)), now() - interval '2 days'),
    (pg_temp.did('notification', 9006), pg_temp.did('owner', 1), 'FOLLOWUP_ALERT',
     'Việc đỏ sau nhận nuôi', 'Một gia đình đang cân nhắc trả bé.',
     jsonb_build_object('alertId', pg_temp.did('alert', 9001)), now() - interval '6 days'),
    (pg_temp.did('notification', 9007), pg_temp.did('admin', 1), 'RESCUE_COMPLETION_NEEDS_REVIEW',
     'Báo cáo hoàn tất bị khiếu nại', 'Người báo ca R-D0008 không đồng ý với kết quả.',
     jsonb_build_object('caseId', pg_temp.did('case', 8)), now() - interval '1 day')
ON CONFLICT DO NOTHING;

-- ── Banners on the public pages ──

INSERT INTO banners (banner_id, title, subtitle, media_id, link_url, link_type, display_order, start_date, end_date,
                     is_active, target_page, created_at, created_by)
SELECT pg_temp.did('banner', v.n), v.title, v.subtitle, m.media_id, v.link, 'INTERNAL', v.n, now() - interval '30 days',
       now() + interval '60 days', TRUE, v.target, now() - interval '30 days', pg_temp.did('admin', 1)
FROM (VALUES
    (1, 'HOME', 'Thấy một bé cần giúp? Báo ngay', 'Ghim vị trí, người gần nhất sẽ thấy ca của bạn.', '/ban-do', 'case-dog', 1),
    (2, 'ADOPTION', 'Hơn 30 bé đang chờ một mái nhà', 'Điền hồ sơ để biết bé nào hợp với bạn nhất.', '/nhan-nuoi', 'pet-dog', 2),
    (3, 'RESCUE', 'Cùng cứu hộ trong khu của bạn', 'Nhận một ca gần nhà và cập nhật tiến độ cho người báo.', '/ban-do', 'case-cat', 3)
) AS v(n, target, title, subtitle, link, kind, idx)
JOIN demo_media m ON m.kind = v.kind AND m.idx = v.idx
ON CONFLICT DO NOTHING;

COMMIT;

-- Run this to see what is there.
SELECT 'users' AS what, COUNT(*) FROM users WHERE user_code LIKE 'U-D%'
UNION ALL SELECT 'organizations', COUNT(*) FROM organizations WHERE organization_code LIKE 'O-D%'
UNION ALL SELECT 'pets', COUNT(*) FROM pets WHERE pet_code LIKE 'P-D%'
UNION ALL SELECT 'rescue_cases', COUNT(*) FROM rescue_cases WHERE case_code LIKE 'R-D%'
UNION ALL SELECT 'adoptions', COUNT(*) FROM adoption_applications WHERE adoption_code LIKE 'A-D%'
UNION ALL SELECT 'medical_records', COUNT(*) FROM pet_medical_records r JOIN pets p ON p.pet_id = r.pet_id WHERE p.pet_code LIKE 'P-D%'
UNION ALL SELECT 'adopter_profiles', COUNT(*) FROM adopter_profiles a JOIN users u ON u.user_id = a.user_id WHERE u.user_code LIKE 'U-D%'
UNION ALL SELECT 'pet_traits', COUNT(*) FROM pet_traits t JOIN pets p ON p.pet_id = t.pet_id WHERE p.pet_code LIKE 'P-D%'
UNION ALL SELECT 'followups', COUNT(*) FROM adoption_followups f JOIN adoption_applications a ON a.application_id = f.application_id WHERE a.adoption_code LIKE 'A-D%';
