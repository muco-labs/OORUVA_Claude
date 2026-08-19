-- ============================================================================
-- OORUVA - migration 07
-- Identity correction, business-model alignment, and the configurable pieces
-- the vendor and reward flows need.
--
-- Run after 06_rls_foundation.sql. Idempotent.
--
-- WHY THIS EXISTS
-- ---------------
-- 1. users.auth_uid was `uuid`. A Firebase UID is a 28-character alphanumeric
--    string, not a UUID, so a real phone sign-in could never have been stored.
--    The test suite missed it because its fixtures used synthetic UUIDs.
--    Supabase's own auth.uid() also casts the JWT `sub` to uuid, so `sub` has
--    to be a UUID regardless. The fix is therefore not "widen the column" but
--    "use the right subject": the JWT carries users.id, and the Firebase UID
--    lives in its own text column used only to look the account up at sign-in.
--
-- 2. Migration 04 introduced `businesses` as the real listing model, but the
--    nearby-search RPC, the app repository and the admin console were all still
--    reading `vendor_profiles`. This adds the equivalent surface on the correct
--    model. `vendor_profiles` is left in place and untouched so nothing breaks
--    mid-migration; it is retired once no caller remains.
-- ============================================================================

-- == 1. Identity =============================================================

alter table users add column if not exists firebase_uid text;

-- Carry over anything already stored, so a re-run on a populated database keeps
-- existing accounts reachable.
update users
   set firebase_uid = auth_uid::text
 where firebase_uid is null
   and auth_uid is not null;

create unique index if not exists idx_users_firebase_uid
  on users (firebase_uid) where firebase_uid is not null;

comment on column users.firebase_uid is
  'Firebase phone-auth subject. Text, because a Firebase UID is not a UUID. Used only by auth-bootstrap to find the account; never appears in a JWT.';

comment on column users.auth_uid is
  'Deprecated by migration 07. The session subject is now users.id. Retained so existing rows keep resolving during the transition.';

-- The signed-in subject is now users.id itself: auth-bootstrap mints a Supabase
-- JWT whose `sub` is the OORUVA user id. That removes a lookup from every
-- policy evaluation and, more importantly, makes the subject a real UUID.
--
-- `not suspended` is deliberate: suspending an account now revokes every
-- owner-scoped grant on the next request, rather than only blocking new logins.
create or replace function current_user_id() returns uuid as $fn$
  select id from users where id = auth.uid() and not suspended;
$fn$ language sql stable security definer set search_path = public, pg_temp;

create or replace function is_admin() returns boolean as $fn$
  select exists (
    select 1 from users
     where id = auth.uid() and role = 'admin' and not suspended
  );
$fn$ language sql stable security definer set search_path = public, pg_temp;

-- users: read yourself, and nothing else. Creation belongs to auth-bootstrap
-- alone (service role, which bypasses RLS), so the self-insert policy that
-- shipped in 02 is withdrawn rather than narrowed.
drop policy if exists users_self_read on users;
create policy users_self_read on users
  for select using (id = auth.uid() or is_admin());

drop policy if exists users_self_insert on users;

drop policy if exists users_admin_write on users;
create policy users_admin_write on users
  for update using (is_admin()) with check (is_admin());

-- == 2. A suspended vendor disappears from discovery =========================
-- Previously `status = 'verified'` alone made a listing public, so suspending
-- the owner left their business on the map.
--
-- The check has to be security definer. Written as a plain subquery over
-- `users` it is itself evaluated under RLS, and users_self_read shows a
-- customer only their own row -- so the subquery finds nothing, concludes
-- "not suspended", and the listing stays visible. That is the failure mode
-- this function exists to close, not a stylistic preference.
create or replace function vendor_suspended(v uuid) returns boolean as $fn$
  select coalesce((select suspended from users where id = v), false);
$fn$ language sql stable security definer set search_path = public, pg_temp;

comment on function vendor_suspended is
  'Security definer so the suspension flag is readable while evaluating another table''s policy. Returns only a boolean, so it leaks nothing beyond the fact being asked about.';

drop policy if exists biz_read on businesses;
create policy biz_read on businesses
  for select using (
    (status = 'verified' and not vendor_suspended(businesses.vendor_id))
    or vendor_id = current_user_id()
    or is_admin()
  );

