-- ============================================================================
-- OORUVA - migration 05
-- Seeds the category taxonomy. This is configuration, not code: adding a
-- category or a business type is an INSERT, never an app release.
--
-- On legal applicability: every requirement below is seeded as
-- 'requires_review'. OORUVA does not assert which registrations a business
-- legally needs until a qualified person records a basis_note. The engine is
-- built to carry that decision; it does not invent it.
-- ============================================================================

insert into business_categories (slug, name, description, sort_order) values
  ('food-beverages', 'Food & Beverages', 'Anything cooked, brewed, baked or served', 10),
  ('grocery',        'Grocery & Fresh',  'Provisions, vegetables, fruit, organic', 20),
  ('electronics',    'Electronics',      'Mobiles, computers, appliances, repair', 30),
  ('textile-fashion','Textile & Fashion','Clothing, tailoring, footwear, accessories', 40),
  ('home-hardware',  'Home & Hardware',  'Hardware, furniture, home and living', 50),
  ('beauty-care',    'Beauty & Personal Care', 'Salons, cosmetics, wellness', 60),
  ('stationery-books','Stationery & Books', 'Stationery, books, printing', 70),
  ('automotive',     'Automotive',       'Two-wheeler, four-wheeler, spares, service', 80),
  ('services',       'Services',         'Repairs, trades, professional services', 90),
  ('other',          'Other',            'Legitimate businesses outside the above', 999)
on conflict (slug) do nothing;

-- ── Business types ─────────────────────────────────────────────────────────
insert into business_types (category_id, slug, name, sort_order)
select c.id, t.slug, t.name, t.sort_order
from business_categories c
join (values
  -- Food & Beverages
  ('food-beverages','restaurant',    'Restaurant',        10),
  ('food-beverages','tea-stall',     'Tea / Coffee Stall',20),
  ('food-beverages','street-food',   'Street Food Cart',  30),
  ('food-beverages','bakery',        'Bakery',            40),
  ('food-beverages','sweet-shop',    'Sweet Shop',        50),
  ('food-beverages','juice-shop',    'Juice / Beverages', 60),
  ('food-beverages','cloud-kitchen', 'Cloud Kitchen',     70),
  ('food-beverages','catering',      'Catering',          80),
  ('food-beverages','food-box',      'Food Box / Packages', 90),
  -- Grocery & Fresh
  ('grocery','provision-store',  'Provision Store',   10),
  ('grocery','vegetable-shop',   'Vegetable Shop',    20),
  ('grocery','fruit-shop',       'Fruit Shop',        30),
  ('grocery','organic-store',    'Organic Store',     40),
  ('grocery','dairy',            'Dairy',             50),
  -- Electronics
  ('electronics','mobile-shop',      'Mobile Shop',      10),
  ('electronics','mobile-repair',    'Mobile Repair',    20),
  ('electronics','computer-shop',    'Computer Shop',    30),
  ('electronics','home-appliances',  'Home Appliances',  40),
  ('electronics','electrical-goods', 'Electrical Goods', 50),
  -- Textile & Fashion
  ('textile-fashion','clothing-store', 'Clothing Store', 10),
  ('textile-fashion','tailoring',      'Tailoring',      20),
  ('textile-fashion','footwear',       'Footwear',       30),
  ('textile-fashion','fabric-shop',    'Fabric Shop',    40),
  -- Home & Hardware
  ('home-hardware','hardware-store', 'Hardware Store', 10),
  ('home-hardware','furniture',      'Furniture',      20),
  ('home-hardware','home-living',    'Home & Living',  30),
  ('home-hardware','paint-shop',     'Paint Shop',     40),
  -- Beauty & Personal Care
  ('beauty-care','salon',          'Salon',            10),
  ('beauty-care','beauty-parlour', 'Beauty Parlour',   20),
  ('beauty-care','cosmetics-shop', 'Cosmetics Shop',   30),
  -- Stationery & Books
  ('stationery-books','stationery', 'Stationery',  10),
  ('stationery-books','bookshop',   'Book Shop',   20),
  ('stationery-books','printing',   'Printing / Xerox', 30),
  -- Automotive
  ('automotive','two-wheeler-service', 'Two-Wheeler Service', 10),
  ('automotive','car-service',         'Car Service',         20),
  ('automotive','spare-parts',         'Spare Parts',         30),
  -- Services
  ('services','electrician', 'Electrician',  10),
  ('services','plumber',     'Plumber',      20),
  ('services','laundry',     'Laundry',      30),
  ('services','courier',     'Courier',      40),
  ('services','gift-shop',   'Gift Shop / Gift Boxes', 50),
  -- Other
  ('other','other', 'Other', 10)
) as t(category_slug, slug, name, sort_order)
  on t.category_slug = c.slug
on conflict (category_id, slug) do nothing;

-- ── Requirements engine ────────────────────────────────────────────────────
-- FSSAI is attached only to types that handle food, and even then as
-- 'requires_review' rather than 'required'. Nothing here is a legal assertion.
insert into business_requirements (business_type_id, requirement_key, applicability)
select bt.id, 'fssai', 'requires_review'
from business_types bt
join business_categories c on c.id = bt.category_id
where c.slug in ('food-beverages', 'grocery')
on conflict (business_type_id, requirement_key) do nothing;

-- Explicitly not applicable where food is never handled. This one is safe to
-- state: a mobile repair shop does not come under a food regulator.
insert into business_requirements (business_type_id, requirement_key, applicability, basis_note)
select bt.id, 'fssai', 'not_applicable',
       'Category does not involve handling, storing or serving food.'
from business_types bt
join business_categories c on c.id = bt.category_id
where c.slug in ('electronics','textile-fashion','home-hardware',
                 'stationery-books','automotive')
on conflict (business_type_id, requirement_key) do nothing;

-- Udyam and GST apply on thresholds and business structure, not on category.
-- Seeded for every type as requires_review so the vendor is asked rather than
-- assumed about.
insert into business_requirements (business_type_id, requirement_key, applicability)
select bt.id, k.key, 'requires_review'
from business_types bt
cross join (values ('udyam'), ('gst')) as k(key)
on conflict (business_type_id, requirement_key) do nothing;

-- ── Districts the platform launches into ───────────────────────────────────
insert into communities (slug, name, district, description) values
  ('erode-district', 'Erode District', 'Erode',
   'Local discoveries, recommendations and questions across Erode district.')
on conflict (slug) do nothing;
