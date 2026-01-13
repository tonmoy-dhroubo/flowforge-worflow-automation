INSERT INTO auth.users (
  id,
  username,
  email,
  password,
  refresh_token,
  refresh_token_expiry_date,
  created_at,
  updated_at
) VALUES (
  '0f4b3f1a-54e0-4c4b-9c5b-6f0fdc0d8a11',
  'flowadmin',
  'flowadmin@flowforge.local',
  '$2b$12$.B0EDDfetk9a3qbqktcGKOPV4hVqi2lODpK9gA1mTVrfWhtYrRXyS',
  NULL,
  NULL,
  now(),
  now()
)
ON CONFLICT (username) DO UPDATE SET
  email = EXCLUDED.email,
  password = EXCLUDED.password,
  updated_at = now();
