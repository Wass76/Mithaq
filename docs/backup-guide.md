# Database Backup Guide

Scope: dockerized PostgreSQL service `mithaq-db` on Ubuntu. Dumps are stored locally under `backups/db` (git-ignored) and rotated automatically. Attachments under `storage/complaints` should be archived separately.

## Manual backup
```bash
# from repo root
./scripts/db-backup.sh
```
Environment knobs:
- `COMPOSE_CMD` (default `docker compose`)
- `DB_SERVICE` (default `mithaq-db`)
- `BACKUP_DIR` (default `./backups/db`)
- `BACKUP_RETENTION_DAYS` (default `7`)

The script uses the DB container’s `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` env vars; nothing sensitive is read from the host.

## Schedule (cron)
Installed on server (daily at 00:00 server time):
```
0 0 * * * cd /home/wassem/Mithaq && COMPOSE_CMD="/usr/bin/docker compose" BACKUP_RETENTION_DAYS=14 ./scripts/db-backup.sh >> /var/log/shakwa-backup.log 2>&1
```
Use absolute paths to avoid PATH issues inside cron. Logs go to `/var/log/shakwa-backup.log`.

## Restore (custom-format dump)
```bash
# choose a dump from backups/db
DUMP=backups/db/shakwa_YYYYMMDDTHHMMSSZ.dump
cat "$DUMP" | docker compose exec -T mithaq-db sh -c '
  set -e
  : "${POSTGRES_DB:=mithaq}"
  : "${POSTGRES_USER:=postgres}"
  : "${POSTGRES_PASSWORD:?need POSTGRES_PASSWORD in container}"
  export PGPASSWORD="$POSTGRES_PASSWORD"
  pg_restore -c -U "$POSTGRES_USER" -d "$POSTGRES_DB"
'
```
- `-c` drops and recreates objects; omit if you need a safer merge.
- To sanity-check a dump without restoring: `pg_restore --list "$DUMP"`.

## File attachments
The complaint files live under `storage/complaints`. Back them up alongside the DB:
```bash
mkdir -p backups/files
tar czf backups/files/shakwa_files_$(date -u +%Y%m%dT%H%M%SZ).tar.gz storage/complaints
```
Apply your own retention policy (e.g., delete older than 30 days).

## Quick validation
- After the first backup, run `pg_restore --list` to ensure the dump is readable.
- Periodically restore into a disposable DB instance to verify recoverability.

