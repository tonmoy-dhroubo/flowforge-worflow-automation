#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$ROOT/.run/pids"
LOG_DIR="$ROOT/.run/logs"
SERVICES=(auth workflow trigger orchestrator executor log api-gateway)

mkdir -p "$PID_DIR" "$LOG_DIR"

usage() {
  cat <<'EOF'
Usage: ./run-all.sh [--skip-build]

Starts all Flowforge Spring services and writes logs to ./.run/logs.
Use SKIP_BUILD=1 or --skip-build to skip the Maven build.
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

if [ "${1:-}" = "--skip-build" ]; then
  SKIP_BUILD=1
fi

if [ ! -x "$ROOT/mvnw" ]; then
  chmod +x "$ROOT/mvnw"
fi

DEFAULT_DB_HOST="${FLOWFORGE_DATASOURCE_HOST:-localhost:5432}"
DEFAULT_DB_NAME="${FLOWFORGE_DATASOURCE_DB_NAME:-devdb}"
DEFAULT_DB_USERNAME="${FLOWFORGE_DATASOURCE_USERNAME:-dev}"
DEFAULT_DB_PASSWORD="${FLOWFORGE_DATASOURCE_PASSWORD:-devpass}"
DEFAULT_DB_PARAMS="${FLOWFORGE_DATASOURCE_PARAMS:-sslmode=require&channelBinding=require}"
DEFAULT_JDBC_URL="jdbc:postgresql://${DEFAULT_DB_HOST}/${DEFAULT_DB_NAME}?${DEFAULT_DB_PARAMS}"

SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-$DEFAULT_JDBC_URL}"
SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$DEFAULT_DB_USERNAME}"
SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$DEFAULT_DB_PASSWORD}"
SPRING_DATASOURCE_DRIVER_CLASS_NAME="${SPRING_DATASOURCE_DRIVER_CLASS_NAME:-org.postgresql.Driver}"
KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
SPRING_DATA_MONGODB_URI="${SPRING_DATA_MONGODB_URI:-mongodb://localhost:27017/flowforge_logs}"

printf "\nFlowforge Spring services\n"
printf "Logs: %s\n" "$LOG_DIR"
printf "Status: ./status-all.sh | Stop: ./stop-all.sh\n\n"

if [ "${SKIP_BUILD:-0}" != "1" ]; then
  services_csv="$(IFS=,; echo "${SERVICES[*]}")"
  echo "Building services: $services_csv (set SKIP_BUILD=1 to skip)"
  "$ROOT/mvnw" -q -DskipTests -pl "$services_csv" -am clean package
else
  echo "Skipping build (SKIP_BUILD=1)"
fi

is_running() {
  local pid_file="$1"
  [ -f "$pid_file" ] || return 1
  local pid
  pid="$(cat "$pid_file")"
  kill -0 "$pid" 2>/dev/null
}

for s in "${SERVICES[@]}"; do
  pid_file="$PID_DIR/$s.pid"
  if is_running "$pid_file"; then
    printf "%-24s %s\n" "$s" "already running (pid $(cat "$pid_file"))"
    continue
  fi

  log_file="$LOG_DIR/$s.log"
  jar_file="$(find "$ROOT/$s/target" -maxdepth 1 -type f -name "$s-*.jar" ! -name "*original*" -print -quit)"
  if [ -z "${jar_file:-}" ]; then
    echo "Missing jar for $s. Rebuild with ./run-all.sh (or run ./mvnw -pl $s -am package)."
    continue
  fi

  SPRING_DATASOURCE_URL="$SPRING_DATASOURCE_URL" \
  SPRING_DATASOURCE_USERNAME="$SPRING_DATASOURCE_USERNAME" \
  SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
  SPRING_DATASOURCE_DRIVER_CLASS_NAME="$SPRING_DATASOURCE_DRIVER_CLASS_NAME" \
  KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP_SERVERS" \
  SPRING_DATA_MONGODB_URI="$SPRING_DATA_MONGODB_URI" \
  nohup java ${JAVA_OPTS:-} -Dio.netty.noUnsafe=true -Dio.netty.noNative=true -jar "$jar_file" >"$log_file" 2>&1 &
  echo $! >"$pid_file"
  printf "%-24s %s\n" "$s" "started (pid $(cat "$pid_file"), log $log_file)"
done
