alter table users
  add column if not exists username varchar(120);

update users
set username = case
  when email is not null and trim(email) <> ''
    then lower(regexp_replace(email, '[^a-zA-Z0-9]+', '.', 'g'))
  else 'user.' || id::text
end
where username is null;

update users
set username = 'user.' || id::text
where username is null or trim(username) = '';

create unique index if not exists uq_users_username_active
  on users (lower(username))
  where username is not null and deleted_at is null;

create table if not exists account_requests (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references organizations(id),
  display_name varchar(180) not null,
  email varchar(180) not null,
  phone varchar(60) not null,
  gender varchar(40),
  birth_date date,
  current_address varchar(255),
  status varchar(40) not null default 'PENDING',
  decision_note varchar(500),
  decided_at timestamptz,
  decided_by_user_id uuid references users(id),
  approved_user_id uuid references users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create unique index if not exists uq_account_requests_org_email_pending
  on account_requests (organization_id, lower(email))
  where status = 'PENDING' and deleted_at is null;

create index if not exists idx_account_requests_org_status
  on account_requests (organization_id, status)
  where deleted_at is null;
