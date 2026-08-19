// OORUVA — rewards
//
// The only path that writes the reward ledger. It runs with the service role,
// which bypasses RLS, so every check RLS would normally make has to be made
// here explicitly — and the ones that matter most are also enforced by the
// database itself (migration 10), because a bug in this file has no policy
// standing behind it.
//
// Two actions:
//   award   — pay a customer for something they actually did
//   redeem  — spend points on an offer
//
// WHY A CLIENT CANNOT DO THIS DIRECTLY
// reward_transactions has no client INSERT policy. If it did, the loyalty
// scheme would be farmable from a decompiled APK: anyone could post themselves
// a hundred thousand points. So the client asks for an award and this function
// decides, after checking that the action being claimed is real and belongs to
// the caller.
//
// Deploy:  supabase functions deploy rewards
// Secrets: SUPABASE_JWT_SECRET (shared with auth-bootstrap)
//          SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY are injected by the platform.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { verify } from "https://deno.land/x/djwt@v3.0.2/mod.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const JWT_SECRET = Deno.env.get("SUPABASE_JWT_SECRET")!;

const db = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
  auth: { persistSession: false },
});

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS, "content-type": "application/json" },
  });

let signingKey: CryptoKey | null = null;
async function key(): Promise<CryptoKey> {
  if (signingKey) return signingKey;
  signingKey = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(JWT_SECRET),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"],
  );
  return signingKey;
}

/** The OORUVA user id from the caller's session, or null. */
async function callerId(req: Request): Promise<string | null> {
  const header = req.headers.get("Authorization") ?? "";
  if (!header.startsWith("Bearer ")) return null;
  try {
    const payload = await verify(header.slice(7), await key()) as Record<string, unknown>;
    return (payload.sub as string) ?? null;
  } catch {
    return null;
  }
}

/**
 * Confirms the thing being claimed actually happened, and belongs to this
 * customer.
 *
 * This is the whole anti-fraud story. Without it, a client could call
 * award("review") in a loop with invented reference ids and be paid every time
 * — the ledger would be server-written and still worthless.
 */
async function verifyActivity(
  activityType: string,
  referenceId: string | null,
  customerId: string,
): Promise<{ ok: true } | { ok: false; reason: string }> {
  // Each activity names the table that proves it and the column that ties it
  // to a person. An unknown activity is refused rather than defaulted.
  const sources: Record<string, { table: string; owner: string }> = {
    review: { table: "reviews", owner: "customer_id" },
    post: { table: "posts", owner: "customer_id" },
    check_in: { table: "check_ins", owner: "customer_id" },
    photo: { table: "posts", owner: "customer_id" },
  };

  const source = sources[activityType];
  if (!source) return { ok: false, reason: `Unknown activity: ${activityType}` };
  if (!referenceId) return { ok: false, reason: "This activity needs a reference_id" };

  const { data, error } = await db
    .from(source.table)
    .select(`id, ${source.owner}`)
    .eq("id", referenceId)
    .maybeSingle();

  if (error) return { ok: false, reason: "Could not verify that activity" };
  if (!data) return { ok: false, reason: "That activity does not exist" };

  // The check that matters: you cannot be paid for someone else's review.
  if ((data as Record<string, unknown>)[source.owner] !== customerId) {
    return { ok: false, reason: "That activity belongs to someone else" };
  }

  return { ok: true };
}

async function award(customerId: string, body: Record<string, unknown>) {
  const activityType = String(body.activity_type ?? "");
  const referenceId = body.reference_id ? String(body.reference_id) : null;

  const { data: rule } = await db
    .from("reward_rules")
    .select("activity_type, points, active, daily_cap")
    .eq("activity_type", activityType)
    .maybeSingle();

  if (!rule) return json({ error: "No reward rule for that activity" }, 400);
  if (!rule.active) return json({ error: "That reward is not currently active" }, 400);

  const check = await verifyActivity(activityType, referenceId, customerId);
  if (!check.ok) return json({ error: check.reason }, 400);

  if (rule.daily_cap != null) {
    const { data: earned } = await db.rpc("rewards_earned_today", {
      target: customerId,
      activity: activityType,
    });
    if ((earned ?? 0) >= rule.daily_cap) {
      return json({
        error: "daily_cap_reached",
        message: `You have reached today's limit for this activity. It resets tomorrow.`,
      }, 429);
    }
  }

  const { data: created, error } = await db
    .from("reward_transactions")
    .insert({
      customer_id: customerId,
      direction: "credit",
      points: rule.points,
      activity_type: activityType,
      reference_id: referenceId,
      status: "credited",
    })
    .select("id, points")
    .single();

  if (error) {
    // 23505 is the partial unique index from migration 10: this action has
    // already been paid for. A retry landing here is success from the caller's
    // point of view, not a failure -- the outcome they wanted is already true.
    if (error.code === "23505") {
      return json({ awarded: false, reason: "already_awarded" }, 200);
    }
    console.error("award failed", error);
    return json({ error: "Could not award points" }, 500);
  }

  const { data: balance } = await db.rpc("reward_balance", { target: customerId });

  return json({
    awarded: true,
    points: created.points,
    transaction_id: created.id,
    balance: balance ?? null,
  });
}

