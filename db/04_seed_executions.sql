INSERT INTO orchestrator.workflow_executions (
  id,
  workflow_id,
  user_id,
  status,
  current_step,
  trigger_payload,
  step_outputs,
  created_at,
  updated_at
) VALUES (
  'a6f48af5-6d7b-4b9a-9bb4-0b7fbaf9c941',
  '2e9d6fcb-3f9b-4a72-9f75-7b7a2a4c5db1',
  '0f4b3f1a-54e0-4c4b-9c5b-6f0fdc0d8a11',
  'COMPLETED',
  1,
  '{"body":{"message":"Hello"},"method":"POST"}'::jsonb,
  '{"step1":{"status":"ok","output":"posted"}}'::jsonb,
  now(),
  now()
)
ON CONFLICT (id) DO UPDATE SET
  workflow_id = EXCLUDED.workflow_id,
  user_id = EXCLUDED.user_id,
  status = EXCLUDED.status,
  current_step = EXCLUDED.current_step,
  trigger_payload = EXCLUDED.trigger_payload,
  step_outputs = EXCLUDED.step_outputs,
  updated_at = now();
