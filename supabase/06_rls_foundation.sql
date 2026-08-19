-- ============================================================================
-- OORUVA - migration 06
-- Row level security for everything migration 04 added, written against the
-- negative cases in the brief: a customer must not reach vendor management
-- data, a vendor must not reach another vendor's, neither may reach admin
-- surfaces, and no client may write the reward ledger.
-- ============================================================================

alter table roles                 enable row level security;
alter table business_categories   enable row level security;
alter table business_types        enable row level security;
alter table business_requirements enable row level security;
alter table businesses            enable row level security;
alter table business_documents    enable row level security;
alter table verification_records  enable row level security;
alter table business_images       enable row level security;
alter table product_images        enable row level security;
alter table reward_transactions   enable row level security;
alter table communities           enable row level security;
alter table community_members     enable row level security;
alter table messages              enable row level security;
alter table support_requests      enable row level security;
alter table assistance_requests   enable row level security;
alter table notifications         enable row level security;
alter table terms_acceptance      enable row level security;
alter table analytics_events      enable row level security;
alter table orders                enable row level security;

-- ── Taxonomy: world-readable, admin-writable ───────────────────────────────
drop policy if exists roles_read on roles;
create policy roles_read on roles for select using (true);

drop policy if exists cat_read on business_categories;
create policy cat_read on business_categories for select using (active or is_admin());
drop policy if exists cat_admin on business_categories;
create policy cat_admin on business_categories for all using (is_admin()) with check (is_admin());

drop policy if exists type_read on business_types;
create policy type_read on business_types for select using (active or is_admin());
drop policy if exists type_admin on business_types;
create policy type_admin on business_types for all using (is_admin()) with check (is_admin());

drop policy if exists req_read on business_requirements;
create policy req_read on business_requirements for select using (true);
drop policy if exists req_admin on business_requirements;
create policy req_admin on business_requirements for all using (is_admin()) with check (is_admin());

-- ── Businesses ─────────────────────────────────────────────────────────────
-- A customer sees verified businesses only. A vendor additionally sees their
-- own, at any status. Nobody sees another vendor's draft.
drop policy if exists biz_read on businesses;
create policy biz_read on businesses
  for select using (
    status = 'verified' or vendor_id = current_user_id() or is_admin()
  );

drop policy if exists biz_insert on businesses;
create policy biz_insert on businesses
  for insert with check (vendor_id = current_user_id());

drop policy if exists biz_update on businesses;
create policy biz_update on businesses
  for update using (vendor_id = current_user_id() or is_admin());

drop policy if exists biz_delete on businesses;
create policy biz_delete on businesses
  for delete using (vendor_id = current_user_id() or is_admin());

-- Helper: does the caller own this business?
create or replace function owns_business(b uuid) returns boolean as $fn$
  select exists (
    select 1 from businesses
    where id = b and vendor_id = current_user_id()
  );
$fn$ language sql stable security definer;

-- ── Documents: never public. Owner and admin only, both directions. ────────
drop policy if exists doc_owner on business_documents;
create policy doc_owner on business_documents
  for all using (owns_business(business_id) or is_admin())
  with check (owns_business(business_id) or is_admin());

drop policy if exists ver_read on verification_records;
create policy ver_read on verification_records
  for select using (owns_business(business_id) or is_admin());

-- Only admins record a verification outcome. A vendor cannot mark itself
-- verified, which is the whole point of the queue.
drop policy if exists ver_admin_write on verification_records;
create policy ver_admin_write on verification_records
  for all using (is_admin()) with check (is_admin());

-- ── Media ──────────────────────────────────────────────────────────────────
drop policy if exists bimg_read on business_images;
create policy bimg_read on business_images
  for select using (approved or owns_business(business_id) or is_admin());
drop policy if exists bimg_owner on business_images;
create policy bimg_owner on business_images
  for all using (owns_business(business_id) or is_admin())
  with check (owns_business(business_id) or is_admin());

