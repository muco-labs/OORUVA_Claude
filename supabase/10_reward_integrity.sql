-- ============================================================================
-- OORUVA - migration 10
-- Reward ledger integrity.
--
-- Run after 09_storage_and_search.sql. Idempotent.
--
-- WHY THIS EXISTS
-- ---------------
-- RLS already stops a client writing the ledger; points are minted by an edge
-- function holding the service role, which bypasses RLS entirely. That means
-- every guard the function relies on has to exist in the database as well,
-- because a bug in the function has no policy standing behind it.
--
-- Two things are enforced here rather than in TypeScript:
--   1. The same action cannot be paid for twice, even if the request is
--      retried, replayed, or arrives twice from a flaky connection.
--   2. A debit can never take a balance negative.
-- ============================================================================

-- == 1. One award per action =================================================
-- A partial unique index rather than a constraint: reference_id is null for
-- adjustments an admin makes by hand, and those must stay repeatable.
create unique index if not exists idx_reward_once_per_reference
  on reward_transactions (activity_type, reference_id)
  where reference_id is not null and direction = 'credit';

comment on index idx_reward_once_per_reference is
  'Idempotency. A retried award for the same review or check-in fails on this index instead of paying twice.';

-- == 2. A balance cannot go negative =========================================
-- Redemption reads the balance, then writes a debit. Between those two steps a
-- second concurrent redemption can read the same balance, and both succeed --
-- the classic double-spend. The check runs inside the insert, so the second
-- transaction sees the first one's row and fails.
create or replace function guard_reward_debit() returns trigger as $fn$
declare
  available integer;
begin
  if new.direction <> 'debit' or new.status <> 'credited' then
    return new;
  end if;

  select coalesce(sum(
           case when direction = 'credit' then points else -points end
         ), 0)
    into available
    from reward_transactions
   where customer_id = new.customer_id
     and status = 'credited'
     and id <> new.id;

  if available < new.points then
    raise exception 'Insufficient points: balance %, tried to spend %', available, new.points
      using errcode = 'check_violation';
  end if;

  return new;
end;
$fn$ language plpgsql;

drop trigger if exists trg_guard_reward_debit on reward_transactions;
create trigger trg_guard_reward_debit
  before insert on reward_transactions
  for each row execute function guard_reward_debit();

comment on function guard_reward_debit is
  'Refuses a debit larger than the credited balance. Enforced in the database because the service role bypasses RLS, so the edge function is not a place a spending limit can safely live alone.';

-- == 3. How much has this customer earned today ==============================
-- Used by the award function to apply reward_rules.daily_cap. Counting here
-- rather than in TypeScript keeps the definition of "today" in one place.
create or replace function rewards_earned_today(
  target uuid,
  activity varchar
) returns integer as $fn$
  select coalesce(count(*), 0)::integer
    from reward_transactions
   where customer_id = target
     and activity_type = activity
     and direction = 'credit'
     and created_at >= date_trunc('day', now());
$fn$ language sql stable security definer;

comment on function rewards_earned_today is
  'Security definer so the award function can check a cap without the caller needing to read the whole ledger.';

revoke all on function rewards_earned_today(uuid, varchar) from public;
revoke all on function rewards_earned_today(uuid, varchar) from anon;

-- == 4. Redemption records what was actually spent ===========================
alter table offer_redemptions
  add column if not exists reward_transaction_id uuid references reward_transactions(id);

comment on column offer_redemptions.reward_transaction_id is
  'The debit that paid for this redemption. Without it a redemption and its ledger entry can drift apart and neither is authoritative.';

create index if not exists idx_redemptions_customer
  on offer_redemptions (customer_id, created_at desc);

-- An offer with a usage limit must stop at that limit.
create or replace function guard_offer_usage() returns trigger as $fn$
declare
  cap integer;
  used integer;
begin
  select usage_limit into cap from offers where id = new.offer_id;
  if cap is null then
    return new;
  end if;

  select count(*) into used from offer_redemptions where offer_id = new.offer_id;
  if used >= cap then
    raise exception 'This offer has reached its usage limit'
      using errcode = 'check_violation';
  end if;

  return new;
end;
$fn$ language plpgsql;

drop trigger if exists trg_guard_offer_usage on offer_redemptions;
create trigger trg_guard_offer_usage
  before insert on offer_redemptions
  for each row execute function guard_offer_usage();
