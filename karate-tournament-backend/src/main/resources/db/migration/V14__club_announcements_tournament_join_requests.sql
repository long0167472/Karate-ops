-- Club announcements: managers publish news that members see in their portal.
create table if not exists club_announcements (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references organizations(id),
  created_by_user_id uuid references users(id),
  title varchar(180) not null,
  content varchar(4000) not null,
  pinned boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index if not exists idx_club_announcements_org
  on club_announcements (organization_id, pinned desc, created_at desc)
  where deleted_at is null;

-- Leave request rework: support long-term leave (no single session), explicit type and expiry.
alter table attendance_leave_requests
  add column if not exists organization_id uuid references organizations(id),
  add column if not exists request_type varchar(40) not null default 'LEAVE_SESSION',
  add column if not exists from_date date,
  add column if not exists to_date date,
  add column if not exists expires_at timestamptz;

update attendance_leave_requests r
set organization_id = s.organization_id
from attendance_sessions s
where r.attendance_session_id = s.id
  and r.organization_id is null;

alter table attendance_leave_requests
  alter column organization_id set not null,
  alter column attendance_session_id drop not null;

create index if not exists idx_leave_requests_org_created
  on attendance_leave_requests (organization_id, created_at desc)
  where deleted_at is null;

-- Member requests to join a tournament, decided by the club manager.
create table if not exists tournament_join_requests (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references tournaments(id),
  organization_id uuid not null references organizations(id),
  organization_member_id uuid not null references organization_members(id),
  requester_user_id uuid references users(id),
  decided_by_user_id uuid references users(id),
  status varchar(40) not null default 'PENDING',
  note varchar(500),
  decision_note varchar(500),
  decided_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create unique index if not exists uq_tournament_join_request_active
  on tournament_join_requests (tournament_id, organization_member_id)
  where deleted_at is null;

create index if not exists idx_tournament_join_requests_org_status
  on tournament_join_requests (organization_id, status)
  where deleted_at is null;

-- Backstop for the duplicate-delegation check done in TournamentServiceImpl.addParticipant.
create unique index if not exists uq_tournament_participant_org_active
  on tournament_participants (tournament_id, organization_id)
  where deleted_at is null;
