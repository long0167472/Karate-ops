alter table tournaments
  add column if not exists organizer_name varchar(180),
  add column if not exists tatami_count integer not null default 1,
  add column if not exists competition_levels varchar(120) not null default 'PHONG_TRAO,NANG_CAO',
  add column if not exists ruleset_preset varchar(40) not null default 'WKF',
  add column if not exists rule_snapshot_json text;

alter table categories
  add column if not exists competition_level varchar(40) not null default 'OPEN',
  add column if not exists weight_label varchar(40),
  add column if not exists open_weight boolean not null default false,
  add column if not exists repechage_enabled boolean not null default true,
  add column if not exists match_duration_seconds integer not null default 180,
  add column if not exists kata_judge_count integer not null default 5,
  add column if not exists kata_repeat_allowed boolean not null default false,
  add column if not exists entry_limit_per_organization integer;

update categories
set open_weight = true,
    weight_label = 'Vo dich tuyet doi'
where discipline in ('KUMITE', 'TEAM_KUMITE')
  and weight_min_kg is null
  and weight_max_kg is null
  and open_weight = false;

alter table entries
  add column if not exists registration_weight_kg numeric(6,2),
  add column if not exists weigh_in_status varchar(40) not null default 'NEEDS_ORGANIZER_REVIEW',
  add column if not exists team_name varchar(180),
  add column if not exists team_member_athlete_ids text,
  add column if not exists validation_notes text;
