alter table attendance_leave_requests
  alter column attendance_session_id drop not null;

alter table attendance_leave_requests
  add column if not exists request_type varchar(40),
  add column if not exists from_date date,
  add column if not exists to_date date,
  add column if not exists expires_at timestamptz;

update attendance_leave_requests
set request_type = 'LEAVE_SESSION'
where request_type is null;

update attendance_leave_requests leave_request
set expires_at = session.scheduled_at
from attendance_sessions session
where leave_request.expires_at is null
  and leave_request.attendance_session_id = session.id;

alter table attendance_leave_requests
  alter column request_type set not null;

with ranked_duplicates as (
  select
    id,
    row_number() over (
      partition by organization_member_id, fee_item_id
      order by created_at asc, id asc
    ) as duplicate_rank
  from member_fee_assignments
  where deleted_at is null
)
update member_fee_assignments assignment
set deleted_at = now(),
    updated_at = now()
from ranked_duplicates duplicate
where assignment.id = duplicate.id
  and duplicate.duplicate_rank > 1;

create unique index if not exists ux_member_fee_assignments_active
  on member_fee_assignments (organization_member_id, fee_item_id)
  where deleted_at is null;
