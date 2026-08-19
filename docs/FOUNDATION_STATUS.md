# OORUVA — backend foundation status

Written at the end of the foundation phase. States plainly what exists, what is
connected, what is verified, and what is still blocked. Nothing here claims
production readiness.

---

## Phase 1 — environment foundation

| # | Item | State | Note |
|---|---|---|---|
| 1 | Android flavor architecture | **Verified** | `customer` / `vendor`, distinct applicationIds, own source sets and nav graphs |
| 2 | Admin web env structure | **Verified** | `.env.example` with placeholders, `.env` git-ignored |
| 3 | Dev / staging / prod separation | **Built this phase** | Three build types, each reading its own Supabase project |
| 4 | Firebase Auth connection | **Wired, unconnected** | SDK added; plugin applies only when `google-services.json` exists |
| 5 | Firebase UID → Supabase user | **Designed and written** | `auth-bootstrap` edge function is the sole creation path |
| 6 | Supabase DB configuration | **Written, unapplied** | 6 migration files; no project to run them against |
| 7 | RLS strategy | **Written, unenforced** | Migrations 02 and 06 |
| 8 | Storage buckets | **Defined, uncreated** | `photos` public, `documents` private |
| 9 | Edge Functions / API | **Boundary started** | `auth-bootstrap` written; the rest is a plan, not code |
| 10 | Maps integration | **Placeholder** | Dependency and manifest key wired; screen is still a drawn canvas |
| 11 | Notifications | **Absent** | No FCM, deliberately deferred |
| 12 | No secrets in Git | **Verified** | See below |

### Item 12 evidence

```
git ls-files | grep -E 'local.properties|\.env$|google-services.json|keystore'   → empty
git grep -E 'eyJ[A-Za-z0-9_-]{20,}|AIza[0-9A-Za-z_-]{30,}'                      → empty
.gitignore: local.properties          admin/.gitignore: .env, .env.local
```

Nothing secret is tracked. `.env.example` mentions the string `service_role`
only in a comment warning not to put it there.

### Environment separation, as built

`local.properties` (git-ignored) now carries per-environment keys:

```properties
SUPABASE_URL_DEV=...          SUPABASE_ANON_KEY_DEV=...
SUPABASE_URL_STAGING=...      SUPABASE_ANON_KEY_STAGING=...
SUPABASE_URL_PROD=...         SUPABASE_ANON_KEY_PROD=...
MAPS_API_KEY_DEV=...          MAPS_API_KEY_STAGING=...   MAPS_API_KEY_PROD=...
```

Unsuffixed keys still work as a fallback for a single-project setup. Six build
variants now exist:

```
assembleCustomerDebug   assembleCustomerStaging   assembleCustomerRelease
assembleVendorDebug     assembleVendorStaging     assembleVendorRelease
```

Debug carries `.dev`, staging `.staging` in the applicationId, so a development
build cannot reach production data and all three can sit on one handset.

---

## Phase 2 — database foundation

Six migration files, applied in order. Idempotent; re-running is safe.

| File | Contents |
|---|---|
| `01_schema.sql` | 18 core tables, indexes, triggers, `vendors_within_km()` |
| `02_rls.sql` | RLS for those tables, both storage buckets |
| `03_seed.sql` | Platform settings and demo rows (optional, dev only) |
| `04_taxonomy_and_foundation.sql` | roles, categories, types, requirements, businesses, documents, verification records, images, reward ledger, communities, messages, support, assistance, notifications, terms, analytics, orders |
| `05_taxonomy_seed.sql` | 10 categories, 40 business types, requirements |
| `06_rls_foundation.sql` | RLS for everything in 04 |

Every table the brief listed now exists. Two decisions worth stating:

**The reward balance is not a column.** `reward_transactions` is an append-only
ledger and a balance is `sum(credits) - sum(debits)` over `status = 'credited'`,
computed on read. A stored balance is a number that can drift from its own
history.

**`orders` exists but is empty.** It gives the vendor dashboard somewhere
truthful to read from the day payments are enabled, and until then the UI says
so rather than inventing figures.

---

## Phase 3/4 — authentication and role security

**The hole this closes.** The anon key ships inside the APK; anyone can extract
it. If the app inserted its own `users` row, it could post `role = 'admin'` and
RLS would honour that forever after.

Two independent defences:

1. **`auth-bootstrap` edge function** — the only path that creates an OORUVA
   user. It verifies the Firebase ID token against Google's certificates, checks
   issuer, audience and expiry, and accepts a role only from
   `['customer','vendor']`. `admin` is not in the list. An existing user keeps
   the role on file, so signing into the vendor app cannot promote a customer.