async function redeem(customerId: string, body: Record<string, unknown>) {
  const offerId = body.offer_id ? String(body.offer_id) : null;
  if (!offerId) return json({ error: "offer_id is required" }, 400);

  const { data: offer } = await db
    .from("offers")
    .select("id, title, points_required, validity_date, vendor_id")
    .eq("id", offerId)
    .maybeSingle();

  if (!offer) return json({ error: "That offer does not exist" }, 404);

  if (offer.validity_date && new Date(offer.validity_date) < new Date()) {
    return json({ error: "That offer has expired" }, 400);
  }

  const cost = offer.points_required ?? 0;

  // The balance is checked here for a decent error message, and again by the
  // trigger in migration 10 for correctness. Only the trigger is safe against
  // two redemptions racing each other.
  const { data: balance } = await db.rpc("reward_balance", { target: customerId });
  if ((balance ?? 0) < cost) {
    return json({
      error: "insufficient_points",
      message: `This offer costs ${cost} points and you have ${balance ?? 0}.`,
      balance: balance ?? 0,
      required: cost,
    }, 400);
  }

  const { data: debit, error: debitError } = await db
    .from("reward_transactions")
    .insert({
      customer_id: customerId,
      direction: "debit",
      points: cost,
      activity_type: "redeem",
      reference_id: offerId,
      status: "credited",
      note: `Redeemed: ${offer.title}`,
    })
    .select("id")
    .single();

  if (debitError) {
    // The balance guard fires as a check_violation when a concurrent
    // redemption got there first.
    if (debitError.code === "23514") {
      return json({ error: "insufficient_points", message: "Your balance changed. Try again." }, 409);
    }
    console.error("redeem debit failed", debitError);
    return json({ error: "Could not redeem that offer" }, 500);
  }

  const { error: redemptionError } = await db.from("offer_redemptions").insert({
    offer_id: offerId,
    customer_id: customerId,
    points_spent: cost,
    reward_transaction_id: debit.id,
  });

  if (redemptionError) {
    // The points were taken but the redemption did not record. Reverse rather
    // than leave the customer out of pocket -- 'reversed' is excluded from the
    // balance, so this hands the points straight back while leaving both rows
    // visible for anyone auditing what happened.
    await db
      .from("reward_transactions")
      .update({ status: "reversed", note: "Reversed: redemption could not be recorded" })
      .eq("id", debit.id);

    const reason = redemptionError.code === "23514"
      ? "This offer has reached its usage limit."
      : "Could not complete that redemption. Your points have not been taken.";
    return json({ error: "redemption_failed", message: reason }, 409);
  }

  const { data: newBalance } = await db.rpc("reward_balance", { target: customerId });

  return json({
    redeemed: true,
    offer: offer.title,
    points_spent: cost,
    balance: newBalance ?? null,
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: CORS });
  if (req.method !== "POST") return json({ error: "POST only" }, 405);

  const customerId = await callerId(req);
  if (!customerId) return json({ error: "Not signed in" }, 401);

  // A suspended account keeps neither earning nor spending.
  const { data: user } = await db
    .from("users")
    .select("id, role, suspended")
    .eq("id", customerId)
    .maybeSingle();

  if (!user) return json({ error: "Not signed in" }, 401);
  if (user.suspended) return json({ error: "Account suspended" }, 403);
  if (user.role !== "customer") {
    return json({ error: "Only customers earn and spend points" }, 403);
  }

  const body = await req.json().catch(() => ({})) as Record<string, unknown>;

  switch (body.action) {
    case "award":
      return await award(customerId, body);
    case "redeem":
      return await redeem(customerId, body);
    default:
      return json({ error: "action must be 'award' or 'redeem'" }, 400);
  }
});
