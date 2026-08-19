-- ============================================================================
-- OORUVA — role security tests (Phase 4)
--
-- These are the negative cases. A feature is not complete because the happy
-- path works; it is complete when the wrong actor is refused.
--
-- Run with:  supabase/tests/run_tests.sh
-- ============================================================================

set client_min_messages to notice;
truncate test_results;

-- ── Fixtures ───────────────────────────────────────────────────────────────
-- Distinct auth_uids so each actor can be impersonated independently.
insert into users (id, phone, role, auth_uid) values
  ('aaaaaaaa-0000-0000-0000-000000000001', '+919000000001', 'customer', 'aaaaaaaa-0000-0000-0000-00000000a111'),
  ('bbbbbbbb-0000-0000-0000-000000000002', '+919000000002', 'vendor',   'bbbbbbbb-0000-0000-0000-00000000b111'),
  ('cccccccc-0000-0000-0000-000000000003', '+919000000003', 'vendor',   'cccccccc-0000-0000-0000-00000000c222'),
  ('dddddddd-0000-0000-0000-000000000004', '+919000000004', 'admin',    'dddddddd-0000-0000-0000-00000000d111')
on conflict (id) do nothing;

insert into businesses (id, vendor_id, name, status) values
  ('11110000-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000002', 'Verified Stall', 'verified'),
  ('22220000-0000-0000-0000-000000000002', 'bbbbbbbb-0000-0000-0000-000000000002', 'Draft Stall',    'draft'),
  ('33330000-0000-0000-0000-000000000003', 'cccccccc-0000-0000-0000-000000000003', 'Rival Stall',    'draft')
on conflict (id) do nothing;

insert into business_documents (id, business_id, document_type, document_number, storage_path) values
  ('44440000-0000-0000-0000-000000000004', '22220000-0000-0000-0000-000000000002',
   'fssai', 'TEST-0000-0000', 'documents/vendor-b/fssai.pdf')
on conflict (id) do nothing;

insert into reward_transactions (id, customer_id, direction, points, activity_type, status) values
  ('55550000-0000-0000-0000-000000000005', 'aaaaaaaa-0000-0000-0000-000000000001',
   'credit', 50, 'check_in', 'credited')
on conflict (id) do nothing;

grant usage on schema public to ooruva_client;
grant select, insert, update, delete on all tables in schema public to ooruva_client;
grant usage, select on all sequences in schema public to ooruva_client;

-- ═══════════════════════════════════════════════════════════════════════════
-- CUSTOMER
-- ═══════════════════════════════════════════════════════════════════════════
begin;
set local role ooruva_client;
select test_as('aaaaaaaa-0000-0000-0000-000000000001');

select expect_visible(
  'customer sees only verified businesses',
  'select id from businesses', 1);

select expect_visible(
  'customer cannot see a vendor draft',
  'select id from businesses where status = ''draft''', 0);

select expect_visible(
  'customer cannot read vendor documents',
  'select id from business_documents', 0);

