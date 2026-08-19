-- ============================================================================
-- OORUVA — reward ledger integrity tests
--
-- These run as the table owner, not as a client, and that is the point. Points
-- are minted by an edge function holding the service role, which bypasses RLS
-- entirely — so RLS proves nothing about this path. What protects the ledger
-- here is the index and the triggers from migration 10, and these tests
-- exercise them the same way a buggy or compromised function would.
--
-- Run with:  supabase/tests/run_tests.sh
-- ============================================================================

set client_min_messages to notice;
truncate test_results;

insert into users (id, phone, role, firebase_uid) values
  ('aaaaaaaa-0000-0000-0000-000000000001', '+919000000001', 'customer', 'fb-alpha'),
  ('bbbbbbbb-0000-0000-0000-000000000002', '+919000000002', 'vendor',   'fb-bravo')
on conflict (id) do nothing;

insert into businesses (id, vendor_id, name, status) values
  ('11110000-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000002',
   'Bravo Chai Kadai', 'verified')
on conflict (id) do nothing;

insert into reviews (id, vendor_id, customer_id, rating, text) values
  ('99990000-0000-0000-0000-000000000009', 'bbbbbbbb-0000-0000-0000-000000000002',
   'aaaaaaaa-0000-0000-0000-000000000001', 5, 'Excellent filter coffee')
on conflict (id) do nothing;

insert into offers (id, vendor_id, title, points_required, usage_limit) values
  ('77770000-0000-0000-0000-000000000007', 'bbbbbbbb-0000-0000-0000-000000000002',
   'Free vadai', 30, null),
  ('88880000-0000-0000-0000-000000000008', 'bbbbbbbb-0000-0000-0000-000000000002',
   'Limited: free tea', 10, 1)
on conflict (id) do nothing;

-- ===========================================================================
-- IDEMPOTENCY — one action is paid for once
-- ===========================================================================
begin;

insert into reward_transactions (customer_id, direction, points, activity_type, reference_id, status)
values ('aaaaaaaa-0000-0000-0000-000000000001', 'credit', 10, 'review',
        '99990000-0000-0000-0000-000000000009', 'credited');

