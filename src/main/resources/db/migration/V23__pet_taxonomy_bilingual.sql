-- ============================================================
-- V23: Bilingual pet taxonomy + duplicate cleanup
--
-- * `name`    (English) is the canonical value / source of truth.
-- * `name_vi` (Vietnamese) is the display label for the FE.
-- * Duplicate species created from free text (e.g. "cat" vs "Mèo") are merged
--   into one canonical row; pets and breeds are re-pointed before deletion.
--
-- Breed seed sources (crawled 2026-10-15, cleaned + translated by hand):
--   dogs: https://dog.ceo/api/breeds/list/all  (Stanford Dogs dataset)
--   cats: https://catfact.ninja/breeds          (Wikipedia list of cat breeds)
--   plus Vietnamese native breeds (Phú Quốc, H'Mông cộc đuôi, Bắc Hà, ...)
-- ============================================================

ALTER TABLE pet_species ADD COLUMN IF NOT EXISTS name_vi VARCHAR(100);
ALTER TABLE pet_breeds  ADD COLUMN IF NOT EXISTS name_vi VARCHAR(100);

-- ── Seed data ────────────────────────────────────────────────

CREATE TEMP TABLE seed_species (name VARCHAR(100), name_vi VARCHAR(100)) ON COMMIT DROP;
INSERT INTO seed_species (name, name_vi) VALUES
    ('Dog', 'Chó'),
    ('Cat', 'Mèo'),
    ('Rabbit', 'Thỏ'),
    ('Hamster', 'Chuột Hamster'),
    ('Guinea Pig', 'Chuột lang'),
    ('Bird', 'Chim'),
    ('Fish', 'Cá'),
    ('Turtle', 'Rùa'),
    ('Other', 'Khác');

-- Free-text spellings that must collapse into a canonical species (lower-case).
CREATE TEMP TABLE seed_species_alias (alias VARCHAR(100), name VARCHAR(100)) ON COMMIT DROP;
INSERT INTO seed_species_alias (alias, name) VALUES
    ('dog', 'Dog'), ('dogs', 'Dog'), ('chó', 'Dog'), ('cho', 'Dog'), ('con chó', 'Dog'),
    ('chó cảnh', 'Dog'), ('cún', 'Dog'), ('puppy', 'Dog'),
    ('cat', 'Cat'), ('cats', 'Cat'), ('mèo', 'Cat'), ('meo', 'Cat'), ('con mèo', 'Cat'),
    ('mèo cảnh', 'Cat'), ('kitten', 'Cat'),
    ('rabbit', 'Rabbit'), ('thỏ', 'Rabbit'), ('tho', 'Rabbit'), ('con thỏ', 'Rabbit'),
    ('hamster', 'Hamster'), ('chuột hamster', 'Hamster'), ('chuot hamster', 'Hamster'),
    ('guinea pig', 'Guinea Pig'), ('chuột lang', 'Guinea Pig'), ('bọ ú', 'Guinea Pig'),
    ('bird', 'Bird'), ('birds', 'Bird'), ('chim', 'Bird'),
    ('fish', 'Fish'), ('cá', 'Fish'),
    ('turtle', 'Turtle'), ('tortoise', 'Turtle'), ('rùa', 'Turtle'), ('rua', 'Turtle'),
    ('other', 'Other'), ('others', 'Other'), ('khác', 'Other'), ('khac', 'Other');

CREATE TEMP TABLE seed_breeds (species VARCHAR(100), name VARCHAR(100), name_vi VARCHAR(100)) ON COMMIT DROP;
INSERT INTO seed_breeds (species, name, name_vi) VALUES
    -- Dogs
    ('Dog', 'Mixed Breed', 'Chó lai'),
    ('Dog', 'Vietnamese Native Dog', 'Chó ta'),
    ('Dog', 'Phu Quoc Ridgeback', 'Chó Phú Quốc'),
    ('Dog', 'H''Mong Bobtail Dog', 'Chó H''Mông cộc đuôi'),
    ('Dog', 'Bac Ha Dog', 'Chó Bắc Hà'),
    ('Dog', 'Lai Dog', 'Chó Lài'),
    ('Dog', 'Affenpinscher', 'Chó Affenpinscher'),
    ('Dog', 'Afghan Hound', 'Chó săn Afghan'),
    ('Dog', 'Airedale Terrier', 'Chó Airedale Terrier'),
    ('Dog', 'Akita', 'Chó Akita'),
    ('Dog', 'Alaskan Malamute', 'Chó Alaska'),
    ('Dog', 'American Staffordshire Terrier', 'Chó American Staffordshire Terrier'),
    ('Dog', 'Australian Cattle Dog', 'Chó chăn gia súc Úc'),
    ('Dog', 'Australian Kelpie', 'Chó Kelpie Úc'),
    ('Dog', 'Australian Shepherd', 'Chó chăn cừu Úc'),
    ('Dog', 'Australian Terrier', 'Chó sục Úc'),
    ('Dog', 'Basenji', 'Chó Basenji'),
    ('Dog', 'Basset Hound', 'Chó Basset Hound'),
    ('Dog', 'Beagle', 'Chó Beagle'),
    ('Dog', 'Bedlington Terrier', 'Chó Bedlington Terrier'),
    ('Dog', 'Belgian Malinois', 'Chó Becgie Bỉ Malinois'),
    ('Dog', 'Belgian Groenendael', 'Chó Becgie Bỉ Groenendael'),
    ('Dog', 'Belgian Tervuren', 'Chó Becgie Bỉ Tervuren'),
    ('Dog', 'Bernese Mountain Dog', 'Chó núi Bernese'),
    ('Dog', 'Bichon Frise', 'Chó Bichon Frise'),
    ('Dog', 'Bloodhound', 'Chó Bloodhound'),
    ('Dog', 'Bluetick Coonhound', 'Chó Bluetick Coonhound'),
    ('Dog', 'Border Collie', 'Chó Border Collie'),
    ('Dog', 'Border Terrier', 'Chó Border Terrier'),
    ('Dog', 'Borzoi', 'Chó Borzoi'),
    ('Dog', 'Boston Terrier', 'Chó Boston Terrier'),
    ('Dog', 'Bouvier des Flandres', 'Chó Bouvier des Flandres'),
    ('Dog', 'Boxer', 'Chó Boxer'),
    ('Dog', 'Briard', 'Chó Briard'),
    ('Dog', 'Brittany', 'Chó Brittany'),
    ('Dog', 'Bullmastiff', 'Chó Bullmastiff'),
    ('Dog', 'Cairn Terrier', 'Chó Cairn Terrier'),
    ('Dog', 'Cane Corso', 'Chó Cane Corso'),
    ('Dog', 'Cardigan Welsh Corgi', 'Chó Corgi Cardigan'),
    ('Dog', 'Caucasian Shepherd Dog', 'Chó chăn cừu Kavkaz'),
    ('Dog', 'Cavalier King Charles Spaniel', 'Chó Cavalier King Charles Spaniel'),
    ('Dog', 'Cavapoo', 'Chó Cavapoo'),
    ('Dog', 'Chesapeake Bay Retriever', 'Chó Chesapeake Bay Retriever'),
    ('Dog', 'Chihuahua', 'Chó Chihuahua'),
    ('Dog', 'Chow Chow', 'Chó Chow Chow'),
    ('Dog', 'Clumber Spaniel', 'Chó Clumber Spaniel'),
    ('Dog', 'Cockapoo', 'Chó Cockapoo'),
    ('Dog', 'Cocker Spaniel', 'Chó Cocker Spaniel'),
    ('Dog', 'Coton de Tulear', 'Chó Coton de Tulear'),
    ('Dog', 'Curly-Coated Retriever', 'Chó Curly-Coated Retriever'),
    ('Dog', 'Dachshund', 'Chó lạp xưởng (Dachshund)'),
    ('Dog', 'Dalmatian', 'Chó đốm (Dalmatian)'),
    ('Dog', 'Dandie Dinmont Terrier', 'Chó Dandie Dinmont Terrier'),
    ('Dog', 'Doberman Pinscher', 'Chó Doberman'),
    ('Dog', 'English Bulldog', 'Chó Bulldog Anh'),
    ('Dog', 'English Foxhound', 'Chó săn cáo Anh'),
    ('Dog', 'English Mastiff', 'Chó Mastiff Anh'),
    ('Dog', 'English Setter', 'Chó English Setter'),
    ('Dog', 'English Springer Spaniel', 'Chó English Springer Spaniel'),
    ('Dog', 'Entlebucher Mountain Dog', 'Chó núi Entlebucher'),
    ('Dog', 'Finnish Lapphund', 'Chó Lapphund Phần Lan'),
    ('Dog', 'Flat-Coated Retriever', 'Chó Flat-Coated Retriever'),
    ('Dog', 'French Bulldog', 'Chó Bulldog Pháp'),
    ('Dog', 'German Shepherd', 'Chó Becgie Đức'),
    ('Dog', 'German Shorthaired Pointer', 'Chó Pointer Đức lông ngắn'),
    ('Dog', 'German Longhaired Pointer', 'Chó Pointer Đức lông dài'),
    ('Dog', 'Giant Schnauzer', 'Chó Schnauzer khổng lồ'),
    ('Dog', 'Golden Retriever', 'Chó Golden Retriever'),
    ('Dog', 'Gordon Setter', 'Chó Gordon Setter'),
    ('Dog', 'Great Dane', 'Chó Great Dane'),
    ('Dog', 'Great Pyrenees', 'Chó Great Pyrenees'),
    ('Dog', 'Greyhound', 'Chó Greyhound'),
    ('Dog', 'Havanese', 'Chó Havanese'),
    ('Dog', 'Ibizan Hound', 'Chó Ibizan Hound'),
    ('Dog', 'Irish Setter', 'Chó Irish Setter'),
    ('Dog', 'Irish Terrier', 'Chó Irish Terrier'),
    ('Dog', 'Irish Water Spaniel', 'Chó Irish Water Spaniel'),
    ('Dog', 'Irish Wolfhound', 'Chó săn sói Ireland'),
    ('Dog', 'Italian Greyhound', 'Chó Greyhound Ý'),
    ('Dog', 'Jack Russell Terrier', 'Chó Jack Russell Terrier'),
    ('Dog', 'Japanese Chin', 'Chó Nhật (Japanese Chin)'),
    ('Dog', 'Japanese Spitz', 'Chó Spitz Nhật'),
    ('Dog', 'Keeshond', 'Chó Keeshond'),
    ('Dog', 'Kerry Blue Terrier', 'Chó Kerry Blue Terrier'),
    ('Dog', 'Komondor', 'Chó Komondor'),
    ('Dog', 'Kuvasz', 'Chó Kuvasz'),
    ('Dog', 'Labradoodle', 'Chó Labradoodle'),
    ('Dog', 'Labrador Retriever', 'Chó Labrador Retriever'),
    ('Dog', 'Lakeland Terrier', 'Chó Lakeland Terrier'),
    ('Dog', 'Leonberger', 'Chó Leonberger'),
    ('Dog', 'Lhasa Apso', 'Chó Lhasa Apso'),
    ('Dog', 'Maltese', 'Chó Maltese'),
    ('Dog', 'Miniature Pinscher', 'Chó Phốc hươu (Miniature Pinscher)'),
    ('Dog', 'Miniature Schnauzer', 'Chó Schnauzer mini'),
    ('Dog', 'Newfoundland', 'Chó Newfoundland'),
    ('Dog', 'Norfolk Terrier', 'Chó Norfolk Terrier'),
    ('Dog', 'Norwegian Buhund', 'Chó Buhund Na Uy'),
    ('Dog', 'Norwegian Elkhound', 'Chó săn nai sừng tấm Na Uy'),
    ('Dog', 'Norwich Terrier', 'Chó Norwich Terrier'),
    ('Dog', 'Old English Sheepdog', 'Chó chăn cừu Anh cổ'),
    ('Dog', 'Otterhound', 'Chó Otterhound'),
    ('Dog', 'Papillon', 'Chó Bướm (Papillon)'),
    ('Dog', 'Patterdale Terrier', 'Chó Patterdale Terrier'),
    ('Dog', 'Pekingese', 'Chó Bắc Kinh'),
    ('Dog', 'Pembroke Welsh Corgi', 'Chó Corgi Pembroke'),
    ('Dog', 'Pit Bull', 'Chó Pit Bull'),
    ('Dog', 'Plott Hound', 'Chó Plott Hound'),
    ('Dog', 'Pomeranian', 'Chó Phốc sóc (Pomeranian)'),
    ('Dog', 'Poodle', 'Chó Poodle'),
    ('Dog', 'Toy Poodle', 'Chó Poodle Toy'),
    ('Dog', 'Pug', 'Chó Pug'),
    ('Dog', 'Puggle', 'Chó Puggle'),
    ('Dog', 'Redbone Coonhound', 'Chó Redbone Coonhound'),
    ('Dog', 'Rhodesian Ridgeback', 'Chó Rhodesian Ridgeback'),
    ('Dog', 'Rottweiler', 'Chó Rottweiler'),
    ('Dog', 'Rough Collie', 'Chó Collie lông dài'),
    ('Dog', 'Saint Bernard', 'Chó Saint Bernard'),
    ('Dog', 'Saluki', 'Chó Saluki'),
    ('Dog', 'Samoyed', 'Chó Samoyed'),
    ('Dog', 'Schipperke', 'Chó Schipperke'),
    ('Dog', 'Scottish Deerhound', 'Chó săn hươu Scotland'),
    ('Dog', 'Scottish Terrier', 'Chó sục Scotland'),
    ('Dog', 'Sealyham Terrier', 'Chó Sealyham Terrier'),
    ('Dog', 'Shar Pei', 'Chó Shar Pei'),
    ('Dog', 'Shetland Sheepdog', 'Chó chăn cừu Shetland'),
    ('Dog', 'Shiba Inu', 'Chó Shiba Inu'),
    ('Dog', 'Shih Tzu', 'Chó Shih Tzu'),
    ('Dog', 'Siberian Husky', 'Chó Husky Sibir'),
    ('Dog', 'Silky Terrier', 'Chó Silky Terrier'),
    ('Dog', 'Soft Coated Wheaten Terrier', 'Chó Wheaten Terrier'),
    ('Dog', 'Spanish Water Dog', 'Chó nước Tây Ban Nha'),
    ('Dog', 'Staffordshire Bull Terrier', 'Chó Staffordshire Bull Terrier'),
    ('Dog', 'Sussex Spaniel', 'Chó Sussex Spaniel'),
    ('Dog', 'Tibetan Mastiff', 'Chó Ngao Tây Tạng'),
    ('Dog', 'Tibetan Terrier', 'Chó sục Tây Tạng'),
    ('Dog', 'Vizsla', 'Chó Vizsla'),
    ('Dog', 'Weimaraner', 'Chó Weimaraner'),
    ('Dog', 'Welsh Springer Spaniel', 'Chó Welsh Springer Spaniel'),
    ('Dog', 'Welsh Terrier', 'Chó sục Wales'),
    ('Dog', 'West Highland White Terrier', 'Chó West Highland White Terrier'),
    ('Dog', 'Whippet', 'Chó Whippet'),
    ('Dog', 'Xoloitzcuintli', 'Chó không lông Mexico'),
    ('Dog', 'Yorkshire Terrier', 'Chó Yorkshire Terrier'),
    -- Cats
    ('Cat', 'Mixed Breed', 'Mèo lai'),
    ('Cat', 'Domestic Shorthair', 'Mèo ta'),
    ('Cat', 'Abyssinian', 'Mèo Abyssinian'),
    ('Cat', 'American Bobtail', 'Mèo cộc đuôi Mỹ'),
    ('Cat', 'American Curl', 'Mèo tai xoăn Mỹ'),
    ('Cat', 'American Shorthair', 'Mèo Mỹ lông ngắn'),
    ('Cat', 'American Wirehair', 'Mèo Mỹ lông xoăn'),
    ('Cat', 'Australian Mist', 'Mèo Australian Mist'),
    ('Cat', 'Balinese', 'Mèo Bali'),
    ('Cat', 'Bengal', 'Mèo Bengal'),
    ('Cat', 'Birman', 'Mèo Birman'),
    ('Cat', 'Bombay', 'Mèo Bombay'),
    ('Cat', 'British Longhair', 'Mèo Anh lông dài'),
    ('Cat', 'British Shorthair', 'Mèo Anh lông ngắn'),
    ('Cat', 'Burmese', 'Mèo Miến Điện'),
    ('Cat', 'Burmilla', 'Mèo Burmilla'),
    ('Cat', 'Chartreux', 'Mèo Chartreux'),
    ('Cat', 'Chausie', 'Mèo Chausie'),
    ('Cat', 'Cornish Rex', 'Mèo Cornish Rex'),
    ('Cat', 'Cymric', 'Mèo Cymric'),
    ('Cat', 'Devon Rex', 'Mèo Devon Rex'),
    ('Cat', 'Donskoy', 'Mèo không lông Don'),
    ('Cat', 'Dragon Li', 'Mèo Li Hoa'),
    ('Cat', 'Egyptian Mau', 'Mèo Mau Ai Cập'),
    ('Cat', 'European Shorthair', 'Mèo châu Âu lông ngắn'),
    ('Cat', 'Exotic Shorthair', 'Mèo Exotic (Ba Tư lông ngắn)'),
    ('Cat', 'Havana Brown', 'Mèo Havana Brown'),
    ('Cat', 'Himalayan', 'Mèo Himalaya'),
    ('Cat', 'Japanese Bobtail', 'Mèo cộc đuôi Nhật Bản'),
    ('Cat', 'Javanese', 'Mèo Java'),
    ('Cat', 'Khao Manee', 'Mèo Khao Manee'),
    ('Cat', 'Korat', 'Mèo Korat'),
    ('Cat', 'Kurilian Bobtail', 'Mèo cộc đuôi Kuril'),
    ('Cat', 'LaPerm', 'Mèo LaPerm'),
    ('Cat', 'Lykoi', 'Mèo người sói (Lykoi)'),
    ('Cat', 'Maine Coon', 'Mèo Maine Coon'),
    ('Cat', 'Manx', 'Mèo Manx'),
    ('Cat', 'Munchkin', 'Mèo chân ngắn Munchkin'),
    ('Cat', 'Nebelung', 'Mèo Nebelung'),
    ('Cat', 'Norwegian Forest Cat', 'Mèo rừng Na Uy'),
    ('Cat', 'Ocicat', 'Mèo Ocicat'),
    ('Cat', 'Oriental Longhair', 'Mèo Phương Đông lông dài'),
    ('Cat', 'Oriental Shorthair', 'Mèo Phương Đông lông ngắn'),
    ('Cat', 'Persian', 'Mèo Ba Tư'),
    ('Cat', 'Peterbald', 'Mèo Peterbald'),
    ('Cat', 'Pixie-bob', 'Mèo Pixie-bob'),
    ('Cat', 'Ragamuffin', 'Mèo Ragamuffin'),
    ('Cat', 'Ragdoll', 'Mèo Ragdoll'),
    ('Cat', 'Russian Blue', 'Mèo Nga xanh'),
    ('Cat', 'Savannah', 'Mèo Savannah'),
    ('Cat', 'Scottish Fold', 'Mèo tai cụp Scotland'),
    ('Cat', 'Selkirk Rex', 'Mèo Selkirk Rex'),
    ('Cat', 'Siamese', 'Mèo Xiêm'),
    ('Cat', 'Siberian', 'Mèo Siberia'),
    ('Cat', 'Singapura', 'Mèo Singapura'),
    ('Cat', 'Snowshoe', 'Mèo Snowshoe'),
    ('Cat', 'Somali', 'Mèo Somali'),
    ('Cat', 'Sphynx', 'Mèo không lông Sphynx'),
    ('Cat', 'Thai', 'Mèo Thái'),
    ('Cat', 'Tonkinese', 'Mèo Tonkinese'),
    ('Cat', 'Toyger', 'Mèo Toyger'),
    ('Cat', 'Turkish Angora', 'Mèo Angora Thổ Nhĩ Kỳ'),
    ('Cat', 'Turkish Van', 'Mèo Van Thổ Nhĩ Kỳ'),
    ('Cat', 'Ukrainian Levkoy', 'Mèo Levkoy Ukraina'),
    -- Rabbits
    ('Rabbit', 'Mixed Breed', 'Thỏ lai'),
    ('Rabbit', 'Holland Lop', 'Thỏ Holland Lop'),
    ('Rabbit', 'Netherland Dwarf', 'Thỏ lùn Hà Lan'),
    ('Rabbit', 'Mini Rex', 'Thỏ Mini Rex'),
    ('Rabbit', 'Lionhead', 'Thỏ đầu sư tử'),
    ('Rabbit', 'Angora Rabbit', 'Thỏ Angora'),
    -- Hamsters
    ('Hamster', 'Syrian Hamster', 'Hamster Bear (Syrian)'),
    ('Hamster', 'Winter White Hamster', 'Hamster Winter White'),
    ('Hamster', 'Campbell''s Dwarf Hamster', 'Hamster Campbell'),
    ('Hamster', 'Roborovski Hamster', 'Hamster Robo');

-- ── 1. Merge duplicate species ───────────────────────────────

CREATE TEMP TABLE species_canon ON COMMIT DROP AS
SELECT ps.species_id,
       COALESCE(ss.name, sa.name, TRIM(ps.name)) AS canon_name,
       ps.is_deleted,
       ps.created_at
FROM pet_species ps
LEFT JOIN seed_species ss ON LOWER(ss.name) = LOWER(TRIM(ps.name)) OR LOWER(ss.name_vi) = LOWER(TRIM(ps.name))
LEFT JOIN seed_species_alias sa ON sa.alias = LOWER(TRIM(ps.name));

-- One keeper per canonical name: exact English match first, then active rows, then oldest.
CREATE TEMP TABLE species_map ON COMMIT DROP AS
SELECT sc.species_id AS old_id,
       FIRST_VALUE(sc.species_id) OVER (
           PARTITION BY LOWER(sc.canon_name)
           ORDER BY (ps.name = sc.canon_name) DESC, sc.is_deleted ASC, sc.created_at ASC, sc.species_id
       ) AS keeper_id,
       sc.canon_name
FROM species_canon sc
JOIN pet_species ps ON ps.species_id = sc.species_id;

-- ── 2. Merge duplicate breeds (within the merged species) ────

CREATE TEMP TABLE breed_canon ON COMMIT DROP AS
SELECT pb.breed_id,
       sm.keeper_id AS species_id,
       COALESCE(sb.name, TRIM(pb.name)) AS canon_name,
       pb.name AS current_name,
       pb.is_deleted,
       pb.created_at
FROM pet_breeds pb
JOIN species_map sm ON sm.old_id = pb.species_id
LEFT JOIN seed_breeds sb ON sb.species = sm.canon_name
     AND (LOWER(sb.name) = LOWER(TRIM(pb.name)) OR LOWER(sb.name_vi) = LOWER(TRIM(pb.name)));

CREATE TEMP TABLE breed_map ON COMMIT DROP AS
SELECT bc.breed_id AS old_id,
       FIRST_VALUE(bc.breed_id) OVER (
           PARTITION BY bc.species_id, LOWER(bc.canon_name)
           ORDER BY (bc.current_name = bc.canon_name) DESC, bc.is_deleted ASC, bc.created_at ASC, bc.breed_id
       ) AS keeper_id,
       bc.species_id,
       bc.canon_name
FROM breed_canon bc;

-- ── 3. Re-point pets, then drop duplicates ───────────────────

UPDATE pets p
SET breed_id = bm.keeper_id
FROM breed_map bm
WHERE p.breed_id = bm.old_id AND bm.old_id <> bm.keeper_id;

UPDATE pets p
SET species_id = sm.keeper_id
FROM species_map sm
WHERE p.species_id = sm.old_id AND sm.old_id <> sm.keeper_id;

DELETE FROM pet_breeds pb
USING breed_map bm
WHERE pb.breed_id = bm.old_id AND bm.old_id <> bm.keeper_id;

UPDATE pet_breeds pb
SET species_id = bm.species_id,
    name       = bm.canon_name
FROM breed_map bm
WHERE pb.breed_id = bm.keeper_id AND bm.old_id = bm.keeper_id;

DELETE FROM pet_species ps
USING species_map sm
WHERE ps.species_id = sm.old_id AND sm.old_id <> sm.keeper_id;

UPDATE pet_species ps
SET name = sm.canon_name
FROM species_map sm
WHERE ps.species_id = sm.keeper_id AND sm.old_id = sm.keeper_id;

-- ── 4. Insert missing seed rows + fill Vietnamese names ──────

INSERT INTO pet_species (species_id, name, name_vi)
SELECT gen_random_uuid(), ss.name, ss.name_vi
FROM seed_species ss
WHERE NOT EXISTS (SELECT 1 FROM pet_species ps WHERE LOWER(ps.name) = LOWER(ss.name));

UPDATE pet_species ps
SET name_vi = ss.name_vi, is_deleted = FALSE, deleted_at = NULL, deleted_by = NULL
FROM seed_species ss
WHERE ps.name = ss.name;

UPDATE pet_species SET name_vi = name WHERE name_vi IS NULL;

INSERT INTO pet_breeds (breed_id, species_id, name, name_vi)
SELECT gen_random_uuid(), ps.species_id, sb.name, sb.name_vi
FROM seed_breeds sb
JOIN pet_species ps ON ps.name = sb.species
WHERE NOT EXISTS (
    SELECT 1 FROM pet_breeds pb
    WHERE pb.species_id = ps.species_id AND LOWER(pb.name) = LOWER(sb.name)
);

UPDATE pet_breeds pb
SET name_vi = sb.name_vi
FROM seed_breeds sb
JOIN pet_species ps ON ps.name = sb.species
WHERE pb.species_id = ps.species_id AND pb.name = sb.name;

UPDATE pet_breeds SET name_vi = name WHERE name_vi IS NULL;

ALTER TABLE pet_species ALTER COLUMN name_vi SET NOT NULL;
ALTER TABLE pet_breeds  ALTER COLUMN name_vi SET NOT NULL;

-- ── 5. Prevent future case-only duplicates ───────────────────

ALTER TABLE pet_species DROP CONSTRAINT IF EXISTS pet_species_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_pet_species_name_lower ON pet_species (LOWER(name));

ALTER TABLE pet_breeds DROP CONSTRAINT IF EXISTS uq_pet_breeds_species_name;
CREATE UNIQUE INDEX IF NOT EXISTS uq_pet_breeds_species_name_lower ON pet_breeds (species_id, LOWER(name));
