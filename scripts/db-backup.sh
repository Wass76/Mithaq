#!/usr/bin/env bash
set -euo pipefail

# Simple PostgreSQL backup runner for the dockerized DB.
# Requirements:
# - docker compose (v2) available as `docker compose` (override with COMPOSE_CMD)
# - DB service reachable as DB_SERVICE (defaults to mithaq-db)
# - The DB container must expose POSTGRES_DB/POSTGRES_USER/POSTGRES_PASSWORD envs

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_CMD="${COMPOSE_CMD:-docker compose}"
DB_SERVICE="${DB_SERVICE:-mithaq-db}"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups/db}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_FILE="$BACKUP_DIR/shakwa_${TIMESTAMP}.dump"

mkdir -p "$BACKUP_DIR"

echo "Starting DB backup from service '$DB_SERVICE' into $BACKUP_FILE"

$COMPOSE_CMD exec -T "$DB_SERVICE" sh -c '
  set -euo pipefail
  : "${POSTGRES_DB:=mithaq}"
  : "${POSTGRES_USER:=postgres}"
  : "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be set in the DB container env}"

  export PGPASSWORD="$POSTGRES_PASSWORD"
  pg_dump \
    -h localhost \
    -U "$POSTGRES_USER" \
    -d "$POSTGRES_DB" \
    -F c \
    -Z 6 \
    --no-owner \
    --no-privileges
' > "$BACKUP_FILE"

find "$BACKUP_DIR" -type f -name 'shakwa_*.dump' -mtime +"$RETENTION_DAYS" -print -delete || true

echo "Backup completed: $BACKUP_FILE"