2. **`guard_role_assignment()` trigger** — refuses any insert or update of
   `users.role` to a non-self-assignable role unless the connection is the
   service role. Belt and braces: even if the function were bypassed, Postgres
   refuses.

Negative cases the RLS is written against:

| Case | Mechanism |
|---|---|
| Customer reads another vendor's draft business | `biz_read` requires `status='verified'` or ownership |
| Customer reads vendor documents | `doc_owner` requires `owns_business()` or admin |
| Vendor reads another vendor's business | same ownership predicate |
| Vendor marks itself verified | `ver_admin_write` is admin-only |
| Any client writes the reward ledger | no client INSERT policy exists at all |
| Any client escalates to admin | trigger plus function allowlist |
| Non-member reads a community message | `msg_member_read` requires membership |

**None of this is verified.** Policies are unexecuted SQL until a project exists.

---

## Phase 5 — category taxonomy

Replaced the hardcoded, food-shaped list with data:

```
business_categories  →  business_types  →  business_requirements
```

10 categories, 40 types, spanning food, grocery, electronics, textile, hardware,
beauty, stationery, automotive, services. Adding one is an INSERT, not a release.

**On legal applicability.** Requirements seed as `requires_review`, not
`required`. OORUVA does not assert which registrations a business legally needs
until a qualified person records a `basis_note`. The only assertions made are
negative and safe — a mobile repair shop does not fall under a food regulator,
so FSSAI is `not_applicable` there. Udyam and GST depend on turnover and
structure rather than category, so every type carries them as `requires_review`.

---

## Phase 7 — fabricated data

Swept the repository against the listed values.

**Removed:** `₹2,140` revenue, `9` customers, `12` check-ins, `₹14,980`, `₹178`
average ticket, `+22% wk`, `48` reviews, `4.5` rating, `VERIFIED`, "Approved 14
March 2026", `2,450` points, `₹245 in vouchers`, `550 points to Gold tier`, the
four-row reward ledger, profile counts `15 / 8 / 23`, and the sample business
"Chai Wali · Main Street · +91 98765 43210".

**Kept, deliberately:** the phone-field *placeholder* `98765 43210` (an input
hint, not data); `points_per_review = 10` and `points_to_rupee = 0.20` in
`platform_settings` (configuration a human sets, not invented analytics); and
`getMockVendors()`, which now lives in `data/mock/` behind
`DataResult.fromMock` so the UI can label it.

**Admin demo data** stays gated behind `isConfigured` and shows a banner reading
"Demo data" with approvals disabled.

---

## Phase 8 — data states

`DataResult` distinguishes `Loading`, `Success(fromMock)` and `Failure`.
`PendingCapability` is the shared empty state for a metric with no truthful
source. Rewards reads "No rewards earned yet"; vendor analytics reads "Sales,
orders and customer counts appear here once transactions are enabled".

---

## What is blocked, and on what

| Blocked | Requires |
|---|---|
| Running any migration | A Supabase project |
| Testing RLS or the role trigger | The same |
| Phone OTP end to end | A Firebase project + `google-services.json` + debug SHA-1 |
| `auth-bootstrap` deployment | Supabase CLI login + `FIREBASE_PROJECT_ID` secret |
| Real map | A restricted Maps API key |
| Automated tests (Phase 11) | A database to run them against |
| FSSAI verification | Determining whether an authorised API exists. Until then the architecture is manual review, and it will stay that way rather than scraping a government portal |

## Exactly what is still required from you

1. **Supabase project** → `SUPABASE_URL_DEV`, `SUPABASE_ANON_KEY_DEV` in
   `local.properties`; same pair in `admin/.env` as `VITE_*`. Run migrations
   01 → 06 in order.
2. **Firebase project** → Phone auth enabled, debug SHA-1 registered, both
   package names added (`com.ooruva.app.customer`, `com.ooruva.app.vendor`, plus
   the `.dev` suffixed variants), `google-services.json` into `app/`.
3. **Maps key** → `MAPS_API_KEY_DEV`, restricted to those package names.
4. **Supabase CLI** → `supabase functions deploy auth-bootstrap` and
   `supabase secrets set FIREBASE_PROJECT_ID=...`

Do not send any of these through chat. They belong in `local.properties`,
`admin/.env` and the Supabase secrets store, all of which are git-ignored.

## Not verified

Everything in Phases 2 through 5 is written and reviewed but **has never
executed**. No migration has run, no policy has been enforced, no token has been
verified, no test exists. The next phase begins by running the migrations
against a real project and testing the negative cases in Phase 4 — in that
order.
