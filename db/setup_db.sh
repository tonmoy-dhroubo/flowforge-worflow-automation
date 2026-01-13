#!/usr/bin/env bash
set -euo pipefail

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-devdb}
DB_USER=${DB_USER:-dev}
DB_PASSWORD=${DB_PASSWORD:-devpass}
DB_SSLMODE=${DB_SSLMODE:-require}
DB_CHANNEL_BINDING=${DB_CHANNEL_BINDING:-require}
DB_DATA_DIR=${DB_DATA_DIR:-/var/lib/postgres/data}
DB_LOCALE=${DB_LOCALE:-}
DB_ENCODING=${DB_ENCODING:-UTF8}

export PGPASSWORD="$DB_PASSWORD"

run_as_postgres() {
  if [ "$(id -u)" -eq 0 ]; then
    su - postgres -c "$*"
  else
    sudo -n -iu postgres bash -lc "$*" || return 1
  fi
}

is_local_host() {
  case "$DB_HOST" in
    localhost|127.0.0.1|::1) return 0 ;;
    *) return 1 ;;
  esac
}

adjust_ssl_for_local() {
  if is_local_host && [ "$DB_SSLMODE" = "require" ]; then
    echo "Local Postgres detected; switching sslmode to disable for setup."
    DB_SSLMODE="disable"
    DB_CHANNEL_BINDING="disable"
  fi
}

ensure_postgres_running() {
  if pg_isready -h "$DB_HOST" -p "$DB_PORT" >/dev/null 2>&1; then
    return 0
  fi

  if ! is_local_host; then
    echo "Postgres is not reachable at $DB_HOST:$DB_PORT and host is not local."
    echo "Skipping auto-start; ensure the server is running and reachable."
    return 1
  fi

  if [ ! -f "$DB_DATA_DIR/PG_VERSION" ]; then
    pick_locale() {
      if [ -n "$DB_LOCALE" ]; then
        echo "$DB_LOCALE"
        return
      fi
      if command -v locale >/dev/null 2>&1; then
        locales="$(locale -a 2>/dev/null || true)"
        if echo "$locales" | grep -Eq '^en_US\.utf8$|^en_US\.UTF-8$'; then
          echo "en_US.UTF-8"
          return
        fi
        if echo "$locales" | grep -Eq '^C\.UTF-8$'; then
          echo "C.UTF-8"
          return
        fi
        if echo "$locales" | grep -Eq '^C$'; then
          echo "C"
          return
        fi
      fi
      echo "C"
    }
    initdb_locale="$(pick_locale)"
    echo "Initializing Postgres data directory at $DB_DATA_DIR"
    run_as_postgres "initdb --locale \"$initdb_locale\" --encoding \"$DB_ENCODING\" -D \"$DB_DATA_DIR\""
  fi

  if command -v systemctl >/dev/null 2>&1; then
    systemctl start postgresql || true
  fi

  if ! pg_isready -h "$DB_HOST" -p "$DB_PORT" >/dev/null 2>&1; then
    if command -v pg_ctl >/dev/null 2>&1; then
      run_as_postgres "pg_ctl -D \"$DB_DATA_DIR\" -l \"$DB_DATA_DIR/postgresql.log\" start"
    fi
  fi

  for _ in {1..30}; do
    if pg_isready -h "$DB_HOST" -p "$DB_PORT" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  echo "Postgres did not become ready at $DB_HOST:$DB_PORT"
  return 1
}

ensure_role_and_db() {
  if ! is_local_host; then
    return 0
  fi

  local admin_conn="host=$DB_HOST port=$DB_PORT dbname=postgres user=$DB_USER sslmode=$DB_SSLMODE channel_binding=$DB_CHANNEL_BINDING"
  if ! psql "$admin_conn" -c "select 1" >/dev/null 2>&1; then
    admin_conn=""
  fi

  if [ -n "$admin_conn" ]; then
    ROLE_EXISTS="$(psql "$admin_conn" -qtAX -c "select 1 from pg_roles where rolname = '$DB_USER';")"
    if [ "$ROLE_EXISTS" != "1" ]; then
      psql "$admin_conn" -v ON_ERROR_STOP=1 -c "create role \"$DB_USER\" login password '$DB_PASSWORD';" || admin_conn=""
    fi

    DB_EXISTS="$(psql "$admin_conn" -qtAX -c "select 1 from pg_database where datname = '$DB_NAME';")"
    if [ "$DB_EXISTS" != "1" ]; then
      psql "$admin_conn" -v ON_ERROR_STOP=1 -c "create database \"$DB_NAME\" owner \"$DB_USER\";" || admin_conn=""
    fi
  fi

  if [ -z "$admin_conn" ]; then
    ROLE_EXISTS="$(run_as_postgres "psql -qtAX -c \"select 1 from pg_roles where rolname = '$DB_USER';\"")" || ROLE_EXISTS=""
    if [ "$ROLE_EXISTS" != "1" ]; then
      run_as_postgres "psql -v ON_ERROR_STOP=1 -c \"create role \\\"$DB_USER\\\" login password '$DB_PASSWORD';\""
    fi

    DB_EXISTS="$(run_as_postgres "psql -qtAX -c \"select 1 from pg_database where datname = '$DB_NAME';\"")"
    if [ "$DB_EXISTS" != "1" ]; then
      run_as_postgres "psql -v ON_ERROR_STOP=1 -c \"create database \\\"$DB_NAME\\\" owner \\\"$DB_USER\\\";\""
    fi
  fi
}

ensure_postgres_running
adjust_ssl_for_local
ensure_role_and_db
PSQL_CONN="host=$DB_HOST port=$DB_PORT dbname=$DB_NAME user=$DB_USER sslmode=$DB_SSLMODE channel_binding=$DB_CHANNEL_BINDING"

psql "$PSQL_CONN" -c "select current_database(), current_user;"

psql "$PSQL_CONN" -f "$(dirname "$0")/00_ddl.sql"
psql "$PSQL_CONN" -f "$(dirname "$0")/01_seed_users.sql"
psql "$PSQL_CONN" -f "$(dirname "$0")/02_seed_workflows.sql"
psql "$PSQL_CONN" -f "$(dirname "$0")/03_seed_triggers.sql"
psql "$PSQL_CONN" -f "$(dirname "$0")/04_seed_executions.sql"

psql "$PSQL_CONN" -c "select 'users' as table, count(*) from auth.users union all select 'workflows', count(*) from workflow.workflows union all select 'triggers', count(*) from trigger_service.trigger_registrations union all select 'executions', count(*) from orchestrator.workflow_executions;"
