# FlowForge Workflow Automation

FlowForge is a multi-service workflow automation platform built with Spring Boot, Kafka, PostgreSQL, and MongoDB. It supports webhook, scheduler, and email triggers, orchestrates multi-step workflows, and executes action plugins (Slack, Google Sheets).

## Services
- `api-gateway` (8080): Public entry point and routing.
- `auth` (8081): Authentication and JWT handling.
- `workflow` (8082): Workflow definitions and management.
- `trigger` (8083): Webhook, scheduler, and email triggers.
- `orchestrator` (8084): Workflow execution state and step dispatch.
- `executor` (8085): Executes actions and produces results.
- `log` (8086): Execution logs in MongoDB.

## Quick Start (Local)
Prereqs: Java 21, Docker, and the Maven Wrapper.

Start infrastructure:
```bash
docker-compose up -d
```

Run a service (example):
```bash
cd orchestrator
mvnw.cmd spring-boot:run
```

Build all modules:
```bash
mvnw.cmd clean package
```

## Configuration
Each service uses `src/main/resources/application.yml`. Local overrides should use env vars.

Common env vars:
```bash
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/flowforge
SPRING_DATASOURCE_USERNAME=flowforge
SPRING_DATASOURCE_PASSWORD=password
APPLICATION_SECURITY_JWT_SECRET_KEY=dev-secret
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Email polling (trigger service):
```bash
email.polling.enabled=true
email.polling.imap.host=imap.example.com
email.polling.imap.port=993
email.polling.imap.protocol=imaps
```
Per-trigger config must include `username` (or `emailAddress`) and `password`.

Slack action config:
```json
{ "webhookUrl": "...", "message": "Hello", "channel": "#general" }
```

Google Sheets action config:
```json
{ "spreadsheetId": "...", "range": "Sheet1!A1", "apiKey": "...", "values": [["a","b"]] }
```

## Kafka Topics
- `trigger.events`
- `execution.start`
- `execution.result`

## Development Notes
- Standard Maven layout per service (`src/main/java`, `src/test/java`).
- PostgreSQL for core services; MongoDB for logs.
