-- ============================================================================
-- OORUVA - migration 09
-- Storage scoping, search, and the index nearby search actually needs.
--
-- Run after 08_admin_access.sql. Idempotent.
--
-- WHY THIS EXISTS
-- ---------------
-- 1. The storage policies from 02 let *any* authenticated user write anywhere
--    in either bucket. Reads were owner-scoped, so nobody could read another
--    vendor's certificate — but anyone with an account could drop files into
--    another business's folder, and an admin reviewing that business would see
--    them sitting alongside the real ones. Writes are now scoped to a business
--    the caller actually owns.
--
-- 2. nearby_businesses() filtered on a haversine expression, which no index can
--    serve: every call was a sequential scan over every business with a
--    trigonometric evaluation per row. A bounding box added in front of it does
--    the same job with a plain btree.
--
-- 3. Search was `ilike '%query%'`, which cannot use an index either.
-- ============================================================================

-- == 1. Storage: writes scoped to an owned business ==========================

-- Both buckets use  <business_id>/<filename>  as the path, so the first folder
-- segment identifies the owner. A malformed or non-UUID prefix must fail
-- closed rather than error, or a bad upload path becomes a 500 instead of a
-- refusal.
create or replace function storage_business_id(object_name text)
returns uuid as $fn$
declare
  first_segment text;
begin
  first_segment := split_part(object_name, '/', 1);
  if first_segment !~ '^[0-9a-fA-F-]{36}$' then
    return null;
  end if;
  return first_segment::uuid;
exception
  when others then
    return null;
end;
$fn$ language plpgsql immutable;

comment on function storage_business_id is
  'Extracts the owning business id from a storage path. Returns null rather than raising on anything unexpected, so a policy using it denies instead of erroring.';

drop policy if exists photos_auth_write on storage.objects;
create policy photos_auth_write on storage.objects
  for insert with check (
    bucket_id = 'photos'
    and owns_business(storage_business_id(name))
  );

drop policy if exists photos_owner_update on storage.objects;
create policy photos_owner_update on storage.objects
  for update using (
    bucket_id = 'photos' and (owns_business(storage_business_id(name)) or is_admin())
  );

drop policy if exists photos_owner_delete on storage.objects;
create policy photos_owner_delete on storage.objects
  for delete using (
    bucket_id = 'photos' and (owns_business(storage_business_id(name)) or is_admin())
  );

drop policy if exists documents_owner_write on storage.objects;
create policy documents_owner_write on storage.objects
  for insert with check (
    bucket_id = 'documents'
    and owns_business(storage_business_id(name))
  );

-- Read by ownership of the business rather than by storage.objects.owner. The
-- owner column records whoever uploaded the file; if a vendor is ever migrated
-- to a new account, or an admin uploads on their behalf during assisted
-- onboarding, owner-based read silently stops working for the actual owner.
drop policy if exists documents_owner_read on storage.objects;
create policy documents_owner_read on storage.objects
  for select using (
    bucket_id = 'documents'
    and (owns_business(storage_business_id(name)) or is_admin())
  );

drop policy if exists documents_owner_delete on storage.objects;
create policy documents_owner_delete on storage.objects
  for delete using (
    bucket_id = 'documents'
    and (owns_business(storage_business_id(name)) or is_admin())
  );

-- == 2. Indexes for discovery ================================================

create index if not exists idx_businesses_lat_lng
  on businesses (location_lat, location_lng)
  where location_lat is not null and location_lng is not null;

create index if not exists idx_businesses_status_type
  on businesses (status, business_type_id);

-- Rewritten with a bounding-box prefilter. The haversine still decides the
-- final answer -- a box is square and the radius is round -- but it only runs
-- on rows the index already narrowed to, instead of on the whole table.
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
  with bounds as (
    select
      radius_km / 111.045 as d_lat,
      -- Longitude degrees shrink towards the poles. The guard keeps this from
      -- dividing by ~0 near them; at Indian latitudes it never binds, but a
      -- silently infinite box would be an odd thing to leave in.
      radius_km / greatest(0.01, 111.045 * cos(radians(origin_lat))) as d_lng
  )
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
    cross join bounds
    left join business_types bt      on bt.id = b.business_type_id
    left join business_categories bc on bc.id = bt.category_id
   where b.location_lat between origin_lat - bounds.d_lat and origin_lat + bounds.d_lat
     and b.location_lng between origin_lng - bounds.d_lng and origin_lng + bounds.d_lng
     and (type_filter is null or b.business_type_id = type_filter)
     and 6371 * acos(least(1.0,
           cos(radians(origin_lat)) * cos(radians(b.location_lat)) *
           cos(radians(b.location_lng) - radians(origin_lng)) +
           sin(radians(origin_lat)) * sin(radians(b.location_lat))
         )) <= radius_km
   order by distance_km
   limit greatest(1, least(max_results, 200));
$fn$ language sql stable security invoker;

comment on function nearby_businesses is
  'Security invoker on purpose: the biz_read policy still decides what the caller may see, so this cannot be used to enumerate drafts.';

-- == 3. Full-text search =====================================================

-- 'simple' rather than 'english'. These are Indian business names -- "Anjappar",
-- "Sri Balaji Stores", "Idli Kadai" -- and an English stemmer mangles them
-- while helping almost nothing. simple lowercases and splits, which is what is
-- actually wanted here.
alter table businesses add column if not exists search_vector tsvector
  generated always as (
    to_tsvector('simple',
      coalesce(name, '') || ' ' ||
      coalesce(description, '') || ' ' ||
      coalesce(district, '') || ' ' ||
      coalesce(address, '')
    )
  ) stored;

create index if not exists idx_businesses_search
  on businesses using gin (search_vector);

create or replace function search_businesses(
  term text,
  max_results integer default 50
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
  rank real
) as $fn$
  -- A trailing :* makes this behave as prefix search, so "chai" matches
  -- "Chaiwala" as the person is still typing. websearch_to_tsquery does not
  -- offer prefixes, and plainto_ does not either, hence building the query.
  with q as (
    select to_tsquery('simple',
      array_to_string(
        array(
          select lexeme || ':*'
            from unnest(string_to_array(lower(regexp_replace(term, '[^\w\s]', ' ', 'g')), ' ')) as lexeme
           where lexeme <> ''
        ),
        ' & '
      )
    ) as tsq
  )
  select b.id, b.vendor_id, b.name, b.business_type_id,
         bt.name as type_name, bc.slug as category_slug,
         b.address, b.district, b.location_lat, b.location_lng,
         b.opening_hours, b.main_photo_url, b.status,
         ts_rank(b.search_vector, q.tsq) as rank
    from businesses b
    cross join q
    left join business_types bt      on bt.id = b.business_type_id
    left join business_categories bc on bc.id = bt.category_id
   where q.tsq is not null
     and b.search_vector @@ q.tsq
   order by rank desc, b.name
   limit greatest(1, least(max_results, 200));
$fn$ language sql stable security invoker;

comment on function search_businesses is
  'Prefix full-text search. Security invoker, so biz_read still governs visibility -- a draft cannot be found by guessing its name.';
