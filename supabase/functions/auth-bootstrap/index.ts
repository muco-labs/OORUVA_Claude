// OORUVA — auth bootstrap
//
// The only path by which an OORUVA user record is created, and the only place a
// Supabase session is minted. It exists because the anon key is public: if the
// app inserted its own `users` row, anyone could post role = 'admin' and RLS
// would honour it forever after.
//
// Flow:
//   Firebase phone OTP  ->  Firebase ID token  ->  this function
//   verify token against Google's JWKS
//   find or create the OORUVA user
//   mint a Supabase JWT whose `sub` is users.id
//   ->  the app uses that JWT for every PostgREST and Storage call
//
// WHY THE MINTING STEP MATTERS
// Every RLS policy keys off auth.uid(). With only the anon key that is NULL,
// current_user_id() is NULL, and every owner-scoped policy fails closed — the
// app would authenticate successfully and then be unable to read or write
// anything of its own. Verifying the Firebase token is half the job; handing
// back a session Postgres will accept is the other half.
//
// WHY `sub` IS users.id AND NOT THE FIREBASE UID
// Supabase's auth.uid() casts the JWT `sub` to uuid. A Firebase UID is a
// 28-character alphanumeric string and will not cast. The OORUVA user id is
// already a uuid, so it is the correct subject; the Firebase UID is kept in
// users.firebase_uid purely to look the account up here. See migration 07.
//
// Deploy:  supabase functions deploy auth-bootstrap
// Secrets: supabase secrets set FIREBASE_PROJECT_ID=... OORUVA_JWT_SECRET=...
//          SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are injected by the
//          platform. Never commit any of them.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { create, verify } from "https://deno.land/x/djwt@v3.0.2/mod.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const FIREBASE_PROJECT_ID = Deno.env.get("FIREBASE_PROJECT_ID")!;
// The project's JWT secret, from Settings > API > JWT Settings. Without it the
// function can verify a caller but cannot issue a session, which is the state
// this file was in before.
//
// The name deliberately does not begin with SUPABASE_. That prefix is reserved
// by the platform: `supabase secrets set` refuses it outright, and the runtime
// injects only its own SUPABASE_* variables, of which the JWT secret is not
// one. Named SUPABASE_JWT_SECRET this read returns undefined in production and
// every minted session is signed with the string "undefined".
const JWT_SECRET = Deno.env.get("OORUVA_JWT_SECRET")!;

/** Roles a client may ever request. 'admin' is deliberately absent. */
const SELF_ASSIGNABLE_ROLES = ["customer", "vendor"] as const;
type SelfRole = (typeof SELF_ASSIGNABLE_ROLES)[number];

/** How long a minted session lasts before the app must bootstrap again. */
const SESSION_TTL_SECONDS = 60 * 60;

// JWK, not the x509 endpoint. Google publishes the same keys in both formats,
// but WebCrypto cannot import an X.509 certificate as "spki" — a certificate
// wraps the SubjectPublicKeyInfo rather than being one, so that import throws.
// The JWK form imports directly.
const GOOGLE_JWKS =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

type Jwk = { kid: string; [k: string]: unknown };
let jwksCache: { at: number; keys: Jwk[] } | null = null;

async function firebaseKeys(): Promise<Jwk[]> {
  // Google rotates these; an hour of caching is well inside their max-age.
  if (jwksCache && Date.now() - jwksCache.at < 3_600_000) return jwksCache.keys;
  const res = await fetch(GOOGLE_JWKS);
  if (!res.ok) throw new Error("Could not fetch Firebase signing keys");
  const body = await res.json();
  const keys: Jwk[] = body.keys ?? [];
  jwksCache = { at: Date.now(), keys };
  return keys;
}

/**
 * Verifies a Firebase ID token: signature against Google's published keys, then
 * issuer, audience and expiry. Returns the Firebase UID and phone number.
 */
