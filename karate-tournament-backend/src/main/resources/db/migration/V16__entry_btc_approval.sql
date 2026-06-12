ALTER TABLE entries
    ADD COLUMN IF NOT EXISTS btc_approval_status varchar(40) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS tournament_points integer NOT NULL DEFAULT 0;
