-- ============================================================================
-- OORUVA - seed data
-- Optional. Gives the apps something to render before real vendors sign up,
-- and gives the admin queue something to approve during testing.
-- ============================================================================

insert into platform_settings (key, value) values
  ('points_per_review',    '10'),
  ('points_per_photo',     '5'),
  ('points_per_post',      '2'),
  ('points_per_checkin',   '50'),
  ('points_to_rupee',      '0.20'),          -- 100 points = Rs 20
  ('support_whatsapp',     '+910000000000'),
  ('fssai_assist_fee',     '750'),
  ('vendor_review_sla_hrs','48')
on conflict (key) do nothing;

-- Demo users -----------------------------------------------------------------
insert into users (id, phone, role) values
  ('11111111-1111-1111-1111-111111111111', '+919876543210', 'customer'),
  ('22222222-2222-2222-2222-222222222222', '+919876543211', 'vendor'),
  ('33333333-3333-3333-3333-333333333333', '+919876543212', 'vendor'),
  ('44444444-4444-4444-4444-444444444444', '+919000000000', 'admin')
on conflict (phone) do nothing;

insert into customer_profiles (customer_id, name, location_lat, location_lng) values
  ('11111111-1111-1111-1111-111111111111', 'Muthu', 13.0827, 80.2707)
on conflict (customer_id) do nothing;

insert into vendor_profiles
  (vendor_id, business_name, owner_name, business_category,
   location_lat, location_lng, address, phone, opening_hours, description,
   verification_status)
values
  ('22222222-2222-2222-2222-222222222222', 'Chai Wali', 'Lakshmi', 'Chai',
   13.0827, 80.2707, 'Main Street, T. Nagar', '+919876543211', '06:00-22:00',
   'Authentic Indian tea and snacks, brewed since 1998.', 'verified'),
  ('33333333-3333-3333-3333-333333333333', 'Street Samosa', 'Ravi', 'Food',
   13.0835, 80.2715, 'Market Road, T. Nagar', '+919876543212', '11:00-20:00',
   'Crispy samosas and pakora, fried to order.', 'pending')
on conflict (vendor_id) do nothing;

insert into products (vendor_id, name, price, description) values
  ('22222222-2222-2222-2222-222222222222', 'Masala chai',    12.00, 'Ginger, cardamom, full cream milk'),
  ('22222222-2222-2222-2222-222222222222', 'Filter coffee',  15.00, 'Chicory blend, steel tumbler'),
  ('22222222-2222-2222-2222-222222222222', 'Vada',           20.00, 'Fried fresh, two per plate'),
  ('33333333-3333-3333-3333-333333333333', 'Samosa',         15.00, 'Potato and pea, mint chutney'),
  ('33333333-3333-3333-3333-333333333333', 'Onion pakora',   25.00, 'By weight, served hot')
on conflict do nothing;

insert into fssai_records (vendor_id, fssai_number, status) values
  ('22222222-2222-2222-2222-222222222222', '1234-5678-9012', 'verified'),
  ('33333333-3333-3333-3333-333333333333', null, 'needs_assistance')
on conflict do nothing;

insert into reviews (vendor_id, customer_id, rating, text) values
  ('22222222-2222-2222-2222-222222222222',
   '11111111-1111-1111-1111-111111111111', 5,
   'Amazing chai and snacks. Best in the area.')
on conflict (vendor_id, customer_id) do nothing;

insert into offers (vendor_id, title, description, discount_percentage, points_required, validity_date) values
  ('22222222-2222-2222-2222-222222222222', 'Second chai half price',
   'Any two cups between 4pm and 6pm', 50.00, 100, current_date + 30)
on conflict do nothing;
