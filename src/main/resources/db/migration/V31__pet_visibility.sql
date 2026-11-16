-- Who may see a pet outside its owner. A pet someone registered themselves is private until they
-- decide otherwise; everything else (organization pets, adopted pets) stays as visible as before.
ALTER TABLE pets ADD COLUMN visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC';

UPDATE pets SET visibility = 'PRIVATE'
WHERE pet_id IN (SELECT pet_id FROM pets_current_owner WHERE acquisition_type = 'SELF_REGISTERED');
