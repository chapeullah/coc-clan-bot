# Repair the shifted war from 19–21 October 2026

The application now identifies a regular war by the two clan tags and
`preparation_start_time`. Start/end times are updated from subsequent API responses.
The old database contains two rows for that identity, so repair it **before pushing
the new application to `main`**, which triggers deployment.

`repair_shifted_war.sql` is deliberately limited to the inspected rows: merge
`wars.id = 2` into `wars.id = 3`, using the final summary from `war_log.id = 61`.
It validates their identity, preserves distinct attacks and their participant links,
keeps sent notification markers, removes the duplicate, and replaces
`uk_wars_clans_start` with `uk_wars_clans_preparation` in one transaction.
If the legacy `telegram_war_attack_notification` table still exists, its attack
links are migrated too. Duplicate notifications retain a sent row in preference
to an unsent row; otherwise the target row is retained. Unique attack and
notification IDs are preserved. Different destinations on duplicate attack
notifications stop the repair for inspection. The table itself is retained.
Conflicting attacks, unknown foreign keys, incomplete final data or an unexpected
constraint definition abort the transaction. Repeating a completed repair is safe.
The latest observed start time is retained; no historical start time is invented.

1. Prepare the code commit locally, without pushing yet.
2. Copy the SQL file from the local checkout to the server. Replace `VPS_HOST` with
   the address used for SSH:

   ```powershell
   scp ./sql/repair_shifted_war.sql root@VPS_HOST:/root/repair_shifted_war.sql
   ```

3. In the VPS SSH session, run:

   ```bash
   bash -euo pipefail <<'REPAIR'
   cd /opt/coc-clan-bot
   umask 077
   docker compose stop -t 120 app
   backup=$(mktemp /root/coc-war-before-repair-XXXXXXXX.dump)
   docker compose exec -T db pg_dump -U gucci -d gucci -Fc > "$backup"
   test -s "$backup"
   printf 'Database backup: %s\n' "$backup"
   docker compose exec -T db psql -X -v ON_ERROR_STOP=1 -U gucci -d gucci \
       < /root/repair_shifted_war.sql
   REPAIR
   ```

   The database stays running. The application remains stopped until deployment.
   If the command fails, retain the dump and error output and inspect the failure
   before continuing with deployment.

4. After successful repair, push the prepared code and let the existing workflow
   deploy it. The Compose project remains `coc-clan-bot`.
5. Check `docker compose ps` and `/wars`: there should be one war against
   `#R8QQUQVG`, a tie at 60:60, ending at `2026-10-21 09:27:26 UTC`
   (`12:27:26 Europe/Moscow`).

The final summary reports 33 home-clan attacks. If an individual attack was never
captured, this repair does not reconstruct it from aggregates. The existing rating
completeness checks continue to exclude incomplete individual history.

## Verification

`mvnw.cmd test` runs the unit and H2 persistence regression tests.
The SQL repair tests also run when `war.repair.jdbcUrl` points to an **isolated**
PostgreSQL database named `codex_war_repair_test`, with user `postgres` and local
test authentication. They replace that database's public schema with fixtures.
Never use a database containing application or other valuable data for these tests.

```powershell
./mvnw.cmd -B -ntp verify '-Dwar.repair.jdbcUrl=jdbc:postgresql://127.0.0.1:55440/codex_war_repair_test'
```

The SQL repair itself is not run automatically during application startup or deployment.

## Retrying after an error in pgAdmin

Run `ROLLBACK;` in the same Query Tool tab to close the failed transaction and
release its locks. Replace the previous script in full with the current
`repair_shifted_war.sql`, then execute the entire file, including `BEGIN` and
`COMMIT`, with the application stopped and the fresh dump retained. Do not just
remove the foreign-key check: a legacy notification may reference an attack that
is about to be merged. Unexpected foreign-key errors now include the table,
constraint name and definition in `DETAIL`.
