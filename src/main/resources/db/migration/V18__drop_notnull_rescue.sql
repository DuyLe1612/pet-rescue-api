ALTER TABLE rescue_case_completions
    ALTER COLUMN verified_by DROP NOT NULL;

ALTER TABLE rescue_case_completions
    ALTER COLUMN verified_at DROP NOT NULL;