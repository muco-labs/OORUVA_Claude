// OORUVA — auth bootstrap
//
// The only path by which an OORUVA user record is created. It exists because the
// anon key is public: if the app inserted its own `users` row, anyone could post
// role = 'admin' and RLS would happily honour it forever after.
//
// Flow:
//   Firebase phone OTP  ->  Firebase ID token  ->  this function
//   verify token with Firebase  ->  upsert users row  ->  return OORUVA identity
//
// Deploy:  supabase functions deploy auth-bootstrap
// Secrets: supabase secrets set FIREBASE_PROJECT_ID=... SERVICE_ROLE_KEY=...
//          (set through the CLI or dashboard; never committed)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { create, verify } from "https://deno.land/x/djwt@v3.0.2/mod.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const FIREBASE_PROJECT_ID = Deno.env.get("FIREBASE_PROJECT_ID")!;

/** Roles a client may ever request. 'admin' is deliberately absent. */
const SELF_ASSIGNABLE_ROLES = ["customer", "vendor"] as const;
type SelfRole = (typeof SELF_ASSIGNABLE_ROLES)[number];

const GOOGLE_CERTS =
  "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

let certCache: { at: number; certs: Record<string, string> } | null = null;

async function firebaseCerts(): Promise<Record<string, string>> {
  // Google rotates these; an hour of caching is well inside their max-age.
  if (certCache && Date.now() - certCache.at < 3_600_000) return certCache.certs;
  const res = await fetch(GOOGLE_CERTS);
  if (!res.ok) throw new Error("Could not fetch Firebase certificates");
  const certs = await res.json();
  certCache = { at: Date.now(), certs };
  return certs;
}

/**
 * Verifies a Firebase ID token: signature against Google's public certs, then
 * issuer, audience and expiry. Returns the Firebase UID and phone number.
 */
async function verifyFirebaseToken(idToken: string) {
  const [headerB64] = idToken.split(".");
  const header = JSON.parse(atob(headerB64.replace(/-/g, "+").replace(/_/g, "/")));
  const certs = await firebaseCerts();
  const pem = certs[header.kid];
  if (!pem) throw new Error("Unknown token key id");

  const key = await importX509(pem);
  const payload = await verify(idToken, key) as Record<string, unknown>;

  const iss = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
  if (payload.iss !== iss) throw new Error("Bad issuer");
  if (payload.aud !== FIREBASE_PROJECT_ID) throw new Error("Bad audience");
  if ((payload.exp as number) * 1000 < Date.now()) throw new Error("Token expired");
  if (!payload.sub) throw new Error("Token has no subject");

  return {
    uid: payload.sub as string,
    phone: (payload.phone_number as string | undefined) ?? null,
  };
}

async function importX509(pem: string): Promise<CryptoKey> {
  const body = pem
    .replace(/-----BEGIN CERTIFICATE-----/, "")
    .replace(/-----END CERTIFICATE-----/, "")
    .replace(/\s+/g, "");
  const der = Uint8Array.from(atob(body), (c) => c.charCodeAt(0));
  // Deno exposes the certificate's SPKI directly for RS256 verification.
  return await crypto.subtle.importKey(
    "spki",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"],
  );
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "POST only" }), { status: 405 });
  }

  try {
    const auth = req.headers.get("Authorization") ?? "";
    const idToken = auth.startsWith("Bearer ") ? auth.slice(7) : null;
    if (!idToken) {
      return new Response(JSON.stringify({ error: "Missing bearer token" }), { status: 401 });
    }

    const { role: requestedRole } = await req.json().catch(() => ({ role: null }));
    if (!SELF_ASSIGNABLE_ROLES.includes(requestedRole as SelfRole)) {
      // Covers both a missing role and any attempt to self-assign 'admin'.
      return new Response(
        JSON.stringify({ error: "Role must be one of: " + SELF_ASSIGNABLE_ROLES.join(", ") }),
        { status: 400 },
      );
    }

    const { uid, phone } = await verifyFirebaseToken(idToken);
    if (!phone) {
      return new Response(JSON.stringify({ error: "Token carries no phone number" }), { status: 400 });
    }

    const db = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
      auth: { persistSession: false },
    });

    // Existing user: return as-is. The role on file wins — a customer cannot
    // become a vendor by signing into the vendor app, and vice versa.
    const { data: existing } = await db
      .from("users")
      .select("id, phone, role, suspended")
      .eq("auth_uid", uid)
      .maybeSingle();

    if (existing) {
      if (existing.suspended) {
        return new Response(JSON.stringify({ error: "Account suspended" }), { status: 403 });
      }
      return Response.json({
        user_id: existing.id,
        role: existing.role,
        phone: existing.phone,
        created: false,
        role_mismatch: existing.role !== requestedRole,
      });
    }

    const { data: created, error } = await db
      .from("users")
      .insert({ phone, role: requestedRole, auth_uid: uid })
      .select("id, phone, role")
      .single();

    if (error) throw error;

    if (requestedRole === "customer") {
      await db.from("customer_profiles").insert({ customer_id: created.id });
    }

    await db.from("audit_log").insert({
      actor_id: created.id,
      action: "user_created",
      entity: "users",
      entity_id: created.id,
      notes: `role=${requestedRole}`,
    });

    return Response.json({
      user_id: created.id,
      role: created.role,
      phone: created.phone,
      created: true,
      role_mismatch: false,
    });
  } catch (e) {
    console.error("auth-bootstrap failed", e);
    return new Response(JSON.stringify({ error: "Authentication failed" }), { status: 401 });
  }
});
