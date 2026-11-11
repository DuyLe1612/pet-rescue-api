-- Compatibility matching (phase 2): what an adopter declares, what an organization records about
-- a pet, and the match result frozen on each application at the moment it is submitted.

CREATE TABLE IF NOT EXISTS adopter_profiles (
    user_id                   UUID PRIMARY KEY REFERENCES users (user_id),
    housing_type              VARCHAR(20)  NOT NULL,
    building_rule             VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    has_children_under_5      BOOLEAN      NOT NULL DEFAULT FALSE,
    has_children_5_to_8       BOOLEAN      NOT NULL DEFAULT FALSE,
    has_children_over_8       BOOLEAN      NOT NULL DEFAULT FALSE,
    allergy_dog               BOOLEAN      NOT NULL DEFAULT FALSE,
    allergy_cat               BOOLEAN      NOT NULL DEFAULT FALSE,
    has_dog                   BOOLEAN      NOT NULL DEFAULT FALSE,
    has_cat                   BOOLEAN      NOT NULL DEFAULT FALSE,
    hours_away_per_day        SMALLINT CHECK (hours_away_per_day BETWEEN 0 AND 24),
    activity_level            SMALLINT CHECK (activity_level BETWEEN 1 AND 5),
    monthly_budget            INTEGER CHECK (monthly_budget >= 0),
    emergency_vet_budget      INTEGER CHECK (emergency_vet_budget >= 0),
    adjustment_weeks_expected SMALLINT CHECK (adjustment_weeks_expected >= 0),
    intolerable_behaviors     TEXT,
    return_circumstances      TEXT,
    relocation_plan           TEXT,
    experience_level          SMALLINT CHECK (experience_level BETWEEN 1 AND 5),
    willing_training          BOOLEAN,
    province_code             VARCHAR(20),
    consent_at                TIMESTAMPTZ  NOT NULL,
    version                   INTEGER      NOT NULL DEFAULT 1,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- One row per pet, filled in directly by whoever manages the pet. Every scale is 1-5 and NULL
-- means "nobody recorded it yet", which the scoring treats as unknown, never as bad.
CREATE TABLE IF NOT EXISTS pet_traits (
    pet_id            UUID PRIMARY KEY REFERENCES pets (pet_id),
    friendly_people   SMALLINT CHECK (friendly_people BETWEEN 1 AND 5),
    friendly_children SMALLINT CHECK (friendly_children BETWEEN 1 AND 5),
    fear_strangers    SMALLINT CHECK (fear_strangers BETWEEN 1 AND 5),
    fear_noise        SMALLINT CHECK (fear_noise BETWEEN 1 AND 5),
    energy            SMALLINT CHECK (energy BETWEEN 1 AND 5),
    good_with_dogs    SMALLINT CHECK (good_with_dogs BETWEEN 1 AND 5),
    good_with_cats    SMALLINT CHECK (good_with_cats BETWEEN 1 AND 5),
    alone_minutes     INTEGER CHECK (alone_minutes >= 0),
    house_trained     SMALLINT CHECK (house_trained BETWEEN 1 AND 5),
    bite_history      VARCHAR(20),
    bite_verified     BOOLEAN     NOT NULL DEFAULT FALSE,
    size              VARCHAR(20),
    noise_level       SMALLINT CHECK (noise_level BETWEEN 1 AND 5),
    monthly_cost      INTEGER CHECK (monthly_cost >= 0),
    care_level        VARCHAR(20),
    source_role       VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    note              TEXT,
    updated_by        UUID REFERENCES users (user_id),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE adoption_applications
    ADD COLUMN IF NOT EXISTS match_level           VARCHAR(20),
    ADD COLUMN IF NOT EXISTS match_score           NUMERIC(5, 1),
    ADD COLUMN IF NOT EXISTS match_certainty       VARCHAR(10),
    ADD COLUMN IF NOT EXISTS match_weights_version INTEGER,
    ADD COLUMN IF NOT EXISTS match_warnings        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS warnings_acknowledged BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Sort key for reviewers: the score, -1 when there was nothing to score, -2 when a warning fired.
    ADD COLUMN IF NOT EXISTS match_rank            NUMERIC(5, 1) NOT NULL DEFAULT -1,
    -- JSON text: both profiles and the full result as they were at submission. Never recomputed.
    ADD COLUMN IF NOT EXISTS match_snapshot        TEXT,
    ADD COLUMN IF NOT EXISTS decision_reason_code  VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_adoption_org_match
    ON adoption_applications (organization_id, match_rank DESC, created_at DESC)
    WHERE is_deleted = FALSE;
