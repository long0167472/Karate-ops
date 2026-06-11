alter table organization_members
  add constraint chk_organization_members_subject
  check (user_id is not null or person_id is not null);

create unique index uq_organization_members_org_person_active
  on organization_members (organization_id, person_id)
  where person_id is not null and deleted_at is null;

create index idx_organization_members_org_status
  on organization_members (organization_id, status)
  where deleted_at is null;

alter table club_roster
  drop constraint if exists uq_club_roster;

create unique index uq_club_roster_org_athlete_active
  on club_roster (organization_id, athlete_id)
  where deleted_at is null;

create index idx_club_roster_org_status
  on club_roster (organization_id, status)
  where deleted_at is null;

create table attendance_sessions (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references organizations(id),
  tournament_participant_id uuid references tournament_participants(id),
  name varchar(180) not null,
  session_type varchar(40) not null default 'TRAINING',
  status varchar(40) not null default 'OPEN',
  scheduled_at timestamptz,
  notes varchar(500),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table attendance_records (
  id uuid primary key default gen_random_uuid(),
  attendance_session_id uuid not null references attendance_sessions(id),
  organization_member_id uuid references organization_members(id),
  athlete_id uuid references athletes(id),
  status varchar(40) not null default 'PRESENT',
  check_in_at timestamptz,
  note varchar(500),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint chk_attendance_record_subject check (organization_member_id is not null or athlete_id is not null)
);

create unique index uq_attendance_record_session_member_active
  on attendance_records (attendance_session_id, organization_member_id)
  where organization_member_id is not null and deleted_at is null;

create unique index uq_attendance_record_session_athlete_active
  on attendance_records (attendance_session_id, athlete_id)
  where athlete_id is not null and deleted_at is null;

create index idx_attendance_sessions_org_scheduled
  on attendance_sessions (organization_id, scheduled_at)
  where deleted_at is null;

create index idx_attendance_records_session_status
  on attendance_records (attendance_session_id, status)
  where deleted_at is null;

insert into roles (id, code, name, description) values
  ('00000000-0000-0000-0000-000000000108', 'COACH', 'Coach', 'Manage attendance and view club roster')
on conflict (code) do nothing;
