# OORUVA — technical overview

## What exists

| Piece | State |
|---|---|
| Android app (customer + vendor flavors) | Builds, installs, runs on device |
| Quiet Luxury design system | Complete — tokens, bundled type, components |
| Supabase schema + RLS + seed | Written, **not yet run** (needs your project) |
| Kotlin data layer | Client, DTOs, VendorRepository — falls back to mock without keys |
| React admin console | Complete, runs on demo data until `.env` is filled |
| Firebase phone auth | **Not implemented** — auth is still mock |

## Stack

- Gradle 9.7.0, AGP 9.3.1, Kotlin 2.4.10 (via AGP built-in Kotlin — no `kotlin-android` plugin)
- compileSdk 37, targetSdk 36, minSdk 26, JVM target 17
- Compose BOM 2026.08.00, Navigation Compose 2.9.8
- Supabase Kotlin BOM 3.0.3 (postgrest, storage, auth) over Ktor/OkHttp
- Admin: React 18, Vite 6, Tailwind 3, `@supabase/supabase-js` 2

The versions in the original brief (Kotlin 1.9.10, Compose 1.6.0, SDK 34) cannot
build on this machine: the only JDK present is the Android Studio JBR 25, and
that toolchain predates JDK 25 support. The stack above is the nearest one that
compiles here, verified across nine builds.

## Two apps, one codebase

Product flavors, not two repositories:

```
./gradlew assembleCustomerDebug   → com.ooruva.app.customer  "OORUVA"
./gradlew assembleVendorDebug     → com.ooruva.app.vendor    "OORUVA Vendor"
```

Different application IDs mean both install side by side. `BuildConfig.AUDIENCE`
tells the app which dashboard to open. Shared design system, models, and data
layer stay in `src/main` and are maintained once.

## Layout

```
ooruva/
├── app/src/main/kotlin/com/ooruva/app/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── models/          UI models + UserRole
│   │   ├── remote/          Supabase client, DTOs, DataResult
│   │   └── repository/      VendorRepository
│   ├── ui/
│   │   ├── components/      Brand, PremiumButton, FabNavigation
│   │   ├── navigation/      NavGraph (role-based)
│   │   ├── screens/         16 screens
│   │   └── theme/           Color, Typography, Brand, Spacing, Theme
│   └── res/                 fonts, vectors, themes
├── supabase/                01_schema, 02_rls, 03_seed, SETUP.md
├── admin/                   React console
└── docs/                    these guides
```

## Data flow

Screens call a repository. Repositories return `DataResult`, which is `Success`
(with a `fromMock` flag), `Failure(message)`, or `Loading`. When
`BuildConfig.SUPABASE_URL` is blank, `Supabase.client` is null and repositories
serve mock data — so a fresh clone builds and runs with no backend, and the UI
can say plainly which it is showing.

## Security posture

- `supabase/02_rls.sql` is not optional. The anon key ships inside the APK and is
  readable by anyone who unzips it; **RLS is the only thing protecting the data.**
- FSSAI certificates go to the private `documents` bucket, served through
  five-minute signed URLs. Photos go to the public `photos` bucket.
- Rewards are read-only to clients. Points must be written by an edge function or
  the service key, otherwise anyone can mint their own.
- The `service_role` key belongs in neither app nor the admin bundle.
- Keys live in `local.properties` and `admin/.env`, both git-ignored.

## Known gaps

1. **Auth is mock.** Any ten digits, any six-digit code. Firebase phone auth is
   set up in the guide but not wired into `AuthScreen`. Nothing persists a session.
2. **Most screens still read mock data.** `VendorRepository` is real; Home,
   Community, Rewards, Group Finder and the vendor dashboard still use hardcoded
   lists. Each needs its repository call swapped in.
3. **Maps is a hand-drawn canvas**, not the Maps SDK. The dependency and key
   placeholder are in place; `MapScreen` needs a `GoogleMap` composable.
4. **Location permission is requested but never read.** Distances are constant.
5. **No tests**, no crash reporting, no offline handling.
6. **Debug signing only** — a release keystore is needed before any store upload.
7. **Admin has no login.** `is_admin()` gates writes in Postgres, but the console
   itself is unauthenticated. Do not deploy it publicly as-is.

## Build commands

```bash
./gradlew -g D:/gradle-home assembleCustomerDebug
./gradlew -g D:/gradle-home assembleVendorDebug
```

`-g D:/gradle-home` is needed on this machine: `C:` has under 3 GB free and the
Gradle cache will not fit. Roughly 2–3 GB of free RAM is also required; a
"insufficient memory for the Java Runtime Environment" failure is the OS, not
the code.
