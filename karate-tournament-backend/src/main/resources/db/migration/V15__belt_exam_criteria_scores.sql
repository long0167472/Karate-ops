alter table belt_exams
  add column pass_threshold numeric(6, 2);

create table belt_exam_criteria (
  id uuid primary key default gen_random_uuid(),
  belt_exam_id uuid not null references belt_exams(id),
  name varchar(180) not null,
  description varchar(300),
  max_score numeric(6, 2) not null default 10.00,
  weight numeric(6, 2) not null default 1.00,
  display_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index idx_belt_exam_criteria_exam
  on belt_exam_criteria (belt_exam_id, display_order)
  where deleted_at is null;

create table belt_exam_scores (
  id uuid primary key default gen_random_uuid(),
  candidate_id uuid not null references belt_exam_candidates(id),
  criterion_id uuid not null references belt_exam_criteria(id),
  score numeric(6, 2) not null default 0,
  note varchar(300),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create unique index uq_belt_exam_score_candidate_criterion
  on belt_exam_scores (candidate_id, criterion_id)
  where deleted_at is null;

create index idx_belt_exam_scores_candidate
  on belt_exam_scores (candidate_id)
  where deleted_at is null;
