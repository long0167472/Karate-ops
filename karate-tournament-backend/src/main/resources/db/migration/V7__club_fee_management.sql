CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE club_fee_roles (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES organizations(id),
  code varchar(80) NOT NULL,
  name varchar(160) NOT NULL,
  description varchar(500),
  priority int NOT NULL DEFAULT 100,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE UNIQUE INDEX ux_club_fee_roles_org_code_active
  ON club_fee_roles(organization_id, lower(code))
  WHERE deleted_at IS NULL;

CREATE TABLE organization_member_fee_roles (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES organizations(id),
  organization_member_id uuid NOT NULL REFERENCES organization_members(id),
  fee_role_id uuid NOT NULL REFERENCES club_fee_roles(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE UNIQUE INDEX ux_member_fee_roles_active
  ON organization_member_fee_roles(organization_member_id, fee_role_id)
  WHERE deleted_at IS NULL;

CREATE INDEX ix_member_fee_roles_org_member
  ON organization_member_fee_roles(organization_id, organization_member_id)
  WHERE deleted_at IS NULL;

CREATE TABLE club_fee_items (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES organizations(id),
  name varchar(180) NOT NULL,
  fee_type varchar(60) NOT NULL DEFAULT 'TUITION',
  billing_cycle varchar(60) NOT NULL DEFAULT 'MONTHLY',
  status varchar(40) NOT NULL DEFAULT 'ACTIVE',
  default_amount numeric(14, 2) NOT NULL DEFAULT 0,
  due_day int,
  description varchar(500),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE INDEX ix_club_fee_items_org
  ON club_fee_items(organization_id, status)
  WHERE deleted_at IS NULL;

CREATE TABLE club_fee_item_role_amounts (
  id uuid PRIMARY KEY,
  fee_item_id uuid NOT NULL REFERENCES club_fee_items(id),
  fee_role_id uuid NOT NULL REFERENCES club_fee_roles(id),
  amount numeric(14, 2) NOT NULL DEFAULT 0,
  exempt boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE UNIQUE INDEX ux_fee_item_role_amount_active
  ON club_fee_item_role_amounts(fee_item_id, fee_role_id)
  WHERE deleted_at IS NULL;

CREATE TABLE member_fee_assignments (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES organizations(id),
  organization_member_id uuid NOT NULL REFERENCES organization_members(id),
  fee_item_id uuid NOT NULL REFERENCES club_fee_items(id),
  assigned_role_id uuid REFERENCES club_fee_roles(id),
  amount_due numeric(14, 2) NOT NULL DEFAULT 0,
  paid_amount numeric(14, 2) NOT NULL DEFAULT 0,
  status varchar(40) NOT NULL DEFAULT 'PENDING',
  due_date date,
  source varchar(40) NOT NULL DEFAULT 'RULE',
  note varchar(500),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE INDEX ix_member_fee_assignments_org_member
  ON member_fee_assignments(organization_id, organization_member_id)
  WHERE deleted_at IS NULL;

CREATE INDEX ix_member_fee_assignments_org_status
  ON member_fee_assignments(organization_id, status)
  WHERE deleted_at IS NULL;

INSERT INTO club_fee_roles (id, organization_id, code, name, description, priority, active, created_at, updated_at)
SELECT gen_random_uuid(), o.id, seed.code, seed.name, seed.description, seed.priority, true, now(), now()
FROM organizations o
CROSS JOIN (
  VALUES
    ('NORMAL', 'Học viên thường', 'Mức phí tiêu chuẩn cho học viên CLB.', 100),
    ('STUDENT', 'Sinh viên', 'Mức phí ưu đãi cho sinh viên.', 80),
    ('ATHLETE', 'VĐV đội tuyển', 'Mức phí ưu đãi cho VĐV thi đấu.', 60),
    ('CLUB_STAFF', 'Ban cán sự CLB', 'Mức phí ưu tiên cho ban cán sự/cộng tác viên.', 40)
) AS seed(code, name, description, priority)
WHERE o.type = 'CLUB'
  AND NOT EXISTS (
    SELECT 1 FROM club_fee_roles r
    WHERE r.organization_id = o.id AND lower(r.code) = lower(seed.code) AND r.deleted_at IS NULL
  );
