alter table attendance_sessions
  add column if not exists source varchar(40) not null default 'MANUAL',
  add column if not exists training_schedule_id uuid,
  add column if not exists scheduled_date date;

create table club_training_schedules (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references organizations(id),
  name varchar(180) not null,
  days_of_week varchar(40) not null default '',
  start_time time not null default '18:30',
  duration_minutes integer not null default 90,
  timezone varchar(80) not null default 'Asia/Ho_Chi_Minh',
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_club_training_schedule_org unique (organization_id)
);

alter table attendance_sessions
  add constraint fk_attendance_training_schedule
  foreign key (training_schedule_id) references club_training_schedules(id);

create unique index uq_attendance_generated_schedule_day
  on attendance_sessions (organization_id, training_schedule_id, scheduled_date)
  where deleted_at is null and source = 'SCHEDULED';

create index idx_attendance_sessions_org_date
  on attendance_sessions (organization_id, scheduled_date)
  where deleted_at is null;

update attendance_sessions
set scheduled_date = (scheduled_at at time zone 'Asia/Ho_Chi_Minh')::date
where scheduled_date is null and scheduled_at is not null;

insert into club_training_schedules (id, organization_id, name, days_of_week, start_time, duration_minutes, timezone, active)
values
  ('20000000-0000-0000-0000-000000000101', '10000000-0000-0000-0000-000000000101', 'Lịch tập chính Sakura Hà Nội', '2,4,6', '18:30', 90, 'Asia/Ho_Chi_Minh', true),
  ('20000000-0000-0000-0000-000000000102', '10000000-0000-0000-0000-000000000102', 'Lịch tập chính Rồng Việt', '3,5,7', '19:00', 90, 'Asia/Ho_Chi_Minh', true),
  ('20000000-0000-0000-0000-000000000103', '10000000-0000-0000-0000-000000000103', 'Lịch tập chính Sen Trắng', '2,5', '17:45', 75, 'Asia/Ho_Chi_Minh', true)
on conflict (organization_id) do nothing;
