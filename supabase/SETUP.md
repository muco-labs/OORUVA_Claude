# Day 1 — infrastructure setup

Three accounts have to be created by you. I can't create accounts or handle
passwords, and you should not paste any key into a chat window. Everything below
takes about 25 minutes and is free.

---

## 1. Supabase (database + file storage)

1. Sign up at <https://supabase.com> and create a project.
2. **Region: Mumbai (ap-south-1)** — closest to Chennai, roughly 40 ms versus
   250 ms from Virginia. This choice is permanent for the project.
3. Set a strong database password and store it in a password manager.
4. Open **SQL Editor → New query** and run, in this order:
   - `supabase/01_schema.sql` — 18 core tables, indexes, triggers, distance function
   - `supabase/02_rls.sql` — row level security, and creates both storage buckets
   - `supabase/03_seed.sql` — optional demo data (development only)
   - `supabase/04_taxonomy_and_foundation.sql` — roles, category taxonomy,
     businesses, documents, verification records, reward ledger, communities,
     support, assistance, notifications, terms, analytics, orders
   - `supabase/05_taxonomy_seed.sql` — 10 categories, 40 business types,
     requirements engine
   - `supabase/06_rls_foundation.sql` — RLS for everything migration 04 added
   - `supabase/07_identity_and_model.sql` — identity correction, nearby search
     on `businesses`, reward rules, product kinds, resumable onboarding
   - `supabase/08_admin_access.sql` — `grant_admin()` / `revoke_admin()`
   - `supabase/09_storage_and_search.sql` — storage writes scoped to an owned
     business, bounding-box nearby search, full-text search
   - `supabase/10_reward_integrity.sql` — award idempotency, overdraft guard,
     offer usage limits

You can check all of this applies cleanly before touching a cloud project:

```bash
PGROOT=/path/to/postgres ./supabase/tests/run_tests.sh
```

That builds a throwaway database from the migrations and runs 95 assertions
against it — mostly negative ones — a customer reaching vendor documents, a vendor
minting reward points, an anonymous caller reading the ledger. It needs no
credentials and touches nothing remote.
5. **Settings → API** gives you three values:
   - `Project URL`
   - `anon public` key — safe in the app
   - `service_role` key — **never** in the app, admin/server only

> `02_rls.sql` matters more than it looks. Without it the anon key can read and
> write every table, including other people's phone numbers and FSSAI
> certificates. Run it before a single real user signs up.

## 2. Firebase (phone OTP)

1. <https://console.firebase.google.com> → add project → name it `OORUVA`.
2. **Build → Authentication → Sign-in method → Phone → Enable.**
3. **Project settings → Your apps → Add Android app.**
   - Customer package: `com.ooruva.app.customer`
   - Vendor package: `com.ooruva.app.vendor`
   - Add **both**; one `google-services.json` covers them.
4. Add your debug SHA-1 (phone auth refuses to work without it):
   ```bash
   keytool -list -v -keystore "$HOME/.android/debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
   Paste the SHA-1 into the Android app settings, then re-download
   `google-services.json`.
5. Drop `google-services.json` into `app/` — it is already git-ignored.

> Free tier gives 10,000 verifications a month. Indian SMS delivery is reliable
> on Jio/Airtel/Vi; add test numbers under Phone → Advanced while developing so
> you aren't burning real SMS on every build.

## 3. Google Maps

1. <https://console.cloud.google.com> → new project → **APIs & Services**.
2. Enable **Maps SDK for Android**.
3. **Credentials → Create credentials → API key.**
4. Restrict it immediately: Application restriction → Android apps → add both
   package names with the same SHA-1; API restriction → Maps SDK for Android.
5. Billing must be enabled even on free tier; the $200/month credit covers far
   more than 25,000 map loads.

---

## 4. Where the keys go

Put them in `local.properties` in the project root. That file is git-ignored, so
nothing secret is committed:

```properties
sdk.dir=C:/Users/ELCOT/AppData/Local/Android/Sdk

# Per environment. Debug builds read _DEV, staging _STAGING, release _PROD,
# so a development build can never reach production data.
SUPABASE_URL_DEV=https://YOUR-DEV-PROJECT.supabase.co
SUPABASE_ANON_KEY_DEV=<dev anon key>
MAPS_API_KEY_DEV=<restricted dev key>

