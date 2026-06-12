ALTER TABLE tournaments
    ADD COLUMN IF NOT EXISTS registration_deadline timestamptz,
    ADD COLUMN IF NOT EXISTS registration_fee numeric(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS step smallint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS phong_trao_enabled boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS nang_cao_enabled boolean NOT NULL DEFAULT true;
