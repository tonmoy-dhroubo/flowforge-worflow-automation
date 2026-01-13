CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS workflow;
CREATE SCHEMA IF NOT EXISTS trigger_service;
CREATE SCHEMA IF NOT EXISTS orchestrator;

CREATE TABLE IF NOT EXISTS auth.users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  username text NOT NULL UNIQUE,
  email text NOT NULL UNIQUE,
  password text NOT NULL,
  refresh_token varchar(512),
  refresh_token_expiry_date timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS workflow.workflows (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  user_id uuid NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  trigger_definition jsonb,
  actions_definition jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_workflows_user_id
  ON workflow.workflows (user_id);

CREATE TABLE IF NOT EXISTS trigger_service.trigger_registrations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workflow_id uuid NOT NULL,
  user_id uuid NOT NULL,
  trigger_type text NOT NULL,
  configuration jsonb,
  enabled boolean NOT NULL DEFAULT true,
  webhook_url text UNIQUE,
  webhook_token text UNIQUE,
  last_triggered_at timestamptz,
  next_scheduled_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_workflow_id
  ON trigger_service.trigger_registrations (workflow_id);
CREATE INDEX IF NOT EXISTS idx_user_id
  ON trigger_service.trigger_registrations (user_id);
CREATE INDEX IF NOT EXISTS idx_webhook_token
  ON trigger_service.trigger_registrations (webhook_token);
CREATE INDEX IF NOT EXISTS idx_trigger_type
  ON trigger_service.trigger_registrations (trigger_type);

CREATE TABLE IF NOT EXISTS orchestrator.workflow_executions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workflow_id uuid NOT NULL,
  user_id uuid NOT NULL,
  status text NOT NULL,
  current_step integer NOT NULL,
  trigger_payload jsonb,
  step_outputs jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_executions_workflow_id
  ON orchestrator.workflow_executions (workflow_id);
CREATE INDEX IF NOT EXISTS idx_executions_user_id
  ON orchestrator.workflow_executions (user_id);
