-- ============================================================================
-- OORUVA — migration 07 security tests
--
-- Covers what 07 introduced: the corrected identity subject, suspension as a
-- live revocation, the reward ledger balance, configurable reward rules, and
-- product visibility following its business.
--
-- Run with:  supabase/tests/run_tests.sh
-- ============================================================================

set client_min_messages to notice;
truncate test_results;

-- == Fixtures ================================================================
-- Subjects are users.id now, not a separate auth_uid: migration 07 makes the
-- JWT `sub` the OORUVA user id.
insert into users (id, phone, role, firebase_uid) values
  ('aaaaaaaa-0000-0000-0000-000000000001', '+919000000001', 'customer', 'fb-customer-alpha'),
  ('bbbbbbbb-0000-0000-0000-000000000002', '+919000000002', 'vendor',   'fb-vendor-bravo'),
  ('cccccccc-0000-0000-0000-000000000003', '+919000000003', 'vendor',   'fb-vendor-charlie'),
  ('dddddddd-0000-0000-0000-000000000004', '+919000000004', 'admin',    'fb-admin-delta'),
  ('eeeeeeee-0000-0000-0000-000000000005', '+919000000005', 'vendor',   'fb-vendor-echo')
on conflict (id) do nothing;

-- Echo is suspended, and owns a verified business. Before 07 that listing
-- stayed publicly visible, which is the bug these tests pin down.
update users set suspended = true where id = 'eeeeeeee-0000-0000-0000-000000000005';

insert into businesses (id, vendor_id, name, status, location_lat, location_lng) values
  ('11110000-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000002', 'Verified Stall', 'verified', 11.3410, 77.7172),
  ('22220000-0000-0000-0000-000000000002', 'bbbbbbbb-0000-0000-0000-000000000002', 'Draft Stall',    'draft',    11.3420, 77.7180),
  ('33330000-0000-0000-0000-000000000003', 'cccccccc-0000-0000-0000-000000000003', 'Rival Stall',    'draft',    11.3430, 77.7190),
  ('44440000-0000-0000-0000-000000000004', 'eeeeeeee-0000-0000-0000-000000000005', 'Suspended Stall','verified', 11.3440, 77.7200)
on conflict (id) do nothing;

insert into products (id, vendor_id, business_id, name, price, kind) values
  ('aa110000-0000-0000-0000-0000000000a1', 'bbbbbbbb-0000-0000-0000-000000000002',
   '11110000-0000-0000-0000-000000000001', 'Filter Coffee', 15.00, 'item'),
  ('aa220000-0000-0000-0000-0000000000a2', 'bbbbbbbb-0000-0000-0000-000000000002',
   '22220000-0000-0000-0000-000000000002', 'Secret Draft Box', 500.00, 'box')
on conflict (id) do nothing;

insert into reward_transactions (id, customer_id, direction, points, activity_type, status) values
  ('55550000-0000-0000-0000-000000000005', 'aaaaaaaa-0000-0000-0000-000000000001', 'credit', 50, 'check_in', 'credited'),
  ('55550000-0000-0000-0000-000000000006', 'aaaaaaaa-0000-0000-0000-000000000001', 'credit', 10, 'review',   'credited'),
  ('55550000-0000-0000-0000-000000000007', 'aaaaaaaa-0000-0000-0000-000000000001', 'debit',  20, 'redeem',   'credited'),
  -- A pending credit must not be spendable.
  ('55550000-0000-0000-0000-000000000008', 'aaaaaaaa-0000-0000-0000-000000000001', 'credit', 999, 'review', 'pending')
on conflict (id) do nothing;

grant usage on schema public to ooruva_client;
grant select, insert, update, delete on all tables in schema public to ooruva_client;
grant usage, select on all sequences in schema public to ooruva_client;

-- ===========================================================================
-- CUSTOMER
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as('aaaaaaaa-0000-0000-0000-000000000001');

