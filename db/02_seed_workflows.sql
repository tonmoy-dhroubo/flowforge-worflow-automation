INSERT INTO workflow.workflows (
  id,
  name,
  user_id,
  enabled,
  trigger_definition,
  actions_definition,
  created_at,
  updated_at
) VALUES (
  '2e9d6fcb-3f9b-4a72-9f75-7b7a2a4c5db1',
  'Incoming Webhook -> Slack',
  '0f4b3f1a-54e0-4c4b-9c5b-6f0fdc0d8a11',
  true,
  '{"type":"webhook","config":{"method":"POST"}}'::jsonb,
  '[{"type":"SLACK_MESSAGE","config":{"message":"Hello from Flowforge","channel":"#alerts","webhookUrl":"https://hooks.slack.com/services/T000/B000/XXX"}}]'::jsonb,
  now(),
  now()
),
(
  '1b4c1d5a-9ad0-4d4f-8d2a-5611d8899a70',
  'Schedule -> Sheets',
  '0f4b3f1a-54e0-4c4b-9c5b-6f0fdc0d8a11',
  true,
  '{"type":"scheduler","config":{"intervalMinutes":15}}'::jsonb,
  '[{"type":"GOOGLE_SHEET_ROW","config":{"spreadsheetId":"demo-sheet","range":"Sheet1!A1","apiKey":"demo-key","values":[["Flow","Forge"]]}}]'::jsonb,
  now(),
  now()
)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  user_id = EXCLUDED.user_id,
  enabled = EXCLUDED.enabled,
  trigger_definition = EXCLUDED.trigger_definition,
  actions_definition = EXCLUDED.actions_definition,
  updated_at = now();