-- == 3. Nearby search on the correct model ===================================
-- Same haversine as the 01 version, but over `businesses`, and it respects
-- suspension. Security invoker so RLS still applies to the caller: this
-- function widens no visibility.
create or replace function nearby_businesses(
  origin_lat double precision,
  origin_lng double precision,
  radius_km  double precision default 5,
  type_filter uuid default null,
  max_results integer default 100
)
returns table (
  id uuid,
  vendor_id uuid,
  name varchar,
  business_type_id uuid,
  type_name varchar,
  category_slug varchar,
  address text,
  district varchar,
  location_lat double precision,
  location_lng double precision,
  opening_hours varchar,
  main_photo_url varchar,
  status varchar,
  distance_km double precision
) as $fn$
  select b.id, b.vendor_id, b.name, b.business_type_id,
         bt.name as type_name, bc.slug as category_slug,
         b.address, b.district, b.location_lat, b.location_lng,
         b.opening_hours, b.main_photo_url, b.status,
         6371 * acos(least(1.0,
           cos(radians(origin_lat)) * cos(radians(b.location_lat)) *
           cos(radians(b.location_lng) - radians(origin_lng)) +
           sin(radians(origin_lat)) * sin(radians(b.location_lat))
         )) as distance_km
    from businesses b
    left join business_types bt      on bt.id = b.business_type_id
    left join business_categories bc on bc.id = bt.category_id
   where b.location_lat is not null
     and b.location_lng is not null
     and (type_filter is null or b.business_type_id = type_filter)
     and 6371 * acos(least(1.0,
           cos(radians(origin_lat)) * cos(radians(b.location_lat)) *
           cos(radians(b.location_lng) - radians(origin_lng)) +
           sin(radians(origin_lat)) * sin(radians(b.location_lat))
         )) <= radius_km
   order by distance_km
   limit greatest(1, least(max_results, 200));
$fn$ language sql stable security invoker set search_path = public, pg_temp;

comment on function nearby_businesses is
  'Security invoker on purpose: the biz_read policy still decides what the caller may see, so this cannot be used to enumerate drafts.';

-- == 4. Reward rules as configuration ========================================
-- The points attached to an action were previously a number typed into the
-- admin demo data. They belong in a table an admin can edit without a release.
create table if not exists reward_rules (
  activity_type varchar(80) primary key,
  label         varchar(150) not null,
  points        integer not null check (points >= 0),
  active        boolean not null default true,
  daily_cap     integer,
  description   text,
  updated_by    uuid references users(id),
  updated_at    timestamptz not null default now()
);

insert into reward_rules (activity_type, label, points, daily_cap, description) values
  ('review',   'Wrote a review',       10, 5,    'One verified review of a business'),
  ('photo',    'Added a photo',         5, 10,   'A photo accepted onto a business listing'),
  ('post',     'Community post',        2, 10,   'A post in a district community'),
  ('check_in', 'Checked in',           50, 3,    'A confirmed visit to a verified business'),
  ('referral', 'Referred a customer',  100, null, 'A referred customer completed sign-up')
on conflict (activity_type) do nothing;

alter table reward_rules enable row level security;

drop policy if exists reward_rules_read on reward_rules;
create policy reward_rules_read on reward_rules for select using (true);

drop policy if exists reward_rules_admin on reward_rules;
create policy reward_rules_admin on reward_rules
  for all using (is_admin()) with check (is_admin());

-- Balance is derived from the ledger, never stored. Only credited rows count,
-- so a pending or reversed entry can never be spent.
create or replace function reward_balance(target uuid default null)
returns integer as $fn$
  select coalesce(sum(
           case when direction = 'credit' then points else -points end
         ), 0)::integer
    from reward_transactions
   where customer_id = coalesce(target, current_user_id())
     and status = 'credited';
$fn$ language sql stable security invoker set search_path = public, pg_temp;

comment on function reward_balance is
  'Security invoker: the ledger RLS policy means a caller can only ever total their own rows. Passing another customer id returns 0 rather than leaking.';

-- == 5. Products that are not only restaurant dishes =========================
-- A gift shop sells boxes, an electrician sells a callout, a caterer sells a
-- package. One flat "name + price" row cannot express that.
alter table products add column if not exists business_id uuid references businesses(id) on delete cascade;
alter table products add column if not exists kind varchar(30) not null default 'item';
alter table products add column if not exists unit varchar(40);
alter table products add column if not exists available boolean not null default true;
alter table products add column if not exists sort_order integer not null default 100;

do $guard$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'products_kind_check'
  ) then
    alter table products add constraint products_kind_check
      check (kind in ('item','box','bundle','package','service'));
  end if;
end
$guard$;

create index if not exists idx_products_business on products(business_id);

comment on column products.kind is
  'item | box | bundle | package | service. OORUVA is not a restaurant app; a gift shop and an electrician have to be expressible in the same table.';
comment on column products.unit is
  'Free text: per kg, per hour, per box of 12. Null where a plain unit price is meant.';

-- Products currently hang off vendor_id and are world-readable. Once they hang
-- off a business, they must follow that business's visibility.
drop policy if exists products_read on products;
create policy products_read on products
  for select using (
    business_id is null
    or exists (select 1 from businesses b where b.id = products.business_id)
  );

comment on policy products_read on products is
  'The businesses subquery is itself filtered by biz_read, so a draft business hides its products without a second copy of the visibility rule.';

-- == 6. Draft progress, so onboarding can be resumed ==========================
alter table businesses add column if not exists onboarding_step varchar(50);
alter table businesses add column if not exists submitted_at timestamptz;
alter table businesses add column if not exists verified_at timestamptz;

comment on column businesses.onboarding_step is
  'Last completed step of vendor onboarding. Lets a vendor close the app on a patchy connection and resume where they stopped instead of starting over.';
