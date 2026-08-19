import { supabase } from './supabaseClient'

/**
 * Admin data access, on the model migration 04 introduced and 07 completed:
 * businesses / business_documents / verification_records / reward_transactions.
 *
 * The older vendor_profiles + fssai_records + rewards tables are no longer read
 * here. They still exist so nothing breaks mid-migration, but the console is the
 * surface where two competing models would do the most damage — an admin
 * approving a row in one table while the customer app reads the other is a
 * silent, invisible failure.
 *
 * The demo-data fallback is gone. It existed so the dashboard rendered before
 * Supabase was configured, but the console now requires a real admin sign-in,
 * so anything reaching these functions already has a live connection. Keeping
 * plausible fake rows around a screen where people click "Approve" is a habit
 * worth not having.
 */

const must = ({ data, error }) => {
  if (error) throw new Error(error.message)
  return data ?? []
}

/** Who is acting. Recorded on every audited action. */
async function actorId() {
  const { data } = await supabase.auth.getUser()
  return data?.user?.id ?? null
}

async function audit(action, entity, entityId, notes) {
  await supabase.from('audit_log').insert({
    actor_id: await actorId(),
    action,
    entity,
    entity_id: entityId,
    notes: notes ?? null,
  })
}

// == Dashboard ===============================================================

export async function fetchStats() {
  const count = async (table, filter) => {
    let q = supabase.from(table).select('*', { count: 'exact', head: true })
    if (filter) q = q.eq(filter[0], filter[1])
    const { count: c, error } = await q
    if (error) throw new Error(error.message)
    return c ?? 0
  }

  return {
    users: await count('users'),
    businesses: await count('businesses'),
    // 'submitted' is the queue. 'draft' is a vendor still typing and must not
    // be counted as work waiting on an admin.
    pending: await count('businesses', ['status', 'submitted']),
    verified: await count('businesses', ['status', 'verified']),
    reviews: await count('reviews'),
  }
}

// == Businesses ==============================================================

const BUSINESS_SELECT = `
  *,
  business_types ( id, name, slug, business_categories ( id, name, slug ) ),
  business_documents ( id, document_type, document_number, storage_path, status, admin_notes )
`

export async function fetchBusinesses(status) {
  let q = supabase
    .from('businesses')
    .select(BUSINESS_SELECT)
    .order('submitted_at', { ascending: true, nullsFirst: false })

  if (status) q = q.eq('status', status)
  return must(await q)
}

/** The verification queue: oldest submission first, so nobody is left behind. */
export async function fetchQueue() {
  return must(
    await supabase
      .from('businesses')
      .select(BUSINESS_SELECT)
      .eq('status', 'submitted')
      .order('submitted_at', { ascending: true })
  )
}

/**
 * Records a verification decision.
 *
 * Three writes, deliberately in this order: the outcome first, so a failure
 * halfway cannot leave a business marked verified with no record of who did it;
 * then the immutable verification_records row; then the audit entry.
 *
 * Postgres has no transaction across separate PostgREST calls. Doing this
 * properly needs an RPC — noted in the report rather than papered over here.
 */
export async function decideVerification(businessId, status, notes) {
  const patch = { status, verification_notes: notes ?? null }
  if (status === 'verified') patch.verified_at = new Date().toISOString()

  const { error } = await supabase.from('businesses').update(patch).eq('id', businessId)
  if (error) throw new Error(error.message)

  await supabase.from('verification_records').insert({
    business_id: businessId,
    // Not 'official_api'. OORUVA has no authorised government verification API,
    // and recording one would be a false provenance claim on a compliance
    // record. A person looked at a document; that is what this says.
    method: 'manual_review',
    outcome: status === 'verified' ? 'passed' : status === 'rejected' ? 'failed' : 'inconclusive',
    checked_field: 'business_name+document_number',
    evidence_note: notes ?? null,
    performed_by: await actorId(),
  })

  await audit(`business_${status}`, 'businesses', businessId, notes)
}

/** Marks one submitted document as checked. */
export async function decideDocument(documentId, status, notes) {
  const { error } = await supabase
    .from('business_documents')
    .update({
      status,
      admin_notes: notes ?? null,
      reviewed_by: await actorId(),
      reviewed_at: new Date().toISOString(),
    })
    .eq('id', documentId)
  if (error) throw new Error(error.message)

  await audit(`document_${status}`, 'business_documents', documentId, notes)
}

export async function fetchCatalogue(businessId) {
  return must(
    await supabase
      .from('products')
      .select('*')
      .eq('business_id', businessId)
      .order('sort_order', { ascending: true })
  )
}

// == Users ===================================================================

export async function fetchUsers() {
  return must(
    await supabase.from('users').select('*').order('created_at', { ascending: false })
  )
}

