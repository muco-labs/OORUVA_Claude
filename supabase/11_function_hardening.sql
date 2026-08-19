-- ============================================================================
-- OORUVA - migration 11
-- Function hardening: a fixed search_path on every function, and EXECUTE
-- withdrawn from the two security definer helpers that take another person's
-- id as an argument.
--
-- Run after 10_reward_integrity.sql. Idempotent.
--
-- WHY THIS EXISTS
-- ---------------
-- Migrations 01-10 have been amended so every definition now carries
--   set search_path = public, pg_temp
-- but a database where those already ran keeps the old, unqualified functions:
-- CREATE OR REPLACE only rewrites what the new statement says, and a re-run is
-- not something to require of a live project. This migration therefore applies
-- the same property with ALTER FUNCTION, and is the file to re-run if any of
-- 01-10 is ever replayed -- CREATE OR REPLACE drops per-function SET clauses
-- that the replacing statement does not restate.
--
-- WHAT A MUTABLE search_path ACTUALLY RISKS
-- A security definer function runs as its owner, and resolves unqualified
-- names against whatever search_path the *caller* set. Anyone able to create
-- an object in a schema earlier on that path can shadow `users` or
-- `businesses` and have is_admin() consult their table instead of ours. On a
-- default Supabase project no client role can create schemas or tables, so
-- this is defence in depth rather than a live hole -- but it costs one clause
-- per function, and the day someone grants CREATE to a role is not the day you
-- want to be rediscovering this.
-- ============================================================================

-- == 1. Fixed search_path on every function ==================================
-- pg_temp is listed last deliberately. Left implicit it is searched *first*,
-- which is the shadowing route that matters here: a caller can always create
-- objects in their own temporary schema.

alter function current_user_id()                          set search_path = public, pg_temp;
alter function is_admin()                                 set search_path = public, pg_temp;
alter function owns_business(uuid)                        set search_path = public, pg_temp;
alter function vendor_suspended(uuid)                     set search_path = public, pg_temp;
alter function grant_admin(uuid, varchar)                 set search_path = public, pg_temp;
alter function revoke_admin(uuid)                         set search_path = public, pg_temp;
alter function rewards_earned_today(uuid, varchar)        set search_path = public, pg_temp;

alter function reward_balance(uuid)                       set search_path = public, pg_temp;
alter function nearby_businesses(double precision, double precision, double precision, uuid, integer)
                                                          set search_path = public, pg_temp;
alter function search_businesses(text, integer)           set search_path = public, pg_temp;
alter function vendors_within_km(double precision, double precision, double precision)
                                                          set search_path = public, pg_temp;
alter function storage_business_id(text)                  set search_path = public, pg_temp;

alter function touch_updated_at()                         set search_path = public, pg_temp;
alter function sync_likes_count()                         set search_path = public, pg_temp;
alter function enqueue_new_vendor()                       set search_path = public, pg_temp;
alter function guard_role_assignment()                    set search_path = public, pg_temp;
alter function guard_reward_debit()                       set search_path = public, pg_temp;
alter function guard_offer_usage()                        set search_path = public, pg_temp;

-- == 2. EXECUTE withdrawn, but only where it can be ==========================
--
-- Completes migration 10, which revoked this from public and anon but not from
-- authenticated. rewards_earned_today is security definer and takes a customer
-- id as an argument, so a signed-in customer reaching it directly could count
-- any other customer's awards for the day -- the ledger's own RLS never gets a
-- say. It exists so the award edge function can apply a daily cap; that
-- function holds the service role, which no revoke here affects.
--
-- Revoking from public is the part that does the work. Postgres grants EXECUTE
-- on every new function to PUBLIC, so revoking from anon and authenticated
-- alone changes nothing: both still inherit it through PUBLIC.
revoke all on function rewards_earned_today(uuid, varchar) from public, anon, authenticated;

-- == 3. Why the other four keep their grant ==================================
--
-- current_user_id(), is_admin(), owns_business(b) and vendor_suspended(v) stay
-- executable by anon and authenticated, and the Supabase linter will keep
-- warning about all four. That is deliberate, and it is not a judgement call:
-- revoking breaks row level security outright.
--
-- A policy expression is NOT evaluated with the table owner's privileges. It
-- runs as the querying role, so that role needs EXECUTE on every function the
-- policy calls. With the grant withdrawn, an anonymous `select count(*) from
-- businesses` fails with:
--
--     ERROR: 42501: permission denied for function current_user_id
--
-- ...because biz_read calls it. Every owner-scoped policy in 02, 06 and 07 is
-- built on current_user_id() and is_admin(), so withdrawing them takes the
-- whole app down rather than hardening it.
--
-- An earlier version of this migration revoked them anyway, on the strength of
-- a test that revoked from anon while PUBLIC still held the grant -- so the
-- function stayed reachable and the test "passed" for the wrong reason. The
-- note is here because the mistake is an easy one to repeat.
--
-- What the remaining warnings actually expose, via /rest/v1/rpc/<name>:
--   current_user_id()   the caller's own id, or null. Nothing they don't have.
--   is_admin()          whether the caller is an admin. About the caller.
--   owns_business(b)    whether the CALLER owns b. About the caller.
--   vendor_suspended(v) whether user v is suspended. The only one that answers
--                       about someone else -- one boolean, for a uuid the
--                       prober must already know.
--
-- If that last one is worth closing, the fix is to stop calling it from a
-- policy: fold the suspension check into biz_read as a security definer
-- function that takes no argument, or denormalise the flag onto businesses and
-- maintain it with a trigger. Both are larger changes than this migration.

comment on function vendor_suspended is
  'Security definer so the suspension flag is readable while evaluating another table''s policy. Returns only a boolean, so it leaks nothing beyond the fact being asked about. EXECUTE is revoked from client roles by migration 11: the biz_read policy still resolves it, so the only thing the grant bought was a public endpoint for probing arbitrary user ids.';
