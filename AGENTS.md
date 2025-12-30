# Repository Guidelines

## Project Structure & Module Organization
This is a multi-module Spring Boot workspace. Top-level modules include `auth`, `workflow`, `trigger`, `orchestrator`, `executor`, `log`, `api-gateway`, and `common`. Each service follows the standard Maven layout (`src/main/java`, `src/main/resources`, `src/test/java`). Configuration lives in each service under `src/main/resources/application.yml`. Kafka topics and service URLs are centralized per service config.

## Build, Test, and Development Commands
Use Maven Wrapper per module:
- Build a service: `cd auth; mvnw.cmd clean package`
- Run a service: `cd orchestrator; mvnw.cmd spring-boot:run`
- Run all tests in a service: `cd trigger; mvnw.cmd test`
Root build (all modules): `mvnw.cmd clean package` from the repo root.

## Coding Style & Naming Conventions
Java: 4-space indentation, standard Spring conventions. Class names are `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`. Keep DTOs in `dto/`, entities in `entity/`, and Kafka components in `kafka/`. Avoid Unicode in source files.

## Testing Guidelines
Frameworks: JUnit 5 with Spring Boot Test. Name tests `*Test.java` and place them under `src/test/java` in each module. Prefer service-level tests for business logic and controller tests for API edges. Run with `mvnw.cmd test` in the target module.

## Commit & Pull Request Guidelines
Commit messages in this repo are short and imperative (e.g., “Implement email polling…”). Keep commits focused to a single logical change. PRs should include:
- Clear summary and scope
- How to run or verify (commands)
- Config changes or required env vars

## Security & Configuration Tips
Local defaults are set in `application.yml`, but secrets should come from env vars. Common envs:
- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `APPLICATION_SECURITY_JWT_SECRET_KEY`
