-- ============================================================
-- V33: one stored form for every catalog value and enum
--
-- The database keeps one canonical value per thing: English catalog names ("Dog"), upper-case
-- enum constants ("MALE"), slug tag codes ("lost-pet"). Spellings such as "dog", "DOG", "Chó"
-- or "cho" are mapped on the way in by the API (EnumAliases, SpeciesResolver, CanonicalText);
-- this migration cleans what is already stored and makes the database refuse duplicates that
-- only differ in case, accents or punctuation.
--
-- canonical_key() below must give the same result as CanonicalText.key() in Java.
-- ============================================================

-- ── Key function ─────────────────────────────────────────────
-- Lower case, Vietnamese accents removed, "đ" -> "d", every other run of characters -> "_".
CREATE OR REPLACE FUNCTION canonical_key(value TEXT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$
SELECT NULLIF(
    btrim(
        regexp_replace(
            lower(translate(
                regexp_replace(value, '[̀-ͯ]', '', 'g'),
                'àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴđĐ',
                'aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyAAAAAAAAAAAAAAAAAEEEEEEEEEEEIIIIIOOOOOOOOOOOOOOOOOUUUUUUUUUUUYYYYYdD')),
            '[^a-z0-9]+', '_', 'g'),
        '_'),
    '');
$$;

-- ── Species: extra spellings, kept as data so they can grow without a release ──

CREATE TABLE IF NOT EXISTS pet_species_aliases (
    alias_key  VARCHAR(100) PRIMARY KEY,
    species_id UUID NOT NULL REFERENCES pet_species (species_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_pet_species_aliases_key CHECK (alias_key = canonical_key(alias_key))
);

-- Names and Vietnamese labels already match on their own; these are the other common spellings.
INSERT INTO pet_species_aliases (alias_key, species_id)
SELECT a.alias_key, s.species_id
FROM (VALUES
    ('dogs', 'Dog'), ('con_cho', 'Dog'), ('cho_canh', 'Dog'), ('cho_con', 'Dog'), ('cun', 'Dog'),
    ('cun_con', 'Dog'), ('puppy', 'Dog'),
    ('cats', 'Cat'), ('con_meo', 'Cat'), ('meo_canh', 'Cat'), ('meo_con', 'Cat'), ('miu', 'Cat'),
    ('kitten', 'Cat'),
    ('con_tho', 'Rabbit'), ('bunny', 'Rabbit'),
    ('bo_u', 'Guinea Pig'),
    ('birds', 'Bird'), ('con_chim', 'Bird'),
    ('con_ca', 'Fish'),
    ('tortoise', 'Turtle'), ('con_rua', 'Turtle'),
    ('others', 'Other'), ('dong_vat_khac', 'Other')
) AS a (alias_key, species_name)
JOIN pet_species s ON s.name = a.species_name
ON CONFLICT (alias_key) DO NOTHING;

-- ── Species and breeds: no two names that only differ in case or accents ──

-- V23 merged species by exact name and no endpoint creates species, so a clash here means data
-- was added by hand. Stop instead of guessing which row is right.
DO $$
DECLARE clashes TEXT;
BEGIN
    SELECT string_agg(k || ' (' || names || ')', '; ') INTO clashes
    FROM (SELECT canonical_key(name) AS k, string_agg(name, ', ') AS names
          FROM pet_species WHERE is_deleted = FALSE
          GROUP BY 1 HAVING COUNT(*) > 1) d;
    IF clashes IS NOT NULL THEN
        RAISE EXCEPTION 'pet_species has names that only differ in spelling: %. Merge them, then rerun.', clashes;
    END IF;
END $$;

-- Breeds of one species that only differ in spelling ("Shih Tzu" / "Shih-Tzu"): keep one, move pets to it.
CREATE TEMP TABLE breed_merge ON COMMIT DROP AS
SELECT breed_id AS old_id,
       first_value(breed_id) OVER (
           PARTITION BY species_id, canonical_key(name)
           ORDER BY is_deleted ASC, (name_vi IS NOT NULL) DESC, created_at ASC, breed_id
       ) AS keeper_id
FROM pet_breeds
WHERE canonical_key(name) IS NOT NULL;

UPDATE pets p SET breed_id = m.keeper_id
FROM breed_merge m
WHERE p.breed_id = m.old_id AND m.old_id <> m.keeper_id;

DELETE FROM pet_breeds b
USING breed_merge m
WHERE b.breed_id = m.old_id AND m.old_id <> m.keeper_id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pet_species_name_key
    ON pet_species (canonical_key(name)) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_pet_breeds_species_name_key
    ON pet_breeds (species_id, canonical_key(name)) WHERE is_deleted = FALSE;

-- ── Tags: the code is a lower-case slug, one per meaning ──

-- Spelling duplicates: the oldest active tag stays, the others are retired.
WITH ranked AS (
    SELECT tag_id,
           row_number() OVER (PARTITION BY canonical_key(code) ORDER BY is_deleted, created_at, tag_id) AS rn
    FROM tags
    WHERE canonical_key(code) IS NOT NULL
)
UPDATE tags t SET is_deleted = TRUE, deleted_at = now()
FROM ranked r
WHERE t.tag_id = r.tag_id AND r.rn > 1 AND t.is_deleted = FALSE;

-- Retired rows give up their code so an active tag can take the clean form of it.
UPDATE tags SET code = 'retired-' || tag_id
WHERE is_deleted = TRUE AND code IS NOT NULL AND code NOT LIKE 'retired-%';

UPDATE tags SET code = replace(canonical_key(code), '_', '-')
WHERE is_deleted = FALSE AND canonical_key(code) IS NOT NULL
  AND code <> replace(canonical_key(code), '_', '-');

CREATE UNIQUE INDEX IF NOT EXISTS uq_tags_code_key
    ON tags (canonical_key(code)) WHERE is_deleted = FALSE;

ALTER TABLE tags DROP CONSTRAINT IF EXISTS ck_tags_code_slug;
ALTER TABLE tags ADD CONSTRAINT ck_tags_code_slug
    CHECK (is_deleted OR code ~ '^[a-z0-9]+(-[a-z0-9]+)*$') NOT VALID;

-- The tags the web and the post analysis use.
INSERT INTO tags (tag_id, code, name, description)
SELECT gen_random_uuid(), t.code, t.name, t.description
FROM (VALUES
    ('rescue', 'Cứu hộ', 'Một con vật đang cần được cứu'),
    ('adoption', 'Nhận nuôi', 'Tìm gia đình cho thú cưng'),
    ('lost-pet', 'Thú đi lạc', 'Chủ đang tìm thú cưng bị lạc'),
    ('found-pet', 'Nhặt được thú', 'Ai đó nhặt được thú cưng và đang tìm chủ'),
    ('care', 'Chăm sóc', 'Kinh nghiệm chăm sóc và sức khỏe'),
    ('volunteer', 'Tình nguyện', 'Kêu gọi tình nguyện viên'),
    ('donation', 'Quyên góp', 'Kêu gọi ủng hộ cho trạm cứu hộ')
) AS t (code, name, description)
WHERE NOT EXISTS (SELECT 1 FROM tags x WHERE x.is_deleted = FALSE AND canonical_key(x.code) = canonical_key(t.code));

-- ── Rescue cases: species is a catalog name ──

-- "chó", "DOG", "cún" -> "Dog"; anything the catalog does not know -> "Other".
WITH spelling AS (
    SELECT canonical_key(name) AS k, name FROM pet_species WHERE is_deleted = FALSE
    UNION
    SELECT canonical_key(name_vi), name FROM pet_species WHERE is_deleted = FALSE AND name_vi IS NOT NULL
    UNION
    SELECT a.alias_key, s.name FROM pet_species_aliases a JOIN pet_species s ON s.species_id = a.species_id
)
UPDATE rescue_cases rc
SET species = COALESCE((SELECT sp.name FROM spelling sp WHERE sp.k = canonical_key(rc.species) LIMIT 1), 'Other')
WHERE rc.species IS NOT NULL
  AND rc.species IS DISTINCT FROM (SELECT sp.name FROM spelling sp WHERE sp.k = canonical_key(rc.species) LIMIT 1);

UPDATE rescue_cases SET species = NULL WHERE species IS NOT NULL AND canonical_key(species) IS NULL;

-- V23 swapped the plain unique constraint for one on LOWER(name); a foreign key needs the plain one.
-- It cannot fail: LOWER(name) is already unique.
ALTER TABLE pet_species DROP CONSTRAINT IF EXISTS uq_pet_species_name;
ALTER TABLE pet_species ADD CONSTRAINT uq_pet_species_name UNIQUE (name);

ALTER TABLE rescue_cases DROP CONSTRAINT IF EXISTS fk_rescue_cases_species;
ALTER TABLE rescue_cases ADD CONSTRAINT fk_rescue_cases_species
    FOREIGN KEY (species) REFERENCES pet_species (name) ON UPDATE CASCADE;

-- ── Enum columns: upper-case constant names only ──

-- Same spelling, different case or spaces: "male ", "Male" -> "MALE".
UPDATE users SET gender = upper(btrim(gender)) WHERE gender IS DISTINCT FROM upper(btrim(gender));
UPDATE users SET gender = CASE canonical_key(gender)
        WHEN 'nam' THEN 'MALE' WHEN 'nu' THEN 'FEMALE'
        WHEN 'unknown' THEN 'OTHER' WHEN 'khac' THEN 'OTHER' END
WHERE canonical_key(gender) IN ('nam', 'nu', 'unknown', 'khac');
UPDATE users SET gender = NULL WHERE gender = '';

UPDATE pets SET gender = upper(btrim(gender)) WHERE gender IS DISTINCT FROM upper(btrim(gender));
UPDATE pets SET gender = CASE
        WHEN canonical_key(gender) IN ('duc', 'con_duc', 'm') THEN 'MALE'
        WHEN canonical_key(gender) IN ('cai', 'con_cai', 'f') THEN 'FEMALE'
        ELSE 'UNKNOWN' END
WHERE gender IS NOT NULL AND gender NOT IN ('MALE', 'FEMALE', 'UNKNOWN');

UPDATE rescue_cases SET size = CASE
        WHEN canonical_key(size) IN ('small', 's', 'nho') THEN 'SMALL'
        WHEN canonical_key(size) IN ('medium', 'm', 'vua', 'trung_binh') THEN 'MEDIUM'
        WHEN canonical_key(size) IN ('large', 'l', 'lon', 'to') THEN 'LARGE'
        ELSE NULL END
WHERE size IS NOT NULL AND size NOT IN ('SMALL', 'MEDIUM', 'LARGE');

UPDATE rescue_cases SET priority = upper(btrim(priority)) WHERE priority IS DISTINCT FROM upper(btrim(priority));
UPDATE rescue_cases SET status = upper(btrim(status)) WHERE status IS DISTINCT FROM upper(btrim(status));
UPDATE organizations SET type = upper(btrim(type)) WHERE type IS DISTINCT FROM upper(btrim(type));
UPDATE organizations SET status = upper(btrim(status)) WHERE status IS DISTINCT FROM upper(btrim(status));
UPDATE organization_members SET role = upper(btrim(role)) WHERE role IS DISTINCT FROM upper(btrim(role));
UPDATE organization_members SET status = upper(btrim(status)) WHERE status IS DISTINCT FROM upper(btrim(status));

-- New rows must hold a known constant. NOT VALID: a value older code wrote and nobody can map
-- safely stays readable until someone fixes it, but nothing new like it can be written.
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT * FROM (VALUES
        ('users',                   'gender',        'ck_users_gender',              'MALE,FEMALE,OTHER'),
        ('users',                   'status',        'ck_users_status',              'PENDING_VERIFICATION,ACTIVE,LOCKED,INACTIVE,BANNED'),
        ('pets',                    'gender',        'ck_pets_gender',               'MALE,FEMALE,UNKNOWN'),
        ('pets',                    'status',        'ck_pets_status',               'FOSTERING,AVAILABLE,ADOPTED,LOST,DECEASED'),
        ('pets',                    'health_status', 'ck_pets_health_status',        'HEALTHY,UNDER_TREATMENT,RECOVERING,SPECIAL_NEEDS'),
        ('rescue_cases',            'size',          'ck_rescue_cases_size',         'SMALL,MEDIUM,LARGE'),
        ('rescue_cases',            'priority',      'ck_rescue_cases_priority',     'CRITICAL,HIGH,NORMAL,LOW'),
        ('rescue_cases',            'status',        'ck_rescue_cases_status',       'REPORTED,IN_PROGRESS,RESCUED,CLOSED'),
        ('organizations',           'type',          'ck_organizations_type',        'SHELTER,VET_CENTER'),
        ('organizations',           'status',        'ck_organizations_status',      'PENDING,ACTIVE,INACTIVE,REJECTED'),
        ('organization_members',    'role',          'ck_org_members_role',          'OWNER,STAFF,MEMBER,VET'),
        ('organization_members',    'status',        'ck_org_members_status',        'ACTIVE,INACTIVE'),
        ('rescue_case_claims',      'status',        'ck_rescue_claims_status',      'ACTIVE,RELEASED,EXPIRED,COMPLETED'),
        ('rescue_case_completions', 'status',        'ck_rescue_completions_status', 'PENDING,APPROVED,REJECTED,DISPUTED'),
        ('rescue_case_completions', 'outcome',       'ck_rescue_completions_outcome','HANDED_TO_ORG,TEMP_HOLDING,KEPT_BY_RESCUER,RELEASED,DECEASED'),
        ('adoption_applications',   'status',        'ck_adoptions_status',          'PENDING,APPROVED,COMPLETED,CANCELED,REJECTED'),
        ('pet_medical_records',     'record_type',   'ck_medical_records_type',      'VACCINATION,TREATMENT,SURGERY'),
        ('pets_current_owner',      'owner_type',    'ck_pets_current_owner_type',   'USER,ORGANIZATION'),
        ('pet_ownerships',          'owner_type',    'ck_pet_ownerships_type',       'USER,ORGANIZATION'),
        ('friend_requests',         'status',        'ck_friend_requests_status',    'PENDING,ACCEPTED,REJECTED')
    ) AS v (tbl, col, name, allowed)
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I', c.tbl, c.name);
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I CHECK (%I IS NULL OR %I = ANY (%L::text[])) NOT VALID',
                       c.tbl, c.name, c.col, c.col, string_to_array(c.allowed, ','));
    END LOOP;
END $$;
