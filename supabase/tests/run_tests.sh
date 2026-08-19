#!/usr/bin/env bash
# ============================================================================
# OORUVA — run the migrations and the security suites against a throwaway
# PostgreSQL. No credentials, no cloud project, no production data.
#
# Local, managing its own cluster:
#   PGROOT=/d/pgtest/pgsql ./supabase/tests/run_tests.sh
#
# Against a Postgres that already exists (CI service container):
#   PGHOST=localhost PGPORT=5432 PGUSER=postgres PGPASSWORD=postgres \
#     ./supabase/tests/run_tests.sh
#
# Every suite gets a database built from nothing, so a pass means the
# migrations apply cleanly from empty — not that they happened to work once.
# ============================================================================
set -uo pipefail

DB="ooruva_test"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# An externally supplied PGHOST means someone else owns the server -- CI, or a
# developer pointing at their own instance. In that case do not try to initdb
# or start anything; just connect.
EXTERNAL_PG="${PGHOST:-}"

if [ -n "$EXTERNAL_PG" ]; then
  PGPORT="${PGPORT:-5432}"
  PGUSER="${PGUSER:-postgres}"
  export PGPASSWORD="${PGPASSWORD:-}"
  PSQL="psql -h $PGHOST -p $PGPORT -U $PGUSER -v ON_ERROR_STOP=1 -q"
  # failures.log is written next to the suites when there is no PGDATA to own it.
  PGDATA="${PGDATA:-$HERE/supabase/tests}"
else
  PGROOT="${PGROOT:-/d/pgtest/pgsql}"
  PGDATA="${PGDATA:-/d/pgtest/data}"
  PGPORT="${PGPORT:-55432}"

  BIN="$PGROOT/bin"
  export PATH="$BIN:$PATH"

  command -v postgres >/dev/null || { echo "postgres not found under $BIN"; exit 1; }

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
fi

# ── Rebuild from nothing ───────────────────────────────────────────────────
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

MIGRATIONS="01_schema 02_rls 04_taxonomy_and_foundation 05_taxonomy_seed 06_rls_foundation 07_identity_and_model 08_admin_access 09_storage_and_search 10_reward_integrity"

# Each suite gets a database built from nothing. Sharing one database between
# suites let suite 01's fixtures win an `on conflict do nothing` in suite 02 and
# silently changed what suite 02 was testing -- the assertions still ran, but
# against rows suite 02 had not written. Isolation is cheaper than that class of
# false result.
rebuild() {
  $PSQL -d postgres -c "drop database if exists $DB;" >/dev/null
  $PSQL -d postgres -c "create database $DB;" >/dev/null

  # The shim goes first: 02_rls.sql defines current_user_id() whose body
  # references auth.uid(), and a SQL function body is parsed at creation time.
  # Supabase already has that schema; a bare PostgreSQL does not.
  apply "$HERE/supabase/tests/00_local_shim.sql"
  for f in $MIGRATIONS; do
    apply "$HERE/supabase/$f.sql"
  done
}

total_passed=0
total_failed=0
total_errors=0
: > "$PGDATA/failures.log"

run_suite() {
  local label="$1" file="$2"

  echo "== rebuilding $DB for $label"
  rebuild
  [ "$fail" -eq 0 ] || { echo; echo "migrations did not apply cleanly -- stopping"; exit 1; }

  echo "== $label"
  local out
  out=$($PSQL -d "$DB" -f "$file" 2>&1)

  echo "$out" | grep -oE "(PASS|FAIL)  .*" | sed 's/^/   /'

  total_passed=$(( total_passed + $(echo "$out" | grep -c "NOTICE:  PASS") ))
  total_failed=$(( total_failed + $(echo "$out" | grep -c "NOTICE:  FAIL") ))
  total_errors=$(( total_errors + $(echo "$out" | grep -c "^psql:.*ERROR:") ))

  echo "$out" | grep -E "NOTICE:  FAIL|^psql:.*ERROR:" >> "$PGDATA/failures.log"
  echo
}

run_suite "role security suite"  "$HERE/supabase/tests/01_role_security.sql"
run_suite "model security suite" "$HERE/supabase/tests/02_model_security.sql"
run_suite "admin access suite"  "$HERE/supabase/tests/03_admin_access.sql"
run_suite "storage and search suite" "$HERE/supabase/tests/04_storage_and_search.sql"
run_suite "reward integrity suite" "$HERE/supabase/tests/05_reward_integrity.sql"

echo "== summary"
echo "   passed=$total_passed  failed=$total_failed  sql_errors=$total_errors"

if [ "$total_failed" -gt 0 ] || [ "$total_errors" -gt 0 ]; then
  echo
  head -20 "$PGDATA/failures.log" | sed 's/^/   /'
  exit 1
fi
exit 0