select expect_denied(
  'customer cannot create a business for a vendor',
  'insert into businesses (vendor_id, name) values
     (''bbbbbbbb-0000-0000-0000-000000000002'', ''Hijacked'')');

select expect_denied(
  'customer cannot mint reward points',
  'insert into reward_transactions (customer_id, direction, points, activity_type, status)
   values (''aaaaaaaa-0000-0000-0000-000000000001'', ''credit'', 100000, ''fraud'', ''credited'')');

select expect_denied(
  'customer cannot record a verification outcome',
  'insert into verification_records (business_id, method, outcome)
   values (''11110000-0000-0000-0000-000000000001'', ''manual_review'', ''passed'')');

select expect_denied(
  'customer cannot edit the category taxonomy',
  'insert into business_categories (slug, name) values (''fake'', ''Fake'')');

select expect_visible(
  'customer reads own reward ledger only',
  'select id from reward_transactions', 1);
rollback;

-- ═══════════════════════════════════════════════════════════════════════════
-- VENDOR
-- ═══════════════════════════════════════════════════════════════════════════
begin;
set local role ooruva_client;
select test_as('bbbbbbbb-0000-0000-0000-000000000002');

select expect_visible(
  'vendor sees own draft plus verified businesses',
  'select id from businesses', 2);

select expect_visible(
  'vendor cannot see a rival draft',
  'select id from businesses where vendor_id = ''cccccccc-0000-0000-0000-000000000003''', 0);

select expect_visible(
  'vendor reads own documents',
  'select id from business_documents', 1);

select expect_denied(
  'vendor cannot mark itself verified',
  'insert into verification_records (business_id, method, outcome)
   values (''22220000-0000-0000-0000-000000000002'', ''manual_review'', ''passed'')');

select expect_denied(
  'vendor cannot approve its own business row',
  'update businesses set status = ''verified'' where id = ''33330000-0000-0000-0000-000000000003''');

select expect_visible(
  'vendor cannot read customer reward ledger',
  'select id from reward_transactions', 0);

select expect_denied(
  'vendor cannot escalate itself to admin',
  'update users set role = ''admin'' where id = ''bbbbbbbb-0000-0000-0000-000000000002''');
rollback;

-- ═══════════════════════════════════════════════════════════════════════════
-- RIVAL VENDOR — the case a single-vendor test suite always misses
-- ═══════════════════════════════════════════════════════════════════════════
begin;
set local role ooruva_client;
select test_as('cccccccc-0000-0000-0000-000000000003');

select expect_visible(
  'rival vendor cannot read another vendor documents',
  'select id from business_documents', 0);

select expect_denied(
  'rival vendor cannot edit another vendor business',
  'update businesses set name = ''Stolen'' where id = ''22220000-0000-0000-0000-000000000002''');

select expect_denied(
  'rival vendor cannot delete another vendor business',
  'delete from businesses where id = ''22220000-0000-0000-0000-000000000002''');
rollback;

-- ═══════════════════════════════════════════════════════════════════════════
-- ANONYMOUS
-- ═══════════════════════════════════════════════════════════════════════════
begin;
set local role ooruva_client;
select test_as_anon();

select expect_visible(
  'anonymous sees only verified businesses',
  'select id from businesses', 1);

select expect_visible(
  'anonymous cannot read documents',
  'select id from business_documents', 0);

select expect_visible(
  'anonymous cannot read any reward ledger',
  'select id from reward_transactions', 0);
rollback;

-- ═══════════════════════════════════════════════════════════════════════════
-- ROLE ESCALATION — the trigger from migration 04
-- ═══════════════════════════════════════════════════════════════════════════
begin;
set local role ooruva_client;
select test_as('aaaaaaaa-0000-0000-0000-000000000001');

select expect_denied(
  'client cannot self-insert an admin user',
  'insert into users (phone, role, auth_uid)
   values (''+919999999999'', ''admin'', ''eeeeeeee-0000-0000-0000-00000000e111'')');

select expect_denied(
  'client cannot insert an unknown role',
  'insert into users (phone, role, auth_uid)
   values (''+919999999998'', ''superuser'', ''eeeeeeee-0000-0000-0000-00000000e222'')');
rollback;

-- ═══════════════════════════════════════════════════════════════════════════
-- ADMIN — the positive control. If this fails, the suite proves nothing.
-- ═══════════════════════════════════════════════════════════════════════════
begin;
set local role ooruva_client;
select test_as('dddddddd-0000-0000-0000-000000000004');

select expect_visible('admin sees every business', 'select id from businesses', 3);
select expect_visible('admin sees every document', 'select id from business_documents', 1);
rollback;

-- ── Summary ────────────────────────────────────────────────────────────────
select
  count(*) filter (where passed)     as passed,
  count(*) filter (where not passed) as failed,
  count(*)                           as total
from test_results;

select name, detail from test_results where not passed order by id;
