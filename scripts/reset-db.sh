#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SCHEMA_SQL="${PROJECT_ROOT}/sql/schema.sql"
SEED_SQL="${PROJECT_ROOT}/sql/seed.sql"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-12345678}"
DB_NAME="${DB_NAME:-ranking_system}"

if ! command -v mysql >/dev/null 2>&1; then
  echo "Error: mysql command not found. Please install MySQL client first."
  exit 1
fi

if [[ ! -f "${SCHEMA_SQL}" || ! -f "${SEED_SQL}" ]]; then
  echo "Error: schema.sql or seed.sql not found."
  exit 1
fi

echo "Resetting database '${DB_NAME}' on ${DB_HOST}:${DB_PORT} ..."
MYSQL_PWD="${DB_PASS}" mysql \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --protocol=TCP \
  --default-character-set=utf8mb4 \
  -e "DROP DATABASE IF EXISTS \`${DB_NAME}\`;"

echo "Running schema.sql ..."
MYSQL_PWD="${DB_PASS}" mysql \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --protocol=TCP \
  --default-character-set=utf8mb4 \
  < "${SCHEMA_SQL}"

echo "Running seed.sql ..."
MYSQL_PWD="${DB_PASS}" mysql \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --protocol=TCP \
  --default-character-set=utf8mb4 \
  < "${SEED_SQL}"

echo "Done. Database '${DB_NAME}' has been refreshed."
