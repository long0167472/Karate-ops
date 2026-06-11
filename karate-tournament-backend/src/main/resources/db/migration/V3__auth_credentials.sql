alter table users
  add column if not exists password_hash varchar(120),
  add column if not exists last_login_at timestamptz;

create unique index if not exists uq_users_email_active
  on users (lower(email))
  where email is not null and deleted_at is null;