-- Bravo's verified stall is visible; Echo's is not, because Echo is suspended.
select expect_visible(
  'suspended vendor business is hidden from customers',
  'select id from businesses where id = ''44440000-0000-0000-0000-000000000004''', 0);

select expect_visible(
  'unsuspended vendor business is still visible',
  'select id from businesses where id = ''11110000-0000-0000-0000-000000000001''', 1);

select expect_visible(
  'nearby_businesses does not leak drafts',
  'select id from nearby_businesses(11.3410, 77.7172, 10)', 1);

select expect_visible(
  'nearby_businesses does not leak a suspended listing',
  'select id from nearby_businesses(11.3410, 77.7172, 10)
    where id = ''44440000-0000-0000-0000-000000000004''', 0);

-- Products follow their business: the draft box must not be readable.
select expect_visible(
  'product on a verified business is readable',
  'select id from products where id = ''aa110000-0000-0000-0000-0000000000a1''', 1);

select expect_visible(
  'product on another actor''s draft business is hidden',
  'select id from products where id = ''aa220000-0000-0000-0000-0000000000a2''', 0);

-- Balance counts credited rows only: 50 + 10 - 20 = 40, ignoring the pending 999.
select expect_value(
  'reward balance ignores pending and honours debits',
  'select reward_balance()', '40');

select expect_value(
  'reward balance for another customer returns zero, not their total',
  'select reward_balance(''bbbbbbbb-0000-0000-0000-000000000002'')', '0');

select expect_denied(
  'customer cannot edit reward rules',
  'update reward_rules set points = 100000 where activity_type = ''review''');

select expect_visible(
  'reward rules are readable so the app can explain what earns points',
  'select activity_type from reward_rules', 5);
rollback;

-- ===========================================================================
-- SUSPENDED VENDOR — suspension revokes access, not just login
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as('eeeeeeee-0000-0000-0000-000000000005');

-- current_user_id() returns null for a suspended account, so every
-- owner-scoped policy fails closed — including on their own rows.
select expect_visible(
  'suspended vendor cannot read its own business',
  'select id from businesses where id = ''44440000-0000-0000-0000-000000000004''', 0);

select expect_denied(
  'suspended vendor cannot create a new business',
  'insert into businesses (vendor_id, name) values
     (''eeeeeeee-0000-0000-0000-000000000005'', ''Evasion Stall'')');
rollback;

-- ===========================================================================
-- VENDOR — sees own drafts, still cannot reach a rival's
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as('bbbbbbbb-0000-0000-0000-000000000002');

select expect_visible(
  'vendor sees own draft product',
  'select id from products where id = ''aa220000-0000-0000-0000-0000000000a2''', 1);

select expect_denied(
  'vendor cannot mint its own reward credit',
  'insert into reward_transactions (customer_id, direction, points, activity_type, status)
   values (''bbbbbbbb-0000-0000-0000-000000000002'', ''credit'', 5000, ''fraud'', ''credited'')');

select expect_denied(
  'vendor cannot promote a pending credit to credited',
  'update reward_transactions set status = ''credited''
    where id = ''55550000-0000-0000-0000-000000000008''');

select expect_denied(
  'vendor cannot edit reward rules',
  'update reward_rules set points = 9999 where activity_type = ''check_in''');
rollback;

-- ===========================================================================
-- ANONYMOUS
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as_anon();

select expect_visible(
  'anonymous cannot see a suspended vendor listing',
  'select id from businesses where id = ''44440000-0000-0000-0000-000000000004''', 0);

select expect_value(
  'anonymous reward balance is zero, not an error',
  'select reward_balance()', '0');
rollback;

-- ===========================================================================
-- ADMIN — positive control
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as('dddddddd-0000-0000-0000-000000000004');

select expect_visible('admin sees every business including suspended',
  'select id from businesses', 4);

select expect_visible('admin sees every product', 'select id from products', 2);
rollback;

-- == Summary =================================================================
select
  count(*) filter (where passed)     as passed,
  count(*) filter (where not passed) as failed,
  count(*)                           as total
from test_results;

select name, detail from test_results where not passed order by id;
