-- ============================================================================
-- LOCAL TEST SHIM — not part of the production migration set.
--
-- Supabase provides auth.uid(), the storage schema and a request.jwt.claim.role
-- setting. A plain PostgreSQL instance does not. This file recreates just
-- enough of that surface so the real RLS policies can be exercised offline.
--
-- Never run this against a Supabase project: it would shadow the genuine auth
-- schema. The runner applies it only to the local test database.
-- ============================================================================

create schema if not exists auth;
create schema if not exists storage;

-- The signed-in subject, swapped per test with set_local.
create or replace function auth.uid() returns uuid as $fn$
  select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid;
$fn$ language sql stable;

-- Storage tables, shaped like Supabase's own so the bucket policies parse.
create table if not exists storage.buckets (
  id     text primary key,
  name   text not null,
  public boolean not null default false
);

create table if not exists storage.objects (
  id        uuid primary key default gen_random_uuid(),
  bucket_id text references storage.buckets(id),
  name      text,
  owner     uuid,
  created_at timestamptz default now()
);

alter table storage.objects enable row level security;

-- ── Test helpers ───────────────────────────────────────────────────────────

-- Become a given OORUVA user for the remainder of the transaction.
create or replace function test_as(user_uid uuid) returns void as $fn$
begin
  perform set_config('request.jwt.claim.sub', user_uid::text, true);
  perform set_config('request.jwt.claim.role', 'authenticated', true);
  perform set_config('role', 'ooruva_client', true);
end;
$fn$ language plpgsql;

-- Become an anonymous caller.
create or replace function test_as_anon() returns void as $fn$
begin
  perform set_config('request.jwt.claim.sub', '', true);
  perform set_config('request.jwt.claim.role', 'anon', true);
end;
$fn$ language plpgsql;

-- A non-superuser role, so RLS is actually applied. Superusers bypass it,
-- which is the classic way an RLS test suite passes while proving nothing.
do $fn$
begin
  if not exists (select 1 from pg_roles where rolname = 'ooruva_client') then
    create role ooruva_client nologin;
  end if;
end;
$fn$;

-- ── Assertions ─────────────────────────────────────────────────────────────

create table if not exists test_results (
  id        serial primary key,
  name      text not null,
  passed    boolean not null,
  detail    text,
  run_at    timestamptz not null default now()
);

/** Records a pass or fail rather than aborting, so one run reports everything. */
create or replace function expect(test_name text, condition boolean, detail text default null)
returns void as $fn$
begin
  insert into test_results (name, passed, detail) values (test_name, condition, detail);
  raise notice '%  %', case when condition then 'PASS' else 'FAIL' end, test_name;
end;
$fn$ language plpgsql;

/** Asserts that a statement is refused — the negative case that matters most. */
create or replace function expect_denied(test_name text, stmt text) returns void as $fn$
declare
  affected integer;
begin
  begin
    execute stmt;
    get diagnostics affected = row_count;
    -- RLS refuses silently on write by returning zero rows; both count as denied.
    perform expect(test_name, affected = 0,
      case when affected = 0 then null
           else affected::text || ' row(s) unexpectedly written' end);
  exception
    when insufficient_privilege or check_violation or raise_exception then
      perform expect(test_name, true, 'refused: ' || sqlerrm);
  end;
end;
$fn$ language plpgsql;

/** Asserts a select returns exactly the expected number of visible rows. */
create or replace function expect_visible(test_name text, stmt text, expected integer)
returns void as $fn$
declare
  actual integer;
begin
  execute 'select count(*) from (' || stmt || ') q' into actual;
  perform expect(test_name, actual = expected,
    'expected ' || expected || ', saw ' || actual);
end;
$fn$ language plpgsql;
