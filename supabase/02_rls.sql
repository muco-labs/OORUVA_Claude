-- ============================================================================
-- OORUVA - row level security
-- Run AFTER 01_schema.sql. Without this every anon key can read and write
-- everything, which is the single most common way a Supabase launch leaks data.
-- ============================================================================

-- Helper: the users.id belonging to the current auth subject
create or replace function current_user_id() returns uuid as $fn$
  select id from users where auth_uid = auth.uid();
$fn$ language sql stable security definer set search_path = public, pg_temp;

create or replace function is_admin() returns boolean as $fn$
  select exists (
    select 1 from users where auth_uid = auth.uid() and role = 'admin'
  );
$fn$ language sql stable security definer set search_path = public, pg_temp;

alter table users              enable row level security;
alter table customer_profiles  enable row level security;
alter table vendor_profiles    enable row level security;
alter table fssai_records      enable row level security;
alter table verification_queue enable row level security;
alter table products           enable row level security;
alter table vendor_photos      enable row level security;
alter table reviews            enable row level security;
alter table posts              enable row level security;
alter table post_likes         enable row level security;
alter table post_comments      enable row level security;
alter table favorites          enable row level security;
alter table check_ins          enable row level security;
alter table rewards            enable row level security;
alter table offers             enable row level security;
alter table offer_redemptions  enable row level security;
alter table platform_settings  enable row level security;
alter table audit_log          enable row level security;

-- users ----------------------------------------------------------------------
drop policy if exists users_self_read on users;
create policy users_self_read on users
  for select using (auth_uid = auth.uid() or is_admin());

drop policy if exists users_self_insert on users;
create policy users_self_insert on users
  for insert with check (auth_uid = auth.uid());

drop policy if exists users_admin_write on users;
create policy users_admin_write on users
  for update using (is_admin());

-- customer_profiles ----------------------------------------------------------
drop policy if exists cp_owner_all on customer_profiles;
create policy cp_owner_all on customer_profiles
  for all using (customer_id = current_user_id() or is_admin())
  with check (customer_id = current_user_id() or is_admin());

-- vendor_profiles: world-readable once verified, owner-writable ---------------
drop policy if exists vp_public_read on vendor_profiles;
create policy vp_public_read on vendor_profiles
  for select using (
    verification_status = 'verified' or vendor_id = current_user_id() or is_admin()
  );

drop policy if exists vp_owner_write on vendor_profiles;
create policy vp_owner_write on vendor_profiles
  for insert with check (vendor_id = current_user_id());

drop policy if exists vp_owner_update on vendor_profiles;
create policy vp_owner_update on vendor_profiles
  for update using (vendor_id = current_user_id() or is_admin());

-- fssai_records: private to the vendor and admins -----------------------------
drop policy if exists fssai_owner on fssai_records;
create policy fssai_owner on fssai_records
  for all using (vendor_id = current_user_id() or is_admin())
  with check (vendor_id = current_user_id() or is_admin());

-- verification_queue: admins only, vendors may read their own row -------------
drop policy if exists vq_read on verification_queue;
create policy vq_read on verification_queue
  for select using (vendor_id = current_user_id() or is_admin());

drop policy if exists vq_admin_write on verification_queue;
create policy vq_admin_write on verification_queue
  for update using (is_admin());

-- products and photos: public read, owner write -------------------------------
drop policy if exists products_read on products;
create policy products_read on products for select using (true);

drop policy if exists products_owner on products;
create policy products_owner on products
  for all using (vendor_id = current_user_id() or is_admin())
  with check (vendor_id = current_user_id() or is_admin());

drop policy if exists photos_read on vendor_photos;
create policy photos_read on vendor_photos for select using (true);

drop policy if exists photos_owner on vendor_photos;
create policy photos_owner on vendor_photos
  for all using (vendor_id = current_user_id() or is_admin())
  with check (vendor_id = current_user_id() or is_admin());

-- reviews: public read; author writes; vendor may only add a response ---------
drop policy if exists reviews_read on reviews;
create policy reviews_read on reviews for select using (not flagged or is_admin());

drop policy if exists reviews_author_insert on reviews;
create policy reviews_author_insert on reviews
  for insert with check (customer_id = current_user_id());

