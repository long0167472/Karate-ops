create extension if not exists pgcrypto;

create table organizations (
  id uuid primary key default gen_random_uuid(),
  name varchar(180) not null,
  short_name varchar(80),
  code varchar(60),
  type varchar(40) not null,
  status varchar(40) not null default 'ACTIVE',
  country varchar(80),
  province varchar(120),
  address varchar(255),
  contact_email varchar(160),
  contact_phone varchar(60),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_organizations_code unique (code)
);

create table users (
  id uuid primary key default gen_random_uuid(),
  display_name varchar(180) not null,
  email varchar(180),
  phone varchar(60),
  status varchar(40) not null default 'ACTIVE',
  primary_organization_id uuid references organizations(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_users_email unique (email)
);

create table roles (
  id uuid primary key default gen_random_uuid(),
  code varchar(60) not null unique,
  name varchar(120) not null,
  description varchar(255),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table user_role_assignments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id),
  role_id uuid not null references roles(id),
  scope_type varchar(40) not null default 'GLOBAL',
  scope_id uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_user_role_scope unique (user_id, role_id, scope_type, scope_id)
);

create table organization_members (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references organizations(id),
  user_id uuid references users(id),
  person_id uuid,
  role_code varchar(60) not null,
  status varchar(40) not null default 'ACTIVE',
  joined_at date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table tournaments (
  id uuid primary key default gen_random_uuid(),
  owner_organization_id uuid references organizations(id),
  created_by_user_id uuid references users(id),
  name varchar(220) not null,
  code varchar(80),
  description text,
  location varchar(255),
  starts_on date,
  ends_on date,
  visibility varchar(40) not null default 'PRIVATE',
  status varchar(40) not null default 'DRAFT',
  ruleset_version varchar(40) not null default 'WKF_2026',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_tournaments_code unique (code)
);

create table tournament_participants (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references tournaments(id),
  organization_id uuid not null references organizations(id),
  display_name varchar(180) not null,
  status varchar(40) not null default 'REQUESTED',
  approved_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_tournament_organization unique (tournament_id, organization_id)
);

create table persons (
  id uuid primary key default gen_random_uuid(),
  display_name varchar(180) not null,
  first_name varchar(90),
  last_name varchar(90),
  birth_date date,
  gender varchar(40),
  national_id varchar(80),
  email varchar(180),
  phone varchar(60),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

alter table organization_members
  add constraint fk_organization_members_person
  foreign key (person_id) references persons(id);

create table athletes (
  id uuid primary key default gen_random_uuid(),
  person_id uuid not null references persons(id),
  primary_organization_id uuid references organizations(id),
  external_code varchar(80),
  belt varchar(80),
  weight_kg numeric(6,2),
  height_cm numeric(6,2),
  status varchar(40) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_athletes_person unique (person_id)
);

create table club_roster (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references organizations(id),
  athlete_id uuid not null references athletes(id),
  status varchar(40) not null default 'ACTIVE',
  joined_at date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_club_roster unique (organization_id, athlete_id)
);

create table tournament_delegation_members (
  id uuid primary key default gen_random_uuid(),
  tournament_participant_id uuid not null references tournament_participants(id),
  person_id uuid not null references persons(id),
  athlete_id uuid references athletes(id),
  role_code varchar(60) not null,
  status varchar(40) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table tournament_officials (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references tournaments(id),
  person_id uuid not null references persons(id),
  organization_id uuid references organizations(id),
  role_code varchar(60) not null,
  tatami_no integer,
  status varchar(40) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table categories (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references tournaments(id),
  name varchar(180) not null,
  discipline varchar(40) not null,
  gender varchar(40) not null default 'OPEN',
  age_min integer,
  age_max integer,
  weight_min_kg numeric(6,2),
  weight_max_kg numeric(6,2),
  entry_type varchar(40) not null default 'INDIVIDUAL',
  status varchar(40) not null default 'DRAFT',
  ruleset_version varchar(40) not null default 'WKF_2026',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table teams (
  id uuid primary key default gen_random_uuid(),
  tournament_participant_id uuid not null references tournament_participants(id),
  name varchar(180) not null,
  status varchar(40) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table team_members (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references teams(id),
  athlete_id uuid not null references athletes(id),
  member_order integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_team_athlete unique (team_id, athlete_id)
);

create table entries (
  id uuid primary key default gen_random_uuid(),
  category_id uuid not null references categories(id),
  tournament_participant_id uuid not null references tournament_participants(id),
  athlete_id uuid references athletes(id),
  team_id uuid references teams(id),
  seed_no integer,
  status varchar(40) not null default 'REGISTERED',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint chk_entry_subject check (athlete_id is not null or team_id is not null)
);

create table tournament_team_members (
  id uuid primary key default gen_random_uuid(),
  entry_id uuid not null references entries(id),
  athlete_id uuid not null references athletes(id),
  member_order integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table tatamis (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references tournaments(id),
  tatami_no integer not null,
  name varchar(120) not null,
  status varchar(40) not null default 'ACTIVE',
  current_match_id uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_tournament_tatami_no unique (tournament_id, tatami_no)
);

create table brackets (
  id uuid primary key default gen_random_uuid(),
  category_id uuid not null references categories(id),
  type varchar(40) not null default 'REPECHAGE',
  size integer not null,
  status varchar(40) not null default 'DRAFT',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table pools (
  id uuid primary key default gen_random_uuid(),
  category_id uuid not null references categories(id),
  name varchar(120) not null,
  pool_order integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table matches (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references tournaments(id),
  category_id uuid not null references categories(id),
  bracket_id uuid references brackets(id),
  pool_id uuid references pools(id),
  tatami_id uuid references tatamis(id),
  match_number integer not null,
  round_name varchar(120) not null,
  round_number integer not null,
  bracket_position integer not null default 1,
  status varchar(40) not null default 'SCHEDULED',
  scheduled_at timestamptz,
  mode varchar(40) not null,
  winner_entry_id uuid references entries(id),
  winner_athlete_id uuid references athletes(id),
  win_type varchar(60),
  winner_next_match_id uuid,
  winner_next_side varchar(10),
  loser_next_match_id uuid,
  loser_next_side varchar(10),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

alter table tatamis
  add constraint fk_tatami_current_match
  foreign key (current_match_id) references matches(id);

alter table matches
  add constraint fk_match_winner_next
  foreign key (winner_next_match_id) references matches(id);

alter table matches
  add constraint fk_match_loser_next
  foreign key (loser_next_match_id) references matches(id);

create table match_participants (
  id uuid primary key default gen_random_uuid(),
  match_id uuid not null references matches(id),
  entry_id uuid references entries(id),
  side varchar(10) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_match_side unique (match_id, side)
);

create table kumite_match_state (
  match_id uuid primary key references matches(id),
  aka_score integer not null default 0,
  ao_score integer not null default 0,
  aka_senshu boolean not null default false,
  ao_senshu boolean not null default false,
  aka_chui integer not null default 0,
  ao_chui integer not null default 0,
  aka_hansoku_chui boolean not null default false,
  ao_hansoku_chui boolean not null default false,
  aka_hansoku boolean not null default false,
  ao_hansoku boolean not null default false,
  aka_shikkaku boolean not null default false,
  ao_shikkaku boolean not null default false,
  aka_kiken boolean not null default false,
  ao_kiken boolean not null default false,
  duration_ms integer not null default 180000,
  remaining_ms integer not null default 180000,
  timer_running boolean not null default false,
  timer_started_at timestamptz,
  updated_at timestamptz not null default now()
);

create table kata_votes (
  id uuid primary key default gen_random_uuid(),
  match_id uuid not null references matches(id),
  judge_number integer not null,
  side varchar(10) not null,
  vote_value numeric(5,2),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_kata_vote unique (match_id, judge_number)
);

create table match_score_events (
  id uuid primary key default gen_random_uuid(),
  match_id uuid not null references matches(id),
  actor_user_id uuid references users(id),
  type varchar(60) not null,
  side varchar(10),
  points integer,
  penalty_code varchar(60),
  judge_number integer,
  vote_side varchar(10),
  payload_json text,
  occurred_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table match_audit_events (
  id uuid primary key default gen_random_uuid(),
  match_id uuid not null references matches(id),
  actor_user_id uuid references users(id),
  action varchar(80) not null,
  reason varchar(255),
  payload_json text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table category_results (
  id uuid primary key default gen_random_uuid(),
  category_id uuid not null references categories(id),
  entry_id uuid not null references entries(id),
  athlete_id uuid references athletes(id),
  team_id uuid references teams(id),
  placement integer not null,
  medal varchar(20),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint uq_category_placement_entry unique (category_id, placement, entry_id)
);

create index idx_tournaments_owner on tournaments(owner_organization_id) where deleted_at is null;
create index idx_tournament_participants_tournament on tournament_participants(tournament_id) where deleted_at is null;
create index idx_persons_display_name on persons(display_name) where deleted_at is null;
create index idx_athletes_org on athletes(primary_organization_id) where deleted_at is null;
create index idx_categories_tournament on categories(tournament_id) where deleted_at is null;
create index idx_entries_category on entries(category_id) where deleted_at is null;
create index idx_tatamis_tournament on tatamis(tournament_id) where deleted_at is null;
create index idx_matches_tournament_status on matches(tournament_id, status) where deleted_at is null;
create index idx_matches_tatami_status on matches(tatami_id, status) where deleted_at is null;
create index idx_match_events_match on match_score_events(match_id, occurred_at) where deleted_at is null;

insert into roles (id, code, name, description) values
  ('00000000-0000-0000-0000-000000000101', 'GLOBAL_ADMIN', 'Global admin', 'Full platform access'),
  ('00000000-0000-0000-0000-000000000102', 'TOURNAMENT_OWNER', 'Tournament owner', 'Manage tournaments owned by the organization'),
  ('00000000-0000-0000-0000-000000000103', 'CLUB_MANAGER', 'Club manager', 'Manage club roster and tournament participation'),
  ('00000000-0000-0000-0000-000000000104', 'DELEGATION_MANAGER', 'Delegation manager', 'Manage one delegation inside a tournament'),
  ('00000000-0000-0000-0000-000000000105', 'TATAMI_OPERATOR', 'Tatami operator', 'Operate assigned tatami matches'),
  ('00000000-0000-0000-0000-000000000106', 'JUDGE', 'Judge', 'Submit Kata votes'),
  ('00000000-0000-0000-0000-000000000107', 'VIEWER', 'Viewer', 'Read display and dashboard data')
on conflict (code) do nothing;

insert into organizations (id, name, short_name, code, type, status)
values ('00000000-0000-0000-0000-000000000201', 'Global Administration', 'GLOBAL', 'GLOBAL_ADMIN_ORG', 'ORGANIZER', 'ACTIVE')
on conflict (code) do nothing;

insert into users (id, display_name, email, status, primary_organization_id)
values ('00000000-0000-0000-0000-000000000001', 'Global Admin', 'global-admin@local.karate', 'ACTIVE', '00000000-0000-0000-0000-000000000201')
on conflict (email) do nothing;

insert into user_role_assignments (id, user_id, role_id, scope_type)
values (
  '00000000-0000-0000-0000-000000000301',
  '00000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000101',
  'GLOBAL'
)
on conflict do nothing;
