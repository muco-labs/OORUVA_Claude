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
```

The build reads them into `BuildConfig` and the manifest — see
`app/build.gradle.kts`. If they are missing, the app still compiles and runs on
mock data, so an absent key never breaks the build.

For the admin dashboard, copy `admin/.env.example` to `admin/.env` and fill in
the same project URL plus the anon key. The `service_role` key never goes in the
React app either — anything needing it belongs in a Supabase edge function.

---

## 5. Verify before moving on

- [ ] `select count(*) from vendor_profiles;` returns 2 (with seed data)
- [ ] Storage shows buckets `photos` (public) and `documents` (private)
- [ ] Authentication → Sign-in method shows Phone enabled
- [ ] `google-services.json` sits in `app/`
- [ ] Maps key is restricted to your two package names
- [ ] `local.properties` holds all three values and is **not** committed
