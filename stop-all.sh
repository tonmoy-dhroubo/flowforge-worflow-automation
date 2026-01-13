#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$ROOT/.run/pids"
SERVICES=(auth workflow trigger orchestrator executor log api-gateway)

if [ ! -d "$PID_DIR" ]; then
  echo "No running services found (missing $PID_DIR)."
  exit 0
fi

printf "\nStopping Flowforge Spring services\n\n"

for s in "${SERVICES[@]}"; do
  pid_file="$PID_DIR/$s.pid"
  if [ ! -f "$pid_file" ]; then
    printf "%-24s %s\n" "$s" "not running"
    continue
  fi

  pid="$(cat "$pid_file")"
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    for _ in {1..30}; do
      if kill -0 "$pid" 2>/dev/null; then
        sleep 1
      else
        break
      fi
    done
    if kill -0 "$pid" 2>/dev/null; then
      kill -9 "$pid" || true
    fi
    printf "%-24s %s\n" "$s" "stopped"
  else
    printf "%-24s %s\n" "$s" "stale pid $pid"
  fi

  rm -f "$pid_file"
done
