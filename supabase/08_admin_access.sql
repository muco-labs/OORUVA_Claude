-- ============================================================================
-- OORUVA - migration 08
-- Admin access for the web console.
--
-- Run after 07_identity_and_model.sql. Idempotent.
--
-- HOW ADMIN SIGN-IN WORKS
-- -----------------------
-- The mobile apps authenticate with Firebase phone OTP and have the
-- auth-bootstrap edge function mint a Supabase session whose `sub` is
-- users.id. A browser has no SIM, so the console uses Supabase's own email +
-- password sign-in instead.
--
-- Both paths have to end up with the same thing — a JWT whose `sub` is the
-- OORUVA users.id — or is_admin() would need a second code path and every
-- policy would have to know which kind of caller it was looking at. They
-- converge because an admin's users row is created with its id set to the
-- Supabase auth user id. auth.uid() then equals users.id for free.
--
-- CREATING AN ADMIN (operator step, not something the app can do)
-- ---------------------------------------------------------------
--  1. Supabase dashboard > Authentication > Users > Add user.
--     Use a real address and a strong password from a password manager.
--     Copy the generated user UID.
--  2. Run, with that UID and the same address:
--
--        select grant_admin('00000000-0000-0000-0000-000000000000', '+919000000000');
--
--     The phone is stored because users.phone is NOT NULL and is how support
--     reaches an admin; it is not used to sign in to the console.
--
--  3. Confirm:  select id, role from users where role = 'admin';
--
-- There is deliberately no self-service path. `roles.self_assignable` is false
-- for admin, the guard trigger from migration 04 enforces it, and the
-- auth-bootstrap function refuses to mint the role. This function is the only
-- way in, and it requires database access that no client key grants.
-- ============================================================================

create or replace function grant_admin(
  auth_user_id uuid,
  contact_phone varchar
) returns uuid as $fn$
declare
  existing_role varchar;
begin
  select role into existing_role from users where id = auth_user_id;

  if existing_role is not null then
    if existing_role = 'admin' then
      raise notice 'User % is already an admin', auth_user_id;
      return auth_user_id;
    end if;
    -- Refusing rather than promoting. Turning a live customer or vendor row
    -- into an admin would leave their businesses, reviews and reward ledger
    -- attached to an administrator account, which is not something to do by
    -- accident from a console.
    raise exception 'User % already exists with role %', auth_user_id, existing_role
      using hint = 'Create a separate auth user for administration.';
  end if;

  -- The id is supplied, not generated: it has to match the Supabase auth user
  -- so that auth.uid() resolves to this row.
  insert into users (id, phone, role, firebase_uid)
  values (auth_user_id, contact_phone, 'admin', null);

  insert into audit_log (actor_id, action, entity, entity_id, notes)
  values (auth_user_id, 'admin_granted', 'users', auth_user_id,
          'Granted via grant_admin()');

  return auth_user_id;
end;
$fn$ language plpgsql security definer set search_path = public, pg_temp;

comment on function grant_admin is
  'Operator-only. Links a Supabase auth user to an OORUVA admin row. Requires direct database access -- neither the anon key nor any client session can reach it, because the role guard trigger refuses a self-assigned admin.';

revoke all on function grant_admin(uuid, varchar) from public;
revoke all on function grant_admin(uuid, varchar) from anon;
revoke all on function grant_admin(uuid, varchar) from authenticated;

-- Withdrawing admin. Kept alongside granting so the pair is obvious: an
-- offboarding step that has to be reconstructed from memory tends not to
-- happen at all.
create or replace function revoke_admin(target uuid) returns void as $fn$
begin
  update users set role = 'customer' where id = target and role = 'admin';

  insert into audit_log (actor_id, action, entity, entity_id, notes)
  values (target, 'admin_revoked', 'users', target, 'Revoked via revoke_admin()');
end;
$fn$ language plpgsql security definer set search_path = public, pg_temp;

revoke all on function revoke_admin(uuid) from public;
revoke all on function revoke_admin(uuid) from anon;
revoke all on function revoke_admin(uuid) from authenticated;
