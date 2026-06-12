CREATE TABLE user_notifications (
  id uuid PRIMARY KEY,
  app_user_id uuid NOT NULL REFERENCES users(id),
  type varchar(80) NOT NULL,
  title varchar(200) NOT NULL,
  body varchar(500),
  link varchar(500),
  read_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE INDEX ix_user_notifications_user_created
  ON user_notifications(app_user_id, created_at DESC)
  WHERE deleted_at IS NULL;
