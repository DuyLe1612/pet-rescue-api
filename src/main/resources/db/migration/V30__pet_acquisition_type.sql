-- How the current owner came to have the pet. It belongs to the ownership, not to the pet:
-- one animal can be rescued by an organization, adopted, and given back.
ALTER TABLE pet_ownerships
    ADD COLUMN acquisition_type VARCHAR(20),
    ADD COLUMN adoption_application_id UUID REFERENCES adoption_applications (application_id);

ALTER TABLE pets_current_owner
    ADD COLUMN acquisition_type VARCHAR(20),
    ADD COLUMN adoption_application_id UUID REFERENCES adoption_applications (application_id);

-- Old rows: an organization owner is intake; a user who has a completed application for the
-- pet adopted it; every other user owner registered the pet themselves.
UPDATE pets_current_owner o
SET acquisition_type = CASE
        WHEN o.owner_type = 'ORGANIZATION' THEN 'ORG_INTAKE'
        WHEN a.application_id IS NOT NULL THEN 'ADOPTED'
        ELSE 'SELF_REGISTERED' END,
    adoption_application_id = CASE WHEN o.owner_type = 'USER' THEN a.application_id END
FROM (SELECT o2.pet_id, (SELECT x.application_id FROM adoption_applications x
                          WHERE x.pet_id = o2.pet_id AND x.applicant_id = o2.owner_id
                            AND x.status = 'COMPLETED' AND x.is_deleted = FALSE
                          ORDER BY x.updated_at DESC NULLS LAST LIMIT 1) AS application_id
        FROM pets_current_owner o2) a
WHERE a.pet_id = o.pet_id;

UPDATE pet_ownerships p
SET acquisition_type = CASE
        WHEN p.owner_type = 'ORGANIZATION' THEN 'ORG_INTAKE'
        WHEN c.acquisition_type IS NOT NULL AND c.owner_id = p.owner_id AND p.to_time IS NULL THEN c.acquisition_type
        ELSE 'SELF_REGISTERED' END,
    adoption_application_id = CASE WHEN p.owner_type = 'USER' AND p.to_time IS NULL AND c.owner_id = p.owner_id
                                   THEN c.adoption_application_id END
FROM pets_current_owner c
WHERE c.pet_id = p.pet_id;

UPDATE pet_ownerships SET acquisition_type = 'SELF_REGISTERED' WHERE acquisition_type IS NULL;

CREATE INDEX idx_current_owner_acquisition ON pets_current_owner (owner_id, acquisition_type);
