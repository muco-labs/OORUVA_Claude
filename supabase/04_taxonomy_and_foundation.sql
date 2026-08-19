-- ============================================================================
-- OORUVA - migration 04
-- Foundation tables missing from 01, plus the configurable category taxonomy
-- and requirements engine.
--
-- Run after 01_schema.sql and 02_rls.sql. Idempotent.
-- ============================================================================

-- ── Roles as data, not a bare string check ──────────────────────────────────
create table if not exists roles (
  key         varchar(50) primary key,
  label       varchar(100) not null,
  description text,
  -- A client may never self-assign a role where this is false. Enforced in the
  -- auth-bootstrap edge function and by the trigger further down.
  self_assignable boolean not null default false
);

insert into roles (key, label, description, self_assignable) values
  ('customer', 'Customer', 'Discovers businesses, reviews, earns rewards', true),
  ('vendor',   'Vendor',   'Owns and manages a business listing',          true),
  ('admin',    'Admin',    'Platform administration and verification',     false)
on conflict (key) do nothing;

-- ── Category taxonomy: Category -> Business Type -> Requirements ────────────
create table if not exists business_categories (
  id          uuid primary key default gen_random_uuid(),
  slug        varchar(80) unique not null,
  name        varchar(120) not null,
  description text,
  icon        varchar(80),
  sort_order  integer not null default 100,
  active      boolean not null default true,
  created_at  timestamptz not null default now()
);

create table if not exists business_types (
  id          uuid primary key default gen_random_uuid(),
  category_id uuid not null references business_categories(id) on delete cascade,
  slug        varchar(80) not null,
  name        varchar(120) not null,
  description text,
  sort_order  integer not null default 100,
  active      boolean not null default true,
  created_at  timestamptz not null default now(),
  unique (category_id, slug)
);

-- What a given business type has to produce before it can be verified.
-- Applicability is configuration, never hardcoded per business.
create table if not exists business_requirements (
  id                uuid primary key default gen_random_uuid(),
  business_type_id  uuid not null references business_types(id) on delete cascade,
  requirement_key   varchar(60) not null,      -- fssai | udyam | gst | trade_licence | ...
  applicability     varchar(30) not null default 'requires_review'
                    check (applicability in ('required','optional','not_applicable','requires_review')),
  -- Why this applicability was set. Left null until a human decides; the app
  -- must not present an unreviewed applicability as legal advice.
  basis_note        text,
  reviewed_by       uuid references users(id),
  reviewed_at       timestamptz,
  created_at        timestamptz not null default now(),
  unique (business_type_id, requirement_key)
);

comment on column business_requirements.applicability is
  'requires_review is the default on purpose. OORUVA does not assert legal '
  'applicability until a qualified human has recorded a basis_note.';

-- ── Businesses as first-class rows (a vendor may hold more than one) ────────
create table if not exists businesses (
  id                  uuid primary key default gen_random_uuid(),
  vendor_id           uuid not null references users(id) on delete cascade,
  business_type_id    uuid references business_types(id),
  name                varchar(200) not null,
  owner_name          varchar(150),
  description         text,
  address             text,
  district            varchar(120),
  location_lat        double precision,
  location_lng        double precision,
  phone               varchar(20),
  opening_hours       varchar(200),
  main_photo_url      varchar(500),
  status              varchar(40) not null default 'draft'
                      check (status in ('draft','submitted','verified','rejected','needs_changes','suspended')),
  verification_notes  text,
  profile_completeness integer not null default 0,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now()
);

create table if not exists business_documents (
  id            uuid primary key default gen_random_uuid(),
  business_id   uuid not null references businesses(id) on delete cascade,
  document_type varchar(60) not null,          -- fssai | udyam | gst | other
  document_number varchar(120),
  storage_path  varchar(500),                  -- private bucket path, never a public URL
  status        varchar(40) not null default 'submitted'
                check (status in ('submitted','under_review','verified','rejected','needs_action')),
  admin_notes   text,
  reviewed_by   uuid references users(id),
  reviewed_at   timestamptz,
  created_at    timestamptz not null default now()
);

create table if not exists verification_records (
  id            uuid primary key default gen_random_uuid(),
  business_id   uuid not null references businesses(id) on delete cascade,
  method        varchar(60) not null,          -- manual_review | official_api | document_check
  outcome       varchar(40) not null check (outcome in ('pending','passed','failed','inconclusive')),
  checked_field varchar(120),                  -- e.g. business_name, licence_number
  evidence_note text,
  performed_by  uuid references users(id),
  created_at    timestamptz not null default now()
);

comment on table verification_records is
  'One row per verification attempt. method=official_api is only permitted where '
  'an authorised API exists; OORUVA never scrapes or bypasses a government portal.';

-- ── Media, split by sensitivity ─────────────────────────────────────────────
create table if not exists business_images (
  id          uuid primary key default gen_random_uuid(),
  business_id uuid not null references businesses(id) on delete cascade,
  storage_path varchar(500) not null,
  caption     varchar(200),
  is_main     boolean not null default false,
  approved    boolean not null default false,
  created_at  timestamptz not null default now()
);

create table if not exists product_images (
  id         uuid primary key default gen_random_uuid(),
  product_id uuid not null references products(id) on delete cascade,
  storage_path varchar(500) not null,
  is_main    boolean not null default false,
  created_at timestamptz not null default now()
);

