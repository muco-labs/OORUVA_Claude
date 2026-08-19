-- ============================================================================
-- OORUVA — storage scoping and search tests
--
-- Migration 09 narrowed the storage policies from "any authenticated user may
-- write anywhere" to "only into a business you own". These are the tests that
-- say so, plus coverage of the rewritten nearby search and the new full-text
-- search — both of which had to keep failing closed on drafts.
--
-- Run with:  supabase/tests/run_tests.sh
-- ============================================================================

set client_min_messages to notice;
truncate test_results;

-- == Fixtures ================================================================
insert into users (id, phone, role, firebase_uid) values
  ('aaaaaaaa-0000-0000-0000-000000000001', '+919000000001', 'customer', 'fb-customer-alpha'),
  ('bbbbbbbb-0000-0000-0000-000000000002', '+919000000002', 'vendor',   'fb-vendor-bravo'),
  ('cccccccc-0000-0000-0000-000000000003', '+919000000003', 'vendor',   'fb-vendor-charlie'),
  ('dddddddd-0000-0000-0000-000000000004', '+919000000004', 'admin',    null)
on conflict (id) do nothing;

-- Bravo owns a verified tea stall and a draft. Charlie owns a rival.
-- Coordinates are real Erode-district positions so the distances mean something.
insert into businesses (id, vendor_id, name, description, district, status,
                        location_lat, location_lng) values
  ('11110000-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000002',
   'Bravo Chai Kadai', 'Filter coffee and vadai since 1998', 'Erode', 'verified', 11.3410, 77.7172),
  ('22220000-0000-0000-0000-000000000002', 'bbbbbbbb-0000-0000-0000-000000000002',
   'Bravo Secret Bakery', 'Unlaunched bakery concept', 'Erode', 'draft', 11.3415, 77.7175),
  ('33330000-0000-0000-0000-000000000003', 'cccccccc-0000-0000-0000-000000000003',
   'Charlie Mobile Repair', 'Screen replacement', 'Erode', 'verified', 11.3480, 77.7250),
  -- Far away: Chennai, ~380 km. Must fall outside a 25 km search.
  ('44440000-0000-0000-0000-000000000004', 'cccccccc-0000-0000-0000-000000000003',
   'Charlie Chennai Branch', 'Second location', 'Chennai', 'verified', 13.0827, 80.2707)
on conflict (id) do nothing;

grant usage on schema public to ooruva_client;
-- Without USAGE on the storage schema every storage statement fails with
-- "permission denied for schema storage", which expect_denied would happily
-- score as a pass. The negative tests below have to be refused by the policy,
-- not by a missing grant.
grant usage on schema storage to ooruva_client;
grant select, insert, update, delete on all tables in schema public to ooruva_client;
grant select, insert, update, delete on storage.objects to ooruva_client;
grant usage, select on all sequences in schema public to ooruva_client;

-- ===========================================================================
-- STORAGE — a vendor may only write into their own business's folder
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as('bbbbbbbb-0000-0000-0000-000000000002');

