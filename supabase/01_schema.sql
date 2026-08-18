-- ============================================================================
-- OORUVA - core schema
-- Paste into the Supabase SQL editor (Database > SQL Editor > New query).
-- Safe to re-run: every object is created IF NOT EXISTS.
-- ============================================================================

create extension if not exists "pgcrypto";

-- Identity -------------------------------------------------------------------
create table if not exists users (
  id          uuid primary key default gen_random_uuid(),
  phone       varchar(20) unique not null,
  role        varchar(50) not null check (role in ('customer', 'vendor', 'admin')),
  auth_uid    uuid unique,                       -- auth subject from Firebase/Supabase
  suspended   boolean not null default false,
  created_at  timestamptz not null default now()
);

create table if not exists customer_profiles (
  id                uuid primary key default gen_random_uuid(),
  customer_id       uuid not null references users(id) on delete cascade,
  name              varchar(100),
  location_lat      double precision,
  location_lng      double precision,
  profile_photo_url varchar(500),
  created_at        timestamptz not null default now(),
  unique (customer_id)
);

create table if not exists vendor_profiles (
  id                  uuid primary key default gen_random_uuid(),
  vendor_id           uuid not null references users(id) on delete cascade,
  business_name       varchar(200) not null,
  owner_name          varchar(150),
  business_category   varchar(100) not null,
  location_lat        double precision not null,
  location_lng        double precision not null,
  address             text,
  phone               varchar(20),
  opening_hours       varchar(200),
  description         text,
  main_photo_url      varchar(500),
  verification_status varchar(50) not null default 'pending'
                      check (verification_status in ('pending','verified','rejected','needs_changes')),
  verification_notes  text,
  views_count         integer not null default 0,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now(),
  unique (vendor_id)
);

-- Compliance -----------------------------------------------------------------
create table if not exists fssai_records (
  id              uuid primary key default gen_random_uuid(),
  vendor_id       uuid not null references users(id) on delete cascade,
  fssai_number    varchar(100),
  certificate_url varchar(500),
  status          varchar(50) not null default 'pending'
                  check (status in ('pending','verified','rejected','needs_action','needs_assistance')),
  admin_notes     text,
  verified_at     timestamptz,
  created_at      timestamptz not null default now()
);

