alter table matches
  add column if not exists team_match_group_id uuid,
  add column if not exists team_bout_order integer,
  add column if not exists team_extra_bout boolean not null default false;

create index if not exists idx_matches_team_match_group
  on matches(team_match_group_id)
  where deleted_at is null;
