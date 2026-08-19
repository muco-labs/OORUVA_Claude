-- ============================================================================
-- OORUVA — admin access tests
--
-- grant_admin() is the only way into the admin role. It is security definer,
-- which means that if the EXECUTE grant were wrong, any signed-in customer
-- could call it and make themselves an administrator. These tests exist to
-- catch exactly that.
--
-- Run with:  supabase/tests/run_tests.sh
-- ============================================================================

set client_min_messages to notice;
truncate test_results;

insert into users (id, phone, role, firebase_uid) values
  ('aaaaaaaa-0000-0000-0000-000000000001', '+919000000001', 'customer', 'fb-customer-alpha'),
  ('bbbbbbbb-0000-0000-0000-000000000002', '+919000000002', 'vendor',   'fb-vendor-bravo'),
  ('dddddddd-0000-0000-0000-000000000004', '+919000000004', 'admin',    null)
on conflict (id) do nothing;

grant usage on schema public to ooruva_client;
grant select, insert, update, delete on all tables in schema public to ooruva_client;
grant usage, select on all sequences in schema public to ooruva_client;

-- ===========================================================================
-- CUSTOMER — must not be able to reach the admin-granting machinery
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as('aaaaaaaa-0000-0000-0000-000000000001');

select expect_denied(
  'customer cannot call grant_admin',
  'select grant_admin(''aaaaaaaa-0000-0000-0000-000000000001'', ''+919000000001'')');

select expect_denied(
  'customer cannot call revoke_admin',
  'select revoke_admin(''dddddddd-0000-0000-0000-000000000004'')');

select expect_denied(
  'customer cannot update its own role directly',
  'update users set role = ''admin'' where id = ''aaaaaaaa-0000-0000-0000-000000000001''');

-- users_self_read shows the caller their own row and nothing else, so an
-- attacker cannot enumerate who the administrators are before targeting one.
select expect_visible(
  'customer cannot list admin accounts',
  'select id from users where role = ''admin''', 0);

select expect_visible(
  'customer sees only its own user row',
  'select id from users', 1);
rollback;

-- ===========================================================================
-- VENDOR
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as('bbbbbbbb-0000-0000-0000-000000000002');

select expect_denied(
  'vendor cannot call grant_admin',
  'select grant_admin(''bbbbbbbb-0000-0000-0000-000000000002'', ''+919000000002'')');

select expect_visible(
  'vendor cannot read the audit log',
  'select id from audit_log', 0);
rollback;

-- ===========================================================================
-- ANONYMOUS
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as_anon();

select expect_denied(
  'anonymous cannot call grant_admin',
  'select grant_admin(''aaaaaaaa-0000-0000-0000-000000000001'', ''+910000000000'')');

select expect_visible(
  'anonymous sees no user rows',
  'select id from users', 0);
rollback;

-- ===========================================================================
-- ADMIN — positive control
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as('dddddddd-0000-0000-0000-000000000004');

select expect_visible('admin sees every user', 'select id from users', 3);
select expect_visible('admin can read the audit log', 'select id from audit_log', 0);
rollback;

-- == Summary =================================================================
select
  count(*) filter (where passed)     as passed,
  count(*) filter (where not passed) as failed,
  count(*)                           as total
from test_results;

select name, detail from test_results where not passed order by id;
