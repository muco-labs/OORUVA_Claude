# Admin guide

You are the gate. A verified badge on OORUVA means a human — you — looked at the
paperwork and believed it. Everything below is that job.

## Getting in

```bash
cd admin && npm install && npm run dev
```

Open <http://localhost:5173>. If a gold banner says "Demo data", `admin/.env` is
missing its Supabase keys — see `supabase/SETUP.md`. In demo mode you can look
around but not approve anything.

## Daily loop: the verification queue

**Verification** in the sidebar is where new vendors wait. The published promise
is 48 hours, so clear it once a day.

For each vendor:

1. **Read the details.** Business name, owner, address, hours, coordinates.
   A stall claiming to be in another city than its coordinates is worth a query.
2. **Open the FSSAI certificate.** It renders inline — PDF or image. The link is
   signed and expires in five minutes, so refresh the page if you leave it open.
3. **Decide.**
   - **Approve** — the vendor becomes visible to every customer immediately.
   - **Request changes** — they stay hidden, and see your note. Use this when
     something is fixable: a blurry certificate, a missing address.
   - **Reject** — a note is mandatory, because the vendor reads it. Say what was
     wrong in a sentence a stall owner would understand.

Every decision writes to `audit_log` with your note attached.

### FSSAI states you will see

| State | Meaning | What to do |
|---|---|---|
| `verified` | Number and certificate check out | Approve if the rest is fine |
| `pending` | Submitted, not yet reviewed | This is your job |
| `needs_assistance` | Vendor said they have no certificate and want help | Approve the listing if the category does not require one; otherwise contact them on WhatsApp about the ₹750 assisted registration |
| `rejected` | Previously failed | Look at the earlier note before re-deciding |

A stall selling packaged goods may legitimately have no FSSAI. A stall cooking
food should. Use judgement, and record it in the notes.

## Vendors

The full directory. Filter by status, search by name or category, export CSV.
**Revoke** moves a verified vendor back to `needs_changes` — use it when a
complaint checks out, and always give a reason.

## Users

Search by phone, filter by role. **Suspend** blocks a user without deleting their
history; **Restore** undoes it. Deletion is deliberately not exposed here — a
delete cascades through reviews, posts and rewards and cannot be undone.

## Moderation

Flagged reviews, posts and comments. **Keep** clears the flag and puts the
content back; **Delete** removes it. Bias toward keeping — a bad review is not a
policy violation, and a vendor complaining about an honest one is not grounds.

## Rewards

Totals by state, editable earning rates, and the full ledger with CSV export.
Rates take effect immediately for new activity.

Points can only be written server-side. If you see a customer's balance climbing
without matching activity, that is a bug or an exploit — check the ledger before
adjusting anything.

## Settings

Everything in `platform_settings`: support WhatsApp number, FSSAI assist fee,
review SLA, points rates. Changes save when you click out of the field.

## Before this goes public

The console has **no login of its own** today. Database writes are gated by
`is_admin()`, but the pages themselves are open to anyone with the URL. Add an
admin sign-in before deploying it anywhere public.
