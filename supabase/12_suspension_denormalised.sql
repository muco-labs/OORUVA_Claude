-- ============================================================================
-- OORUVA - migration 12
-- Takes vendor_suspended() off the policy path so its EXECUTE grant can go.
--
-- Run after 11_function_hardening.sql. Idempotent.
--
-- WHY THIS EXISTS
-- ---------------
-- biz_read called vendor_suspended(v), a security definer function that reports
-- whether an arbitrary user id is suspended. A policy expression is evaluated
-- as the querying role, so anon and authenticated had to hold EXECUTE on it --
-- which also exposed /rest/v1/rpc/vendor_suspended, where anyone could probe
-- any user id they knew for suspension status.
--
-- The grant could not simply be revoked: migration 11 tried, and every
-- owner-scoped select failed with "permission denied for function". The fix is
-- to stop needing the call. The flag is denormalised onto businesses and kept
-- true by triggers, so the policy reads a plain column and the function becomes
-- unreferenced.
--
-- WHY A COLUMN IS SAFE HERE
-- Denormalisation is a correctness risk when the copy can drift. This one
-- cannot be written by a client: a BEFORE trigger overwrites whatever was
-- submitted with the value from users, on every insert and every update. The
-- only writer is the AFTER trigger on users.suspended.
-- ============================================================================

alter table businesses
  add column if not exists vendor_suspended boolean not null default false;

comment on column businesses.vendor_suspended is
  'Mirror of users.suspended for this business''s owner, maintained by trigger. Exists so biz_read can filter on a column instead of calling a security definer function, which would require granting every client EXECUTE on it. Never client-writable: trg_business_suspension_stamp overwrites it.';

-- Backfill for any rows created before this migration.
update businesses b
   set vendor_suspended = u.suspended
  from users u
 where u.id = b.vendor_id
   and b.vendor_suspended is distinct from u.suspended;

-- == Suspending a user hides their businesses =================================
create or replace function sync_business_vendor_suspension() returns trigger as $fn$
begin
  update businesses
     set vendor_suspended = new.suspended
   where vendor_id = new.id
     and vendor_suspended is distinct from new.suspended;
  return null;
end;
$fn$ language plpgsql set search_path = public, pg_temp;

drop trigger if exists trg_user_suspension_sync on users;
create trigger trg_user_suspension_sync
  after update of suspended on users
  for each row when (old.suspended is distinct from new.suspended)
  execute function sync_business_vendor_suspension();

-- == The column is stamped, never supplied ===================================
-- Security definer because it reads users.suspended while running as whoever
-- is inserting the business, and users' own RLS shows a caller only their own
-- row. Without it the subquery finds nothing, coalesces to false, and a
-- suspended vendor's new listing would go straight back onto the map.
create or replace function set_business_vendor_suspension() returns trigger as $fn$
begin
  new.vendor_suspended := coalesce(
    (select suspended from users where id = new.vendor_id), false
  );
  return new;
end;
$fn$ language plpgsql security definer set search_path = public, pg_temp;

drop trigger if exists trg_business_suspension_stamp on businesses;
create trigger trg_business_suspension_stamp
  before insert or update of vendor_id, vendor_suspended on businesses
  for each row execute function set_business_vendor_suspension();

-- Trigger functions are not reachable through PostgREST, but the grant is
-- there by default and costs nothing to withdraw.
revoke all on function sync_business_vendor_suspension() from public, anon, authenticated;
revoke all on function set_business_vendor_suspension() from public, anon, authenticated;

-- == The policy, with no function call in it =================================
drop policy if exists biz_read on businesses;
create policy biz_read on businesses
  for select using (
    (status = 'verified' and not vendor_suspended)
    or vendor_id = current_user_id()
    or is_admin()
  );

comment on policy biz_read on businesses is
  'Reads the denormalised vendor_suspended column rather than calling vendor_suspended(), so no client needs EXECUTE on a security definer function to see a listing. See migration 12.';

-- == And now the grant can go ================================================
-- Nothing references vendor_suspended() any more. It is kept rather than
-- dropped so a replay of migration 07 does not fail, but no client role can
-- reach it.
revoke all on function vendor_suspended(uuid) from public, anon, authenticated;

comment on function vendor_suspended is
  'Superseded by businesses.vendor_suspended in migration 12, which removed the last caller. EXECUTE is revoked from every client role: while this was on the biz_read path it had to be granted, which made /rest/v1/rpc/vendor_suspended a public oracle for probing whether a given user id is suspended.';