drop policy if exists pimg_read on product_images;
create policy pimg_read on product_images for select using (true);
drop policy if exists pimg_owner on product_images;
create policy pimg_owner on product_images
  for all using (
    exists (select 1 from products p
            where p.id = product_id and p.vendor_id = current_user_id())
    or is_admin()
  )
  with check (
    exists (select 1 from products p
            where p.id = product_id and p.vendor_id = current_user_id())
    or is_admin()
  );

-- ── Reward ledger: readable by its owner, writable by nobody on a client ───
drop policy if exists ledger_owner_read on reward_transactions;
create policy ledger_owner_read on reward_transactions
  for select using (customer_id = current_user_id() or is_admin());

-- Deliberately no INSERT/UPDATE policy for authenticated users. Points are
-- minted only by an edge function using the service role, which bypasses RLS.
-- Without this the loyalty scheme is farmable from a decompiled APK.
drop policy if exists ledger_admin_write on reward_transactions;
create policy ledger_admin_write on reward_transactions
  for all using (is_admin()) with check (is_admin());

-- ── Community ──────────────────────────────────────────────────────────────
drop policy if exists comm_read on communities;
create policy comm_read on communities for select using (true);
drop policy if exists comm_admin on communities;
create policy comm_admin on communities for all using (is_admin()) with check (is_admin());

drop policy if exists member_read on community_members;
create policy member_read on community_members for select using (true);
drop policy if exists member_self on community_members;
create policy member_self on community_members
  for all using (user_id = current_user_id() or is_admin())
  with check (user_id = current_user_id() or is_admin());

-- Only members may read or post in a community.
drop policy if exists msg_member_read on messages;
create policy msg_member_read on messages
  for select using (
    (not flagged and exists (
      select 1 from community_members m
      where m.community_id = messages.community_id and m.user_id = current_user_id()
    )) or is_admin()
  );

drop policy if exists msg_member_write on messages;
create policy msg_member_write on messages
  for insert with check (
    user_id = current_user_id() and exists (
      select 1 from community_members m
      where m.community_id = messages.community_id and m.user_id = current_user_id()
    )
  );

drop policy if exists msg_author on messages;
create policy msg_author on messages
  for delete using (user_id = current_user_id() or is_admin());

-- ── Support, assistance, notifications, terms ──────────────────────────────
drop policy if exists support_owner on support_requests;
create policy support_owner on support_requests
  for all using (user_id = current_user_id() or is_admin())
  with check (user_id = current_user_id() or is_admin());

drop policy if exists assist_owner on assistance_requests;
create policy assist_owner on assistance_requests
  for select using (vendor_id = current_user_id() or is_admin());
drop policy if exists assist_create on assistance_requests;
create policy assist_create on assistance_requests
  for insert with check (vendor_id = current_user_id());
-- Only an admin moves an assistance request along.
drop policy if exists assist_admin on assistance_requests;
create policy assist_admin on assistance_requests
  for update using (is_admin());

drop policy if exists notif_owner on notifications;
create policy notif_owner on notifications
  for select using (user_id = current_user_id() or is_admin());
drop policy if exists notif_read_mark on notifications;
create policy notif_read_mark on notifications
  for update using (user_id = current_user_id());

drop policy if exists terms_owner on terms_acceptance;
create policy terms_owner on terms_acceptance
  for select using (user_id = current_user_id() or is_admin());
drop policy if exists terms_insert on terms_acceptance;
create policy terms_insert on terms_acceptance
  for insert with check (user_id = current_user_id());

-- Analytics: a client may record its own events and read none of them.
drop policy if exists events_insert on analytics_events;
create policy events_insert on analytics_events
  for insert with check (user_id = current_user_id() or user_id is null);
drop policy if exists events_admin_read on analytics_events;
create policy events_admin_read on analytics_events
  for select using (is_admin());

-- Orders: the customer who placed it, the business that received it, admins.
drop policy if exists orders_read on orders;
create policy orders_read on orders
  for select using (
    customer_id = current_user_id() or owns_business(business_id) or is_admin()
  );
drop policy if exists orders_admin_write on orders;
create policy orders_admin_write on orders
  for all using (is_admin()) with check (is_admin());