select expect_denied(
  'the same review cannot be awarded twice',
  'insert into reward_transactions (customer_id, direction, points, activity_type, reference_id, status)
   values (''aaaaaaaa-0000-0000-0000-000000000001'', ''credit'', 10, ''review'',
           ''99990000-0000-0000-0000-000000000009'', ''credited'')');

-- Manual adjustments carry no reference_id and must stay repeatable, or an
-- admin could only ever make one correction per customer.
insert into reward_transactions (customer_id, direction, points, activity_type, status, note)
values ('aaaaaaaa-0000-0000-0000-000000000001', 'credit', 5, 'manual_adjustment', 'credited', 'goodwill');

insert into reward_transactions (customer_id, direction, points, activity_type, status, note)
values ('aaaaaaaa-0000-0000-0000-000000000001', 'credit', 5, 'manual_adjustment', 'credited', 'goodwill again');

select expect_visible(
  'manual adjustments without a reference are repeatable',
  'select id from reward_transactions where activity_type = ''manual_adjustment''', 2);
rollback;

-- ===========================================================================
-- OVERDRAFT — a debit cannot exceed the credited balance
-- ===========================================================================
begin;

insert into reward_transactions (customer_id, direction, points, activity_type, status)
values ('aaaaaaaa-0000-0000-0000-000000000001', 'credit', 50, 'check_in', 'credited');

select expect_denied(
  'a debit larger than the balance is refused',
  'insert into reward_transactions (customer_id, direction, points, activity_type, status)
   values (''aaaaaaaa-0000-0000-0000-000000000001'', ''debit'', 51, ''redeem'', ''credited'')');

insert into reward_transactions (customer_id, direction, points, activity_type, status)
values ('aaaaaaaa-0000-0000-0000-000000000001', 'debit', 50, 'redeem', 'credited');

select expect_value(
  'spending the exact balance is allowed and leaves zero',
  'select reward_balance(''aaaaaaaa-0000-0000-0000-000000000001'')', '0');

select expect_denied(
  'a second debit against an empty balance is refused',
  'insert into reward_transactions (customer_id, direction, points, activity_type, status)
   values (''aaaaaaaa-0000-0000-0000-000000000001'', ''debit'', 1, ''redeem'', ''credited'')');
rollback;

-- A pending credit is not money. If the guard counted it, a customer could
-- spend points that moderation was still deciding about.
begin;

insert into reward_transactions (customer_id, direction, points, activity_type, status)
values ('aaaaaaaa-0000-0000-0000-000000000001', 'credit', 500, 'post', 'pending');

select expect_denied(
  'a pending credit cannot be spent',
  'insert into reward_transactions (customer_id, direction, points, activity_type, status)
   values (''aaaaaaaa-0000-0000-0000-000000000001'', ''debit'', 100, ''redeem'', ''credited'')');

select expect_value(
  'a pending credit does not appear in the balance',
  'select reward_balance(''aaaaaaaa-0000-0000-0000-000000000001'')', '0');
rollback;

-- A reversed transaction must drop straight back out of the balance, because
-- that is how the rewards function undoes a redemption it could not record.
begin;

insert into reward_transactions (id, customer_id, direction, points, activity_type, status)
values ('66660000-0000-0000-0000-000000000006', 'aaaaaaaa-0000-0000-0000-000000000001',
        'credit', 40, 'check_in', 'credited');

insert into reward_transactions (id, customer_id, direction, points, activity_type, status)
values ('66660000-0000-0000-0000-000000000016', 'aaaaaaaa-0000-0000-0000-000000000001',
        'debit', 30, 'redeem', 'credited');

select expect_value(
  'balance reflects a debit',
  'select reward_balance(''aaaaaaaa-0000-0000-0000-000000000001'')', '10');

update reward_transactions set status = 'reversed'
 where id = '66660000-0000-0000-0000-000000000016';

select expect_value(
  'reversing a debit returns the points',
  'select reward_balance(''aaaaaaaa-0000-0000-0000-000000000001'')', '40');
rollback;

-- ===========================================================================
-- DAILY CAP
-- ===========================================================================
begin;

insert into reward_transactions (customer_id, direction, points, activity_type, reference_id, status)
values
  ('aaaaaaaa-0000-0000-0000-000000000001', 'credit', 10, 'review',
   '99990000-0000-0000-0000-000000000001', 'credited'),
  ('aaaaaaaa-0000-0000-0000-000000000001', 'credit', 10, 'review',
   '99990000-0000-0000-0000-000000000002', 'credited');

select expect_value(
  'rewards_earned_today counts only the named activity',
  'select rewards_earned_today(''aaaaaaaa-0000-0000-0000-000000000001'', ''review'')::text', '2');

select expect_value(
  'an activity with no awards today counts zero',
  'select rewards_earned_today(''aaaaaaaa-0000-0000-0000-000000000001'', ''check_in'')::text', '0');

-- A debit is not an earning and must not eat into the cap.
insert into reward_transactions (customer_id, direction, points, activity_type, status)
values ('aaaaaaaa-0000-0000-0000-000000000001', 'debit', 5, 'review', 'credited');

select expect_value(
  'a debit does not count towards the daily earning cap',
  'select rewards_earned_today(''aaaaaaaa-0000-0000-0000-000000000001'', ''review'')::text', '2');
rollback;

-- ===========================================================================
-- OFFER USAGE LIMIT
-- ===========================================================================
begin;

insert into reward_transactions (customer_id, direction, points, activity_type, status)
values ('aaaaaaaa-0000-0000-0000-000000000001', 'credit', 100, 'check_in', 'credited');

insert into offer_redemptions (offer_id, customer_id, points_spent)
values ('88880000-0000-0000-0000-000000000008', 'aaaaaaaa-0000-0000-0000-000000000001', 10);

select expect_denied(
  'an offer cannot be redeemed beyond its usage limit',
  'insert into offer_redemptions (offer_id, customer_id, points_spent)
   values (''88880000-0000-0000-0000-000000000008'',
           ''aaaaaaaa-0000-0000-0000-000000000001'', 10)');

-- An unlimited offer keeps working.
insert into offer_redemptions (offer_id, customer_id, points_spent)
values ('77770000-0000-0000-0000-000000000007', 'aaaaaaaa-0000-0000-0000-000000000001', 30);

insert into offer_redemptions (offer_id, customer_id, points_spent)
values ('77770000-0000-0000-0000-000000000007', 'aaaaaaaa-0000-0000-0000-000000000001', 30);

select expect_visible(
  'an offer with no usage limit can be redeemed repeatedly',
  'select id from offer_redemptions
    where offer_id = ''77770000-0000-0000-0000-000000000007''', 2);
rollback;

-- == Summary =================================================================
select
  count(*) filter (where passed)     as passed,
  count(*) filter (where not passed) as failed,
  count(*)                           as total
from test_results;

select name, detail from test_results where not passed order by id;
