ALTER TABLE tournament_participants
    ADD COLUMN IF NOT EXISTS approved_by_user_id uuid REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS inactivated_at timestamptz;
