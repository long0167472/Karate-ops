CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE club_fee_items
  ADD COLUMN IF NOT EXISTS fee_kind varchar(60);

UPDATE club_fee_items
SET fee_kind = CASE
  WHEN fee_type = 'TUITION' AND billing_cycle = 'MONTHLY' THEN 'MONTHLY_TUITION_OVERRIDE'
  ELSE 'ONE_TIME_INCOME'
END
WHERE fee_kind IS NULL;

WITH first_monthly_tuition AS (
  SELECT DISTINCT ON (organization_id) id
  FROM club_fee_items
  WHERE deleted_at IS NULL
    AND fee_type = 'TUITION'
    AND billing_cycle = 'MONTHLY'
  ORDER BY organization_id, created_at ASC
)
UPDATE club_fee_items
SET fee_kind = 'MONTHLY_TUITION_DEFAULT'
WHERE id IN (SELECT id FROM first_monthly_tuition);

INSERT INTO club_fee_items (
  id,
  organization_id,
  name,
  fee_type,
  fee_kind,
  billing_cycle,
  status,
  default_amount,
  due_day,
  description,
  created_at,
  updated_at
)
SELECT
  gen_random_uuid(),
  o.id,
  'Học phí',
  'TUITION',
  'MONTHLY_TUITION_DEFAULT',
  'MONTHLY',
  'ACTIVE',
  0,
  10,
  'Khoản học phí tháng mặc định của CLB.',
  now(),
  now()
FROM organizations o
WHERE o.type = 'CLUB'
  AND NOT EXISTS (
    SELECT 1
    FROM club_fee_items item
    WHERE item.organization_id = o.id
      AND item.fee_kind = 'MONTHLY_TUITION_DEFAULT'
      AND item.deleted_at IS NULL
  );

ALTER TABLE club_fee_items
  ALTER COLUMN fee_kind SET DEFAULT 'ONE_TIME_INCOME',
  ALTER COLUMN fee_kind SET NOT NULL;

CREATE TABLE IF NOT EXISTS organization_member_tuition_overrides (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES organizations(id),
  organization_member_id uuid NOT NULL REFERENCES organization_members(id),
  fee_item_id uuid NOT NULL REFERENCES club_fee_items(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_member_tuition_override_active
  ON organization_member_tuition_overrides(organization_member_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_member_tuition_overrides_org
  ON organization_member_tuition_overrides(organization_id)
  WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS club_finance_expenses (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES organizations(id),
  name varchar(180) NOT NULL,
  amount numeric(14, 2) NOT NULL DEFAULT 0,
  expense_date date,
  status varchar(60) NOT NULL DEFAULT 'PENDING_DISBURSEMENT',
  note varchar(500),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE INDEX IF NOT EXISTS ix_club_finance_expenses_org_status
  ON club_finance_expenses(organization_id, status)
  WHERE deleted_at IS NULL;