# SUPABASE_URL_STAGING= ...   SUPABASE_URL_PROD= ...
# Unsuffixed keys still work as a fallback for a single-project setup.
```

### Deploy the auth bootstrap function

The only path that creates an OORUVA user record, because the anon key is public
and a self-inserted `users` row could claim `role = 'admin'`.

```bash
supabase functions deploy auth-bootstrap
supabase secrets set FIREBASE_PROJECT_ID=<your firebase project id>
supabase secrets set SUPABASE_JWT_SECRET=<Settings > API > JWT Settings>
```

`SUPABASE_JWT_SECRET` is what lets the function mint a session. Without it
sign-in verifies the phone number and then hands back nothing Postgres will
accept: every RLS policy keys off `auth.uid()`, so the app would authenticate
and then be unable to read or write any of its own data. It is the single most
important secret here — treat it like the service role key.

### Deploy the rewards function

```bash
supabase functions deploy rewards
```

It reads the same `SUPABASE_JWT_SECRET`, so no new secret is needed. This is the
only thing that writes `reward_transactions` — there is no client INSERT policy
on that table, deliberately. Before paying for an action it looks the action up
and checks it belongs to the caller, so a client asking to be paid for someone
else's review, or for a review that does not exist, gets nothing.

The function returns `access_token`, and the app sends that on every PostgREST
and Storage call. The token's `sub` is the OORUVA `users.id`, not the Firebase
UID: Supabase casts `sub` to `uuid` and a Firebase UID is not one. The Firebase
UID lives in `users.firebase_uid` and is used only to find the account.

The build reads them into `BuildConfig` and the manifest — see
`app/build.gradle.kts`. If they are missing, the app still compiles and runs on
mock data, so an absent key never breaks the build.

For the admin dashboard, copy `admin/.env.example` to `admin/.env` and fill in
the same project URL plus the anon key. The `service_role` key never goes in the
React app either — anything needing it belongs in a Supabase edge function.

---

## 4a. Storage paths

Both buckets use `<business_id>/<filename>`. Migration 09 refuses any write
whose first path segment is not a business the caller owns, so the path is a
permission check, not a filing convention — building one differently produces a
refused upload rather than a misfiled object.

`documents` stays private. It is read only through a signed URL that expires in
minutes, never a public link: these are people's licence certificates.

## 4b. Create an admin

The console signs in with Supabase email + password rather than phone OTP — a
browser has no SIM, and an admin account should be created deliberately rather
than by anyone who can receive an SMS.

1. **Authentication → Users → Add user.** Real address, strong password from a
   password manager. Copy the generated user UID.
2. In the SQL editor, with that UID:

   ```sql
   select grant_admin('<auth user uid>', '+91XXXXXXXXXX');
   ```

3. Check: `select id, role from users where role = 'admin';`

There is no self-service path and there is not meant to be. `roles.admin` is
not self-assignable, a trigger enforces it, and `auth-bootstrap` refuses to
mint the role. `grant_admin()` needs direct database access, which no client
key grants.

## 5. Verify before moving on

- [ ] `select count(*) from vendor_profiles;` returns 2 (with seed data)
- [ ] Storage shows buckets `photos` (public) and `documents` (private)
- [ ] Authentication → Sign-in method shows Phone enabled
- [ ] `google-services.json` sits in `app/`
- [ ] Maps key is restricted to your two package names
- [ ] `local.properties` holds all three values and is **not** committed
- [ ] `supabase secrets list` shows `FIREBASE_PROJECT_ID` and `SUPABASE_JWT_SECRET`
- [ ] `select count(*) from business_categories;` returns 10
- [ ] `select count(*) from business_types;` returns 40
- [ ] At least one row in `users` with `role = 'admin'`
- [ ] `./supabase/tests/run_tests.sh` reports `failed=0  sql_errors=0`
- [ ] `select count(*) from reward_rules;` returns 5
- [ ] `supabase functions list` shows `auth-bootstrap` and `rewards`
- [ ] Storage shows buckets `photos` (public) and `documents` (private)