-- ── Rewards ledger: balance is derived, never stored ────────────────────────
create table if not exists reward_transactions (
  id           uuid primary key default gen_random_uuid(),
  customer_id  uuid not null references users(id) on delete cascade,
  direction    varchar(10) not null check (direction in ('credit','debit')),
  points       integer not null check (points > 0),
  activity_type varchar(80) not null,
  reference_id uuid,
  status       varchar(30) not null default 'pending'
               check (status in ('pending','verified','credited','rejected','reversed')),
  note         text,
  created_by   uuid references users(id),
  created_at   timestamptz not null default now()
);

comment on table reward_transactions is
  'Append-only ledger. A customer balance is sum(credited credits) - sum(credited '
  'debits), computed on read. No client may insert here; see RLS in 05.';

-- ── Community ───────────────────────────────────────────────────────────────
create table if not exists communities (
  id          uuid primary key default gen_random_uuid(),
  slug        varchar(100) unique not null,
  name        varchar(150) not null,
  district    varchar(120),
  description text,
  created_by  uuid references users(id),
  created_at  timestamptz not null default now()
);

create table if not exists community_members (
  community_id uuid not null references communities(id) on delete cascade,
  user_id      uuid not null references users(id) on delete cascade,
  role         varchar(30) not null default 'member' check (role in ('member','moderator','owner')),
  joined_at    timestamptz not null default now(),
  primary key (community_id, user_id)
);

create table if not exists messages (
  id           uuid primary key default gen_random_uuid(),
  community_id uuid not null references communities(id) on delete cascade,
  user_id      uuid not null references users(id) on delete cascade,
  body         text not null,
  flagged      boolean not null default false,
  created_at   timestamptz not null default now()
);

-- ── Support, assistance, notifications, terms ───────────────────────────────
create table if not exists support_requests (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references users(id) on delete cascade,
  subject    varchar(200),
  body       text,
  status     varchar(30) not null default 'open' check (status in ('open','in_progress','resolved','closed')),
  created_at timestamptz not null default now()
);

create table if not exists assistance_requests (
  id           uuid primary key default gen_random_uuid(),
  business_id  uuid references businesses(id) on delete cascade,
  vendor_id    uuid not null references users(id) on delete cascade,
  request_type varchar(60) not null,           -- fssai | udyam | gst | other
  status       varchar(40) not null default 'requested'
               check (status in ('requested','contacted','in_progress','completed','cancelled')),
  contact_phone varchar(20),
  notes        text,
  assigned_to  uuid references users(id),
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

create table if not exists notifications (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references users(id) on delete cascade,
  title      varchar(200) not null,
  body       text,
  kind       varchar(60),
  read_at    timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists terms_acceptance (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references users(id) on delete cascade,
  terms_key    varchar(60) not null,           -- customer_terms | vendor_terms
  terms_version varchar(40) not null,
  accepted_at  timestamptz not null default now(),
  unique (user_id, terms_key, terms_version)
);

create table if not exists analytics_events (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid references users(id) on delete set null,
  event      varchar(80) not null,
  entity     varchar(80),
  entity_id  uuid,
  properties jsonb,
  created_at timestamptz not null default now()
);

-- Orders exist so the vendor dashboard has somewhere truthful to read from the
-- day payments are enabled. Until then it stays empty and the UI says so.
create table if not exists orders (
  id          uuid primary key default gen_random_uuid(),
  business_id uuid not null references businesses(id) on delete cascade,
  customer_id uuid references users(id) on delete set null,
  amount      numeric(10,2) not null check (amount >= 0),
  status      varchar(30) not null default 'pending'
              check (status in ('pending','paid','cancelled','refunded')),
  created_at  timestamptz not null default now()
);

-- ── Indexes ────────────────────────────────────────────────────────────────
create index if not exists idx_business_vendor   on businesses(vendor_id);
create index if not exists idx_business_status   on businesses(status);
create index if not exists idx_business_type     on businesses(business_type_id);
create index if not exists idx_types_category    on business_types(category_id);
create index if not exists idx_reqs_type         on business_requirements(business_type_id);
create index if not exists idx_docs_business     on business_documents(business_id);
create index if not exists idx_ledger_customer   on reward_transactions(customer_id, status);
create index if not exists idx_messages_community on messages(community_id, created_at desc);
create index if not exists idx_assist_status     on assistance_requests(status);
create index if not exists idx_events_event      on analytics_events(event, created_at desc);

drop trigger if exists trg_business_touch on businesses;
create trigger trg_business_touch before update on businesses
  for each row execute function touch_updated_at();

drop trigger if exists trg_assist_touch on assistance_requests;
create trigger trg_assist_touch before update on assistance_requests
  for each row execute function touch_updated_at();

-- ── Role cannot be self-escalated ──────────────────────────────────────────
create or replace function guard_role_assignment() returns trigger as $fn$
declare
  allowed boolean;
begin
  select self_assignable into allowed from roles where key = new.role;
  if allowed is null then
    raise exception 'Unknown role: %', new.role;
  end if;
  -- A client arriving through PostgREST always carries a role claim of 'anon'
  -- or 'authenticated'. Its absence means a direct connection: a migration, a
  -- seed, or an edge function holding the service key. Only those may mint a
  -- role that is not self-assignable.
  if not allowed
     and coalesce(current_setting('request.jwt.claim.role', true), '') in ('anon', 'authenticated')
  then
    raise exception 'Role % cannot be self-assigned', new.role
      using hint = 'Create privileged roles through auth-bootstrap or admin tooling.';
  end if;
  return new;
end;
$fn$ language plpgsql;

drop trigger if exists trg_guard_role on users;
create trigger trg_guard_role before insert or update of role on users
  for each row execute function guard_role_assignment();
