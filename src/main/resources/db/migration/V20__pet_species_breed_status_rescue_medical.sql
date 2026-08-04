CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS pet_species (
    species_id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE TABLE IF NOT EXISTS pet_breeds (
    breed_id UUID PRIMARY KEY,
    species_id UUID NOT NULL REFERENCES pet_species (species_id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uq_pet_breeds_species_name UNIQUE (species_id, name)
);

CREATE INDEX IF NOT EXISTS idx_pet_breeds_species ON pet_breeds (species_id);

INSERT INTO pet_species (species_id, name)
SELECT gen_random_uuid(), trimmed_species
FROM (
    SELECT DISTINCT NULLIF(TRIM(species), '') AS trimmed_species
    FROM pets
    WHERE species IS NOT NULL
) source
WHERE trimmed_species IS NOT NULL
ON CONFLICT (name) DO NOTHING;

INSERT INTO pet_breeds (breed_id, species_id, name)
SELECT gen_random_uuid(), ps.species_id, source.trimmed_breed
FROM (
    SELECT DISTINCT NULLIF(TRIM(species), '') AS trimmed_species,
                    NULLIF(TRIM(breed), '') AS trimmed_breed
    FROM pets
    WHERE species IS NOT NULL AND breed IS NOT NULL
) source
JOIN pet_species ps ON ps.name = source.trimmed_species
WHERE source.trimmed_species IS NOT NULL
  AND source.trimmed_breed IS NOT NULL
ON CONFLICT (species_id, name) DO NOTHING;

ALTER TABLE pets ADD COLUMN IF NOT EXISTS species_id UUID;
ALTER TABLE pets ADD COLUMN IF NOT EXISTS breed_id UUID;

UPDATE pets p
SET species_id = ps.species_id
FROM pet_species ps
WHERE p.species_id IS NULL
  AND ps.name = NULLIF(TRIM(p.species), '');

UPDATE pets p
SET breed_id = pb.breed_id
FROM pet_breeds pb
JOIN pet_species ps ON pb.species_id = ps.species_id
WHERE p.breed_id IS NULL
  AND p.species_id = ps.species_id
  AND pb.name = NULLIF(TRIM(p.breed), '');

ALTER TABLE pets ALTER COLUMN species_id SET NOT NULL;

ALTER TABLE pets
    ADD CONSTRAINT fk_pets_species
    FOREIGN KEY (species_id) REFERENCES pet_species (species_id);

ALTER TABLE pets
    ADD CONSTRAINT fk_pets_breed
    FOREIGN KEY (breed_id) REFERENCES pet_breeds (breed_id);

DROP INDEX IF EXISTS idx_pets_species;
CREATE INDEX IF NOT EXISTS idx_pets_species ON pets (species_id);
CREATE INDEX IF NOT EXISTS idx_pets_breed ON pets (breed_id);

UPDATE pets
SET status = CASE status
    WHEN 'UNOWNED' THEN 'AVAILABLE'
    WHEN 'FOSTERED' THEN 'FOSTERING'
    WHEN 'PENDING' THEN 'FOSTERING'
    WHEN 'UNAVAILABLE' THEN 'LOST'
    ELSE status
END;

ALTER TABLE pet_medical_records
    ADD COLUMN IF NOT EXISTS record_type VARCHAR(30) NOT NULL DEFAULT 'TREATMENT';

ALTER TABLE pet_medical_records
    ALTER COLUMN record_type DROP DEFAULT;

ALTER TABLE pets DROP COLUMN IF EXISTS species;
ALTER TABLE pets DROP COLUMN IF EXISTS breed;
ALTER TABLE pets DROP COLUMN IF EXISTS rescue_date;
ALTER TABLE pets DROP COLUMN IF EXISTS rescue_location;