select expect_denied(
  'vendor cannot upload a document into a rival business folder',
  'insert into storage.objects (bucket_id, name, owner)
   values (''documents'', ''33330000-0000-0000-0000-000000000003/fssai.pdf'',
           ''bbbbbbbb-0000-0000-0000-000000000002'')');

select expect_denied(
  'vendor cannot upload a photo into a rival business folder',
  'insert into storage.objects (bucket_id, name, owner)
   values (''photos'', ''33330000-0000-0000-0000-000000000003/shopfront.jpg'',
           ''bbbbbbbb-0000-0000-0000-000000000002'')');

-- A path with no business id used to be accepted by the old
-- "any authenticated user" policy. storage_business_id returns null for it and
-- owns_business(null) is false, so it now fails closed.
select expect_denied(
  'a document path with no business id is refused',
  'insert into storage.objects (bucket_id, name, owner)
   values (''documents'', ''loose-file.pdf'', ''bbbbbbbb-0000-0000-0000-000000000002'')');

select expect_denied(
  'a document path with a malformed business id is refused, not errored',
  'insert into storage.objects (bucket_id, name, owner)
   values (''documents'', ''not-a-uuid/fssai.pdf'', ''bbbbbbbb-0000-0000-0000-000000000002'')');
rollback;

-- The positive control. Without this the tests above would pass on a policy
-- that simply denies everything.
begin;
set local role ooruva_client;
select test_as('bbbbbbbb-0000-0000-0000-000000000002');

insert into storage.objects (bucket_id, name, owner)
values ('documents', '11110000-0000-0000-0000-000000000001/fssai.pdf',
        'bbbbbbbb-0000-0000-0000-000000000002');

select expect_visible(
  'vendor can upload into a business it owns',
  'select id from storage.objects
    where name = ''11110000-0000-0000-0000-000000000001/fssai.pdf''', 1);

-- Uploading to a draft they own must work too: documents are submitted during
-- onboarding, before anything is verified.
insert into storage.objects (bucket_id, name, owner)
values ('documents', '22220000-0000-0000-0000-000000000002/udyam.pdf',
        'bbbbbbbb-0000-0000-0000-000000000002');

select expect_visible(
  'vendor can upload against its own draft business',
  'select id from storage.objects
    where name = ''22220000-0000-0000-0000-000000000002/udyam.pdf''', 1);
rollback;

-- ===========================================================================
-- STORAGE — documents stay private between vendors
-- ===========================================================================
begin;
set local role ooruva_client;

-- Seeded as the owner so there is something to try to read.
select test_as('bbbbbbbb-0000-0000-0000-000000000002');
insert into storage.objects (bucket_id, name, owner)
values ('documents', '11110000-0000-0000-0000-000000000001/fssai.pdf',
        'bbbbbbbb-0000-0000-0000-000000000002');

select test_as('cccccccc-0000-0000-0000-000000000003');
select expect_visible(
  'rival vendor cannot read another vendor document',
  'select id from storage.objects where bucket_id = ''documents''', 0);

select test_as('aaaaaaaa-0000-0000-0000-000000000001');
select expect_visible(
  'customer cannot read any document',
  'select id from storage.objects where bucket_id = ''documents''', 0);

select expect_denied(
  'customer cannot delete a vendor document',
  'delete from storage.objects
    where name = ''11110000-0000-0000-0000-000000000001/fssai.pdf''');

select test_as('dddddddd-0000-0000-0000-000000000004');
select expect_visible(
  'admin can read documents for review',
  'select id from storage.objects where bucket_id = ''documents''', 1);
rollback;

-- ===========================================================================
-- NEARBY SEARCH — the bounding box must not change what is visible
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as('aaaaaaaa-0000-0000-0000-000000000001');

-- Two verified businesses within 25 km of Erode; the Chennai one is 380 km out
-- and Bravo's draft is invisible to a customer.
select expect_visible(
  'nearby returns verified businesses in range only',
  'select id from nearby_businesses(11.3410, 77.7172, 25)', 2);

select expect_visible(
  'nearby excludes a business outside the radius',
  'select id from nearby_businesses(11.3410, 77.7172, 25)
    where id = ''44440000-0000-0000-0000-000000000004''', 0);

select expect_visible(
  'the far business appears once the radius covers it',
  'select id from nearby_businesses(11.3410, 77.7172, 500)
    where id = ''44440000-0000-0000-0000-000000000004''', 1);

select expect_visible(
  'nearby still hides a draft the customer does not own',
  'select id from nearby_businesses(11.3410, 77.7172, 25)
    where id = ''22220000-0000-0000-0000-000000000002''', 0);

-- Distance has to be right, not merely ordered. Bravo's stall is at the search
-- origin, so it should be essentially zero.
select expect_value(
  'distance to a business at the origin is zero',
  'select round(distance_km::numeric, 1)::text
     from nearby_businesses(11.3410, 77.7172, 25)
    where id = ''11110000-0000-0000-0000-000000000001''', '0.0');
rollback;

-- The owner sees their own draft in nearby results; nobody else does.
begin;
set local role ooruva_client;
select test_as('bbbbbbbb-0000-0000-0000-000000000002');

select expect_visible(
  'vendor sees own draft in nearby results',
  'select id from nearby_businesses(11.3410, 77.7172, 25)
    where id = ''22220000-0000-0000-0000-000000000002''', 1);
rollback;

-- ===========================================================================
-- FULL-TEXT SEARCH
-- ===========================================================================
begin;
set local role ooruva_client;
select test_as('aaaaaaaa-0000-0000-0000-000000000001');

select expect_visible(
  'search finds a business by name',
  'select id from search_businesses(''chai'')', 1);

select expect_visible(
  'search matches a prefix as the person is still typing',
  'select id from search_businesses(''mobi'')', 1);

select expect_visible(
  'search matches words in the description',
  'select id from search_businesses(''vadai'')', 1);

select expect_visible(
  'search is case insensitive',
  'select id from search_businesses(''CHAI'')', 1);

-- The important one: a draft must not be discoverable by guessing its name.
select expect_visible(
  'search cannot surface another actor''s draft',
  'select id from search_businesses(''secret bakery'')', 0);

select expect_visible(
  'a term matching nothing returns nothing rather than everything',
  'select id from search_businesses(''zzzznotathing'')', 0);

-- Punctuation is stripped before the tsquery is built; if it were not, this
-- would raise a syntax error instead of returning rows.
select expect_visible(
  'punctuation in the search term does not break the query',
  'select id from search_businesses(''chai & !! kadai'')', 1);

select expect_visible(
  'an empty search term returns nothing rather than erroring',
  'select id from search_businesses(''   '')', 0);
rollback;

begin;
set local role ooruva_client;
select test_as('bbbbbbbb-0000-0000-0000-000000000002');

select expect_visible(
  'a vendor can find their own draft by name',
  'select id from search_businesses(''secret bakery'')', 1);
rollback;

-- == Summary =================================================================
select
  count(*) filter (where passed)     as passed,
  count(*) filter (where not passed) as failed,
  count(*)                           as total
from test_results;

select name, detail from test_results where not passed order by id;
