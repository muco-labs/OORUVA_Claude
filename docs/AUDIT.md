# OORUVA — prototype audit (spec §52)

Inspection of what exists today, mapped to Keep / Refactor / Replace / Remove /
Build / Defer. Written before any further coding, per the execution process in
§55.

**Method:** file-level inspection of the repository, route registration in the
navigation graph, and grep for fabricated values. Not from memory.

---

## A. State of each surface

| Surface | State | Evidence |
|---|---|---|
| OORUVA Customer | **Partially working** | 7 screens render, on mock data. No auth, no backend reads. |
| OORUVA Vendors | **Partially working** | 5 screens render. Dashboard was showing invented trading figures. |
| OORUVA Admin | **Working (demo mode)** | 7 React pages, real Supabase calls, degrades to demo data with a visible banner. |
| Shared backend | **Defined, not deployed** | 18 tables + RLS + seed written; no Supabase project exists. |
| Identity | **Missing** | Zero files reference FirebaseAuth. Auth is a mock screen. |
| Storage | **Missing** | Zero files call storage upload. |

## B. Findings, by severity

### 1. Role duplication in one binary — **critical, fixed this pass**

The customer APK registered and could navigate to `VendorHome`,
`VendorBusinessInfo`, `VendorPhotos`, `VendorAnalytics`, `VendorProfile` and
`AdminDashboard`. The customer Profile linked to Vendor Portal and Admin
Dashboard.

Both flavors shared one `NavGraph.kt`, so vendor and admin code was compiled
into the customer app. Hiding it behind a flag would have left the classes in
the binary and the routes reachable by deep link.

**Action taken (Remove + Refactor):** flavor source sets. `src/customer/` and
`src/vendor/` each hold their own navigation graph and screens; `src/main/`
keeps only genuinely shared code (theme, components, data layer, models, the
phone-auth core). The vendor screens are no longer present in the customer
binary at all. `AdminDashboardScreen` and `VendorPortalScreen` were deleted
outright — admin is a web application (§3), so an Android admin screen was
duplication by definition. `RoleSelectionScreen` was deleted because each app
now opens its own front door.

### 2. Fabricated business data — **critical, fixed this pass**

The vendor dashboard displayed `₹2,140` revenue, `9` customers, `12` check-ins;
analytics showed `₹14,980` revenue, `₹178` average ticket, `+22% wk`; the
profile claimed `VERIFIED` and "Approved 14 March 2026".

None of it had a source. A vendor could have made stocking decisions on it.
This is the exact failure §19 and §51 name.

**Action taken (Replace):** every figure without a truthful source is replaced
by a `PendingCapability` empty state — *"Sales, orders and customer counts
appear here once transactions are enabled on OORUVA."* Verification now reads
`AWAITING VERIFICATION` / "Not yet submitted", because no verification pipeline
is connected.

Still to do in the same spirit: the customer Rewards screen shows a hardcoded
`2,450` points, and the customer Profile shows `15 / 8 / 23`. Both need the same
treatment once the reward ledger is live.

### 3. Everything reads mock data — **known, deferred by dependency**

One repository exists (`VendorRepository`) and no screen calls it yet. This is
not fixable without a Supabase project.

### 4. Missing features against the product spec

| Area | State |
|---|---|
| Onboarding, Settings screens | **Missing** |
| Business category / type taxonomy | **Missing** — categories are a hardcoded food-leaning list, violating §9 |
| Vendor registration flow (§24) | **Missing** — no wizard, business info is read-only text |
| FSSAI (§25–26), Udyam (§27), GST (§28) | **Missing entirely** |
| Products, pricing, packages/boxes (§16, §21) | **Missing** |
| Photo upload (§22) | **Missing** — the photo grid is static |
| Review submission, posts, comments | **Missing** |
| Rewards ledger, offers, redemption | **Missing** |
| Community / Club House (§17) | **Missing** — feed is a read-only mock |
| Food Finder intent (§10) | **Partial** — Group Finder does budget maths, no food intent |
| Map (§11) | **Placeholder** — hand-drawn canvas, not the Maps SDK |
| Terms acceptance flow (§39) | **Missing** |
| Analytics events (§43) | **Missing** |

### 5. Architecture decisions taken

**Two apps from one repository, not two repositories.** Product flavors give
two separately installable apps (`com.ooruva.app.customer`,
`com.ooruva.app.vendor`) with their own names, icons and front doors, while the
design system, data layer and models are maintained once. §36 says not to force
a shared package if it would be harmful — the inverse is also true, and
duplicating 30 files into a second repo would guarantee drift. Flavor source
sets give the isolation §4 demands without that cost.

**Admin stays a separate web app.** Already correct.

**Categories must become data, not code.** §9 requires configurable categories
across ~30 business types. The current `listOf("ALL","CHAI","FOOD",…)` in
`HomeScreen` is a food-shaped hardcode. This needs a `business_categories`
table with `business_types` beneath it, seeded and admin-editable — planned, not
yet built.

## C. Blocking dependency

Everything in section 4 writes to or reads from a database, and needs a user
identity to attach rows to. Neither exists yet:

- No Supabase project → schema unrun, RLS unenforced, storage buckets absent
- No Firebase project → no phone auth, no session, no user record to own data
- No Maps key → map stays a placeholder

These require account creation, which is yours to do (`supabase/SETUP.md`).
Building further UI against imagined responses would produce code that has never
executed against a real table — the opposite of production readiness.

## D. Recommended order once the backend is live

1. Firebase phone auth → OORUVA user record → session persistence
2. Category/type taxonomy as data, seeded and admin-managed
3. Vendor registration wizard through to submission (§24), FSSAI last
4. Storage: photo upload with compression and validation; private documents
5. Customer discovery against real vendors; verification gates visibility
6. Reviews and posts; then the reward ledger, written server-side only
7. Offers, then Maps SDK, then community
8. Analytics events last — they describe the rest

Each step ends with the screens moved off mock data and the `fromMock` flag
gone, so nothing claims to be live before it is.
