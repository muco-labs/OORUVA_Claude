#!/usr/bin/env bash
# ============================================================================
# OORUVA — run the migrations and the role-security suite against a throwaway
# local PostgreSQL. No credentials, no cloud project, no production data.
#
#   PGROOT=/d/pgtest/pgsql ./supabase/tests/run_tests.sh
#
# The database is dropped and rebuilt on every run, so a pass means the
# migrations apply cleanly from nothing — not that they happened to work once.
# ============================================================================
set -uo pipefail

PGROOT="${PGROOT:-/d/pgtest/pgsql}"
PGDATA="${PGDATA:-/d/pgtest/data}"
PGPORT="${PGPORT:-55432}"
DB="ooruva_test"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

BIN="$PGROOT/bin"
export PATH="$BIN:$PATH"

command -v postgres >/dev/null || { echo "postgres not found under $BIN"; exit 1; }

# ── Cluster ────────────────────────────────────────────────────────────────
if [ ! -f "$PGDATA/PG_VERSION" ]; then
  echo "== initdb"
  mkdir -p "$PGDATA"
  initdb -D "$PGDATA" -U postgres --auth=trust --encoding=UTF8 >/dev/null || exit 1
fi

if ! pg_isready -p "$PGPORT" -q 2>/dev/null; then
  echo "== starting postgres on :$PGPORT"
  pg_ctl -D "$PGDATA" -o "-p $PGPORT -c listen_addresses=127.0.0.1" -l "$PGDATA/server.log" -w start \
    || { tail -20 "$PGDATA/server.log"; exit 1; }
fi

PSQL="psql -h 127.0.0.1 -p $PGPORT -U postgres -v ON_ERROR_STOP=1 -q"

# ── Rebuild from nothing ───────────────────────────────────────────────────
echo "== rebuilding $DB"
$PSQL -d postgres -c "drop database if exists $DB;" >/dev/null
$PSQL -d postgres -c "create database $DB;" >/dev/null

fail=0
apply() {
  printf '   %-38s ' "$(basename "$1")"
  if out=$($PSQL -d "$DB" -f "$1" 2>&1); then
    echo "ok"
  else
    echo "FAILED"
    echo "$out" | grep -E "ERROR|LINE" | head -5 | sed 's/^/      /'
    fail=1
  fi
}

# The shim goes first: 02_rls.sql defines current_user_id() whose body
# references auth.uid(), and a SQL function body is parsed at creation time.
# Supabase already has that schema; a bare PostgreSQL does not.
echo "== local shim (test only, never run against Supabase)"
apply "$HERE/supabase/tests/00_local_shim.sql"

echo "== migrations"
for f in 01_schema 02_rls 04_taxonomy_and_foundation 05_taxonomy_seed 06_rls_foundation; do
  apply "$HERE/supabase/$f.sql"
done

[ "$fail" -eq 0 ] || { echo; echo "migrations did not apply cleanly — stopping"; exit 1; }

# -- Tests ------------------------------------------------------------------
# Each block runs inside a transaction that is rolled back, so the assertions
# cannot be counted from a table: the rollback discards those rows too. The
# NOTICE stream is the record of truth.
echo "== role security suite"
out=$($PSQL -d "$DB" -f "$HERE/supabase/tests/01_role_security.sql" 2>&1)

echo "$out" | grep -oE "(PASS|FAIL)  .*" | sed 's/^/   /'

passed=$(echo "$out" | grep -c "NOTICE:  PASS")
failed=$(echo "$out" | grep -c "NOTICE:  FAIL")
errors=$(echo "$out" | grep -c "^psql:.*ERROR:")

echo
echo "== summary"
echo "   passed=$passed  failed=$failed  sql_errors=$errors"

if [ "$failed" -gt 0 ] || [ "$errors" -gt 0 ]; then
  echo
  echo "$out" | grep -E "NOTICE:  FAIL|^psql:.*ERROR:" | head -20 | sed 's/^/   /'
  exit 1
fi
exit 0
