insert into roles (code, name, description)
values ('MEMBER', 'Member', 'Baseline authenticated member access')
on conflict (code) do nothing;

create table if not exists attendance_leave_requests (
  id uuid primary key default gen_random_uuid(),
  attendance_session_id uuid not null references attendance_sessions(id),
  organization_member_id uuid not null references organization_members(id),
  requester_user_id uuid references users(id),
  decided_by_user_id uuid references users(id),
  status varchar(40) not null default 'PENDING',
  reason varchar(500) not null,
  decision_note varchar(500),
  decided_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create unique index if not exists uq_leave_request_session_member_active
  on attendance_leave_requests (attendance_session_id, organization_member_id)
  where deleted_at is null;

create index if not exists idx_leave_requests_org_status
  on attendance_leave_requests (status)
  where deleted_at is null;
