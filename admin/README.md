# OORUVA Admin

React + Vite + Tailwind console for verification, moderation, users and rewards.
Same Quiet Luxury palette as the apps — espresso, ivory, aged gold.

## Run locally

```bash
cd admin
npm install
cp .env.example .env      # then fill in URL + anon key
npm run dev               # http://localhost:5173
```

Without `.env` it still runs, on demo data, with a banner saying so. Approvals
and edits are disabled in that mode rather than silently doing nothing.

## Pages

| Route | What it does |
|---|---|
| `/` | Counts, newest vendors, size of the queue |
| `/verification` | The core loop: read the FSSAI certificate, approve, request changes, or reject with a reason |
| `/vendors` | Full directory, filter by status, revoke verification, CSV export |
| `/users` | Search by phone, filter by role, suspend and restore, CSV export |
| `/moderation` | Flagged reviews, posts and comments — keep or delete |
| `/rewards` | Points totals by state, editable earning rates, ledger, CSV export |
| `/settings` | Platform settings from `platform_settings` |

## Deploy

Vercel and Netlify both need **your** account, so this is your step:

```bash
cd admin
npm run build            # outputs dist/
npx vercel --prod        # or: npx netlify deploy --prod --dir=dist
```

Set `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` in the host's environment
variables panel. They are baked into the bundle at build time, so a rebuild is
needed after changing them.

## Security notes

- The **anon key is public** by design. Everything that protects data is in
  `supabase/02_rls.sql`. If RLS is not enabled, this dashboard — and anyone with
  the key — can read every table.
- The **service_role key never goes in this app.** Anything needing it (awarding
  points, bulk operations) belongs in a Supabase edge function.
- Admin identity is currently whatever Supabase auth session exists, with
  `is_admin()` gating writes at the database level. Add a proper admin login
  before this is exposed publicly — see "Known gaps" in `docs/TECHNICAL.md`.
- FSSAI certificates live in the private `documents` bucket and are fetched
  through five-minute signed URLs, never public links.