export async function setUserSuspended(userId, suspended) {
  const { error } = await supabase.from('users').update({ suspended }).eq('id', userId)
  if (error) throw new Error(error.message)

  // Worth auditing in both directions. Suspension now revokes every
  // owner-scoped grant immediately (migration 07), so it is a consequential act.
  await audit(suspended ? 'user_suspended' : 'user_reinstated', 'users', userId, null)
}

// == Moderation ==============================================================

export async function fetchFlagged() {
  const [reviews, posts, comments, messages] = await Promise.all([
    supabase.from('reviews').select('*').eq('flagged', true),
    supabase.from('posts').select('*').eq('flagged', true),
    supabase.from('post_comments').select('*').eq('flagged', true),
    supabase.from('messages').select('*').eq('flagged', true),
  ])

  return [
    ...(reviews.data ?? []).map((r) => ({ ...r, kind: 'review', body: r.text })),
    ...(posts.data ?? []).map((p) => ({ ...p, kind: 'post', body: p.caption })),
    ...(comments.data ?? []).map((c) => ({ ...c, kind: 'comment', body: c.text })),
    ...(messages.data ?? []).map((m) => ({ ...m, kind: 'message', body: m.body })),
  ]
}

const MODERATION_TABLES = {
  review: 'reviews',
  post: 'posts',
  comment: 'post_comments',
  message: 'messages',
}

export async function resolveFlag(kind, id, action) {
  const table = MODERATION_TABLES[kind]
  if (!table) throw new Error(`Unknown content type: ${kind}`)

  if (action === 'delete') {
    const { error } = await supabase.from(table).delete().eq('id', id)
    if (error) throw new Error(error.message)
  } else {
    const { error } = await supabase.from(table).update({ flagged: false }).eq('id', id)
    if (error) throw new Error(error.message)
  }

  await audit(`moderation_${action}`, table, id, null)
}

// == Rewards =================================================================

export async function fetchRewardLedger(limit = 200) {
  return must(
    await supabase
      .from('reward_transactions')
      .select('*')
      .order('created_at', { ascending: false })
      .limit(limit)
  )
}

/** The configurable earning rates. Editing these needs no app release. */
export async function fetchRewardRules() {
  return must(await supabase.from('reward_rules').select('*').order('activity_type'))
}

export async function saveRewardRule(activityType, patch) {
  const { error } = await supabase
    .from('reward_rules')
    .update({ ...patch, updated_by: await actorId(), updated_at: new Date().toISOString() })
    .eq('activity_type', activityType)
  if (error) throw new Error(error.message)

  await audit('reward_rule_changed', 'reward_rules', null, `${activityType}: ${JSON.stringify(patch)}`)
}

// == Taxonomy ================================================================
// Categories and types are data. This is the surface that makes them editable
// without an app release, which is the whole point of the taxonomy tables.

export async function fetchCategories() {
  return must(
    await supabase.from('business_categories').select('*').order('sort_order')
  )
}

export async function fetchTypes() {
  return must(
    await supabase
      .from('business_types')
      .select('*, business_categories ( name, slug )')
      .order('sort_order')
  )
}

export async function saveCategory(id, patch) {
  const { error } = await supabase.from('business_categories').update(patch).eq('id', id)
  if (error) throw new Error(error.message)
  await audit('category_changed', 'business_categories', id, JSON.stringify(patch))
}

// == Platform settings =======================================================

export async function fetchSettings() {
  return must(await supabase.from('platform_settings').select('*').order('key'))
}

export async function saveSetting(key, value) {
  const { error } = await supabase
    .from('platform_settings')
    .upsert({ key, value, updated_at: new Date().toISOString() })
  if (error) throw new Error(error.message)
  await audit('setting_changed', 'platform_settings', null, `${key}=${value}`)
}

// == Audit ===================================================================

export async function fetchAuditLog(limit = 200) {
  return must(
    await supabase
      .from('audit_log')
      .select('*')
      .order('created_at', { ascending: false })
      .limit(limit)
  )
}

// == Storage =================================================================

/**
 * Short-lived signed URL for a private document.
 *
 * Five minutes, and generated per view rather than stored: these are people's
 * licence certificates, and a long-lived URL in a browser history or a support
 * ticket is a leak that outlives the session that created it.
 */
export async function documentUrl(path) {
  if (!path) return null
  const { data, error } = await supabase.storage.from('documents').createSignedUrl(path, 300)
  if (error) return null
  return data?.signedUrl ?? null
}

// == Export ==================================================================

export function exportCsv(rows, filename) {
  if (!rows?.length) return
  const cols = Object.keys(rows[0]).filter((c) => typeof rows[0][c] !== 'object')
  const escape = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const csv = [
    cols.join(','),
    ...rows.map((r) => cols.map((c) => escape(r[c])).join(',')),
  ].join('\n')

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  a.click()
  URL.revokeObjectURL(a.href)
}