drop policy if exists reviews_author_update on reviews;
create policy reviews_author_update on reviews
  for update using (customer_id = current_user_id() or vendor_id = current_user_id() or is_admin());

drop policy if exists reviews_author_delete on reviews;
create policy reviews_author_delete on reviews
  for delete using (customer_id = current_user_id() or is_admin());

-- posts, likes, comments ------------------------------------------------------
drop policy if exists posts_read on posts;
create policy posts_read on posts for select using (not flagged or is_admin());

drop policy if exists posts_author on posts;
create policy posts_author on posts
  for all using (customer_id = current_user_id() or is_admin())
  with check (customer_id = current_user_id() or is_admin());

drop policy if exists likes_read on post_likes;
create policy likes_read on post_likes for select using (true);

drop policy if exists likes_owner on post_likes;
create policy likes_owner on post_likes
  for all using (customer_id = current_user_id())
  with check (customer_id = current_user_id());

drop policy if exists comments_read on post_comments;
create policy comments_read on post_comments for select using (not flagged or is_admin());

drop policy if exists comments_author on post_comments;
create policy comments_author on post_comments
  for all using (customer_id = current_user_id() or is_admin())
  with check (customer_id = current_user_id() or is_admin());

-- favourites and check-ins: strictly private ----------------------------------
drop policy if exists fav_owner on favorites;
create policy fav_owner on favorites
  for all using (customer_id = current_user_id())
  with check (customer_id = current_user_id());

drop policy if exists checkin_owner on check_ins;
create policy checkin_owner on check_ins
  for all using (customer_id = current_user_id() or vendor_id = current_user_id() or is_admin())
  with check (customer_id = current_user_id());

-- rewards: readable by owner, writable only by the server ---------------------
-- Clients must never insert their own points; award them from an edge function
-- or the admin service key, or the loyalty scheme is trivially farmed.
drop policy if exists rewards_owner_read on rewards;
create policy rewards_owner_read on rewards
  for select using (customer_id = current_user_id() or is_admin());

drop policy if exists rewards_admin_write on rewards;
create policy rewards_admin_write on rewards
  for all using (is_admin()) with check (is_admin());

-- offers: public read, vendor writes own --------------------------------------
drop policy if exists offers_read on offers;
create policy offers_read on offers for select using (true);

drop policy if exists offers_owner on offers;
create policy offers_owner on offers
  for all using (vendor_id = current_user_id() or is_admin())
  with check (vendor_id = current_user_id() or is_admin());

drop policy if exists redemptions_owner on offer_redemptions;
create policy redemptions_owner on offer_redemptions
  for all using (customer_id = current_user_id() or is_admin())
  with check (customer_id = current_user_id() or is_admin());

-- settings and audit: admin only ----------------------------------------------
drop policy if exists settings_read on platform_settings;
create policy settings_read on platform_settings for select using (true);

drop policy if exists settings_admin on platform_settings;
create policy settings_admin on platform_settings
  for all using (is_admin()) with check (is_admin());

drop policy if exists audit_admin on audit_log;
create policy audit_admin on audit_log
  for all using (is_admin()) with check (is_admin());

-- Storage buckets -------------------------------------------------------------
-- Run once; "documents" stays private because it holds FSSAI certificates.
insert into storage.buckets (id, name, public)
  values ('photos', 'photos', true)
  on conflict (id) do nothing;

insert into storage.buckets (id, name, public)
  values ('documents', 'documents', false)
  on conflict (id) do nothing;

drop policy if exists photos_public_read on storage.objects;
create policy photos_public_read on storage.objects
  for select using (bucket_id = 'photos');

drop policy if exists photos_auth_write on storage.objects;
create policy photos_auth_write on storage.objects
  for insert with check (bucket_id = 'photos' and auth.uid() is not null);

drop policy if exists documents_owner_read on storage.objects;
create policy documents_owner_read on storage.objects
  for select using (
    bucket_id = 'documents' and (owner = auth.uid() or is_admin())
  );

drop policy if exists documents_owner_write on storage.objects;
create policy documents_owner_write on storage.objects
  for insert with check (bucket_id = 'documents' and auth.uid() is not null);
