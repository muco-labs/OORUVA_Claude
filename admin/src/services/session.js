import { supabase, isConfigured } from './supabaseClient'

/**
 * Admin authentication.
 *
 * The mobile apps sign in with Firebase phone OTP and have the auth-bootstrap
 * edge function mint a Supabase session. The console uses Supabase's own
 * email + password sign-in instead, for two reasons: a browser has no SIM, and
 * an admin account is created deliberately by an operator rather than by anyone
 * who can receive an SMS.
 *
 * Both paths converge on the same shape — a JWT whose `sub` is the OORUVA
 * users.id — because an admin's users row is created with its id set to the
 * Supabase auth user id. See supabase/08_admin_access.sql. That is what lets
 * is_admin() work for the console with no special-casing anywhere in RLS.
 */

/** Signs in and confirms the account is actually an admin. */
export async function signIn(email, password) {
  if (!isConfigured) throw new Error('Supabase is not configured. See admin/.env.example.')

  const { data, error } = await supabase.auth.signInWithPassword({ email, password })
  if (error) throw new Error(readable(error.message))

  const role = await roleOf(data.user.id)
  if (role !== 'admin') {
    // Signed in as a real user who is not an admin. Sign straight back out:
    // leaving the session alive would put a non-admin in the console shell
    // with every query failing under RLS and no explanation.
    await supabase.auth.signOut()
    throw new Error('That account does not have admin access.')
  }
  return { id: data.user.id, email: data.user.email, role }
}

export async function signOut() {
  if (!isConfigured) return
  await supabase.auth.signOut()
}

/**
 * The current admin, or null.
 *
 * Re-checks the role against the database rather than trusting a stored flag —
 * an admin whose access was revoked should lose the console on their next page
 * load, not at the end of a token's life.
 */
export async function currentAdmin() {
  if (!isConfigured) return null

  const { data } = await supabase.auth.getUser()
  const user = data?.user
  if (!user) return null

  const role = await roleOf(user.id)
  if (role !== 'admin') return null

  return { id: user.id, email: user.email, role }
}

async function roleOf(userId) {
  // users_self_read lets a signed-in caller read exactly their own row, so this
  // needs no elevated key.
  const { data, error } = await supabase
    .from('users')
    .select('role, suspended')
    .eq('id', userId)
    .maybeSingle()

  if (error || !data) return null
  if (data.suspended) return null
  return data.role
}

/** Supabase auth errors are terse; these are the ones an admin actually hits. */
function readable(message) {
  const m = (message || '').toLowerCase()
  if (m.includes('invalid login credentials')) return 'Wrong email or password.'
  if (m.includes('email not confirmed')) return 'That address has not been confirmed yet.'
  if (m.includes('rate limit') || m.includes('too many')) {
    return 'Too many attempts. Wait a minute and try again.'
  }
  return 'Could not sign in. Please try again.'
}