async function verifyFirebaseToken(idToken: string) {
  const [headerB64] = idToken.split(".");
  if (!headerB64) throw new Error("Malformed token");
  const header = JSON.parse(
    atob(headerB64.replace(/-/g, "+").replace(/_/g, "/")),
  );

  const jwk = (await firebaseKeys()).find((k) => k.kid === header.kid);
  if (!jwk) throw new Error("Unknown token key id");

  const key = await crypto.subtle.importKey(
    "jwk",
    jwk as JsonWebKey,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"],
  );

  // djwt checks the signature and the exp/nbf claims; issuer and audience are
  // ours to check, and skipping them would accept a token minted for a
  // different Firebase project.
  const payload = await verify(idToken, key) as Record<string, unknown>;

  if (payload.iss !== `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`) {
    throw new Error("Bad issuer");
  }
  if (payload.aud !== FIREBASE_PROJECT_ID) throw new Error("Bad audience");
  if (!payload.sub) throw new Error("Token has no subject");

  return {
    uid: payload.sub as string,
    phone: (payload.phone_number as string | undefined) ?? null,
  };
}

let signingKey: CryptoKey | null = null;
async function supabaseSigningKey(): Promise<CryptoKey> {
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

/**
 * Mints a session PostgREST will accept. The claims are the minimum Supabase
 * requires: `sub` becomes auth.uid(), and `role` selects the Postgres role the
 * request runs as — 'authenticated', never anything higher.
 */
async function mintSession(userId: string, role: string) {
  const now = Math.floor(Date.now() / 1000);
  const token = await create(
    { alg: "HS256", typ: "JWT" },
    {
      sub: userId,
      aud: "authenticated",
      role: "authenticated",
      iat: now,
      exp: now + SESSION_TTL_SECONDS,
      // Informational only. Authorisation is decided by users.role in the
      // database, never by a claim the client could learn to forge.
      ooruva_role: role,
    },
    await supabaseSigningKey(),
  );
  return { token, expires_in: SESSION_TTL_SECONDS };
}

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

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: CORS });
  if (req.method !== "POST") return json({ error: "POST only" }, 405);

  try {
    const auth = req.headers.get("Authorization") ?? "";
    const idToken = auth.startsWith("Bearer ") ? auth.slice(7) : null;
    if (!idToken) return json({ error: "Missing bearer token" }, 401);

    const { role: requestedRole } = await req.json().catch(() => ({
      role: null,
    }));
    if (!SELF_ASSIGNABLE_ROLES.includes(requestedRole as SelfRole)) {
      // Covers both a missing role and any attempt to self-assign 'admin'.
      return json({
        error: "Role must be one of: " + SELF_ASSIGNABLE_ROLES.join(", "),
      }, 400);
    }

    const { uid, phone } = await verifyFirebaseToken(idToken);
    if (!phone) return json({ error: "Token carries no phone number" }, 400);

    const db = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
      auth: { persistSession: false },
    });

    const { data: existing } = await db
      .from("users")
      .select("id, phone, role, suspended")
      .eq("firebase_uid", uid)
      .maybeSingle();

    if (existing) {
      if (existing.suspended) return json({ error: "Account suspended" }, 403);

      // The role on file wins: a customer cannot become a vendor by signing
      // into the vendor app. Refuse rather than mint a session for the wrong
      // app, so the caller can say something useful instead of showing an
      // empty dashboard the person has no rights to.
      if (existing.role !== requestedRole) {
        return json({
          error: "role_mismatch",
          registered_role: existing.role,
          message:
            `This number is already registered as a ${existing.role}. ` +
            `Use the OORUVA ${existing.role} app to sign in.`,
        }, 409);
      }

      const session = await mintSession(existing.id, existing.role);
      return json({
        user_id: existing.id,
        role: existing.role,
        phone: existing.phone,
        created: false,
        access_token: session.token,
        expires_in: session.expires_in,
      });
    }

    const { data: created, error } = await db
      .from("users")
      .insert({ phone, role: requestedRole, firebase_uid: uid })
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

    const session = await mintSession(created.id, created.role);
    return json({
      user_id: created.id,
      role: created.role,
      phone: created.phone,
      created: true,
      access_token: session.token,
      expires_in: session.expires_in,
    });
  } catch (e) {
    // Deliberately opaque to the caller: distinguishing "bad signature" from
    // "unknown user" here would turn this into a phone-number oracle. The
    // detail goes to the function log instead.
    console.error("auth-bootstrap failed", e);
    return json({ error: "Authentication failed" }, 401);
  }
});