create table if not exists verification_queue (
  id          uuid primary key default gen_random_uuid(),
  vendor_id   uuid not null references users(id) on delete cascade,
  status      varchar(50) not null default 'pending'
              check (status in ('pending','in_review','approved','rejected')),
  admin_notes text,
  reviewed_by uuid references users(id),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

-- Catalogue ------------------------------------------------------------------
create table if not exists products (
  id          uuid primary key default gen_random_uuid(),
  vendor_id   uuid not null references users(id) on delete cascade,
  name        varchar(200) not null,
  price       numeric(10,2) not null check (price >= 0),
  description text,
  photo_url   varchar(500),
  created_at  timestamptz not null default now()
);

create table if not exists vendor_photos (
  id         uuid primary key default gen_random_uuid(),
  vendor_id  uuid not null references users(id) on delete cascade,
  photo_url  varchar(500) not null,
  caption    varchar(200),
  is_main    boolean not null default false,
  created_at timestamptz not null default now()
);

-- Social ---------------------------------------------------------------------
create table if not exists reviews (
  id              uuid primary key default gen_random_uuid(),
  vendor_id       uuid not null references users(id) on delete cascade,
  customer_id     uuid not null references users(id) on delete cascade,
  rating          integer not null check (rating between 1 and 5),
  text            text,
  photo_url       varchar(500),
  vendor_response text,
  responded_at    timestamptz,
  flagged         boolean not null default false,
  created_at      timestamptz not null default now(),
  unique (vendor_id, customer_id)   -- one review per customer per vendor
);

create table if not exists posts (
  id           uuid primary key default gen_random_uuid(),
  customer_id  uuid not null references users(id) on delete cascade,
  vendor_id    uuid references users(id) on delete set null,
  caption      text,
  photo_url    varchar(500),
  likes_count  integer not null default 0,
  flagged      boolean not null default false,
  created_at   timestamptz not null default now()
);

create table if not exists post_likes (
  post_id     uuid not null references posts(id) on delete cascade,
  customer_id uuid not null references users(id) on delete cascade,
  created_at  timestamptz not null default now(),
  primary key (post_id, customer_id)
);

create table if not exists post_comments (
  id          uuid primary key default gen_random_uuid(),
  post_id     uuid not null references posts(id) on delete cascade,
  customer_id uuid not null references users(id) on delete cascade,
  text        text not null,
  flagged     boolean not null default false,
  created_at  timestamptz not null default now()
);

create table if not exists favorites (
  customer_id uuid not null references users(id) on delete cascade,
  vendor_id   uuid not null references users(id) on delete cascade,
  created_at  timestamptz not null default now(),
  primary key (customer_id, vendor_id)
);

create table if not exists check_ins (
  id          uuid primary key default gen_random_uuid(),
  customer_id uuid not null references users(id) on delete cascade,
  vendor_id   uuid not null references users(id) on delete cascade,
  created_at  timestamptz not null default now()
);

-- Rewards and offers ---------------------------------------------------------
create table if not exists rewards (
  id            uuid primary key default gen_random_uuid(),
  customer_id   uuid not null references users(id) on delete cascade,
  points        integer not null,
  activity_type varchar(100) not null,
  reference_id  uuid,                            -- the review/post/check-in that earned it
  status        varchar(50) not null default 'pending'
                check (status in ('pending','verified','credited','rejected')),
  created_at    timestamptz not null default now()
);

create table if not exists offers (
  id                  uuid primary key default gen_random_uuid(),
  vendor_id           uuid not null references users(id) on delete cascade,
  title               varchar(200) not null,
  description         text,
  discount_percentage numeric(5,2) check (discount_percentage between 0 and 100),
  discount_amount     numeric(10,2) check (discount_amount >= 0),
  points_required     integer not null default 0,
  validity_date       date,
  usage_limit         integer,
  redemptions_count   integer not null default 0,
  created_at          timestamptz not null default now()
);

create table if not exists offer_redemptions (
  id           uuid primary key default gen_random_uuid(),
  offer_id     uuid not null references offers(id) on delete cascade,
  customer_id  uuid not null references users(id) on delete cascade,
  points_spent integer not null default 0,
  created_at   timestamptz not null default now()
);

-- Platform config and audit --------------------------------------------------
create table if not exists platform_settings (
  key        varchar(100) primary key,
  value      text,
  updated_at timestamptz not null default now()
);

create table if not exists audit_log (
  id         uuid primary key default gen_random_uuid(),
  actor_id   uuid references users(id),
  action     varchar(100) not null,
  entity     varchar(100),
  entity_id  uuid,
  notes      text,
  created_at timestamptz not null default now()
);

-- Indexes for the queries the apps actually run -------------------------------
create index if not exists idx_vendor_status    on vendor_profiles(verification_status);
create index if not exists idx_vendor_category  on vendor_profiles(business_category);
create index if not exists idx_vendor_geo       on vendor_profiles(location_lat, location_lng);
create index if not exists idx_products_vendor  on products(vendor_id);
create index if not exists idx_reviews_vendor   on reviews(vendor_id);
create index if not exists idx_posts_created    on posts(created_at desc);
create index if not exists idx_rewards_customer on rewards(customer_id, status);
create index if not exists idx_offers_vendor    on offers(vendor_id);
create index if not exists idx_queue_status     on verification_queue(status);

-- Triggers --------------------------------------------------------------------
create or replace function touch_updated_at() returns trigger as $fn$
begin
  new.updated_at = now();
  return new;
end;
$fn$ language plpgsql;

drop trigger if exists trg_vendor_touch on vendor_profiles;
create trigger trg_vendor_touch before update on vendor_profiles
  for each row execute function touch_updated_at();

drop trigger if exists trg_queue_touch on verification_queue;
create trigger trg_queue_touch before update on verification_queue
  for each row execute function touch_updated_at();

-- Keep posts.likes_count honest instead of trusting the client
create or replace function sync_likes_count() returns trigger as $fn$
begin
  if (tg_op = 'INSERT') then
    update posts set likes_count = likes_count + 1 where id = new.post_id;
  elsif (tg_op = 'DELETE') then
    update posts set likes_count = greatest(likes_count - 1, 0) where id = old.post_id;
  end if;
  return null;
end;
$fn$ language plpgsql;

drop trigger if exists trg_post_like on post_likes;
create trigger trg_post_like after insert or delete on post_likes
  for each row execute function sync_likes_count();

-- A vendor joins the verification queue the moment their profile appears
create or replace function enqueue_new_vendor() returns trigger as $fn$
begin
  insert into verification_queue (vendor_id, status) values (new.vendor_id, 'pending')
  on conflict do nothing;
  return new;
end;
$fn$ language plpgsql;

drop trigger if exists trg_vendor_enqueue on vendor_profiles;
create trigger trg_vendor_enqueue after insert on vendor_profiles
  for each row execute function enqueue_new_vendor();

-- Distance helper so "vendors within 10km" is one call, not a full scan --------
create or replace function vendors_within_km(
  origin_lat double precision,
  origin_lng double precision,
  radius_km  double precision default 10
)
returns table (
  id uuid,
  vendor_id uuid,
  business_name varchar,
  business_category varchar,
  location_lat double precision,
  location_lng double precision,
  address text,
  opening_hours varchar,
  main_photo_url varchar,
  verification_status varchar,
  distance_km double precision
) as $fn$
  select v.id, v.vendor_id, v.business_name, v.business_category,
         v.location_lat, v.location_lng, v.address, v.opening_hours,
         v.main_photo_url, v.verification_status,
         6371 * acos(
           least(1.0,
             cos(radians(origin_lat)) * cos(radians(v.location_lat)) *
             cos(radians(v.location_lng) - radians(origin_lng)) +
             sin(radians(origin_lat)) * sin(radians(v.location_lat))
           )
         ) as distance_km
  from vendor_profiles v
  where v.verification_status = 'verified'
  and 6371 * acos(
        least(1.0,
          cos(radians(origin_lat)) * cos(radians(v.location_lat)) *
          cos(radians(v.location_lng) - radians(origin_lng)) +
          sin(radians(origin_lat)) * sin(radians(v.location_lat))
        )
      ) <= radius_km
  order by distance_km;
$fn$ language sql stable;
