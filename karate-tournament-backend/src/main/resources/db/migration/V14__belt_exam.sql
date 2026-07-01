create table belt_exams (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references organizations(id),
  name varchar(180) not null,
  status varchar(40) not null default 'DRAFT',
  exam_date date,
  location varchar(180),
  examiner_name varchar(180),
  notes varchar(500),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index idx_belt_exams_org_date
  on belt_exams (organization_id, exam_date)
  where deleted_at is null;

create table belt_exam_candidates (
  id uuid primary key default gen_random_uuid(),
  belt_exam_id uuid not null references belt_exams(id),
  organization_member_id uuid references organization_members(id),
  athlete_id uuid references athletes(id),
  current_belt varchar(80),
  target_belt varchar(80) not null,
  result varchar(40) not null default 'PENDING',
  examiner_note varchar(500),
  belt_applied boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint chk_belt_exam_candidate_subject check (organization_member_id is not null or athlete_id is not null)
);

create unique index uq_belt_exam_candidate_exam_member
  on belt_exam_candidates (belt_exam_id, organization_member_id)
  where organization_member_id is not null and deleted_at is null;

create unique index uq_belt_exam_candidate_exam_athlete
  on belt_exam_candidates (belt_exam_id, athlete_id)
  where athlete_id is not null and deleted_at is null;

create index idx_belt_exam_candidates_exam
  on belt_exam_candidates (belt_exam_id)
  where deleted_at is null;
