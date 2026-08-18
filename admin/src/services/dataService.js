import { supabase, isConfigured } from './supabaseClient'

/**
 * Every call returns { data, mock }. `mock: true` means the dashboard is showing
 * demo rows because Supabase is not configured yet — the UI says so out loud
 * rather than passing fiction off as production numbers.
 */

const demo = {
  stats: { users: 4, vendors: 2, pending: 1, reviews: 1 },
  vendors: [
    {
      id: 'demo-1', vendor_id: 'demo-1', business_name: 'Chai Wali',
      business_category: 'Chai', address: 'Main Street, T. Nagar',
      phone: '+919876543211', opening_hours: '06:00-22:00',
      verification_status: 'verified', created_at: '2026-08-01T09:00:00Z',
      fssai_records: [{ fssai_number: '1234-5678-9012', status: 'verified', certificate_url: null }],
    },
    {
      id: 'demo-2', vendor_id: 'demo-2', business_name: 'Street Samosa',
      business_category: 'Food', address: 'Market Road, T. Nagar',
      phone: '+919876543212', opening_hours: '11:00-20:00',
      verification_status: 'pending', created_at: '2026-08-17T14:30:00Z',
      fssai_records: [{ fssai_number: null, status: 'needs_assistance', certificate_url: null }],
    },
  ],
  users: [
    { id: 'u1', phone: '+919876543210', role: 'customer', suspended: false, created_at: '2026-07-14T10:00:00Z' },
    { id: 'u2', phone: '+919876543211', role: 'vendor', suspended: false, created_at: '2026-08-01T09:00:00Z' },
    { id: 'u3', phone: '+919876543212', role: 'vendor', suspended: false, created_at: '2026-08-17T14:30:00Z' },
    { id: 'u4', phone: '+919000000000', role: 'admin', suspended: false, created_at: '2026-07-01T08:00:00Z' },
  ],
  flagged: [],
  rewards: [
    { id: 'r1', customer_id: 'u1', points: 50, activity_type: 'check_in', status: 'credited', created_at: '2026-08-18T19:00:00Z' },
    { id: 'r2', customer_id: 'u1', points: 10, activity_type: 'review', status: 'pending', created_at: '2026-08-18T20:00:00Z' },
  ],
  settings: [
    { key: 'points_per_review', value: '10' },
    { key: 'points_per_photo', value: '5' },
    { key: 'points_per_post', value: '2' },
    { key: 'points_per_checkin', value: '50' },
    { key: 'points_to_rupee', value: '0.20' },
    { key: 'support_whatsapp', value: '+910000000000' },
  ],
}

const wrap = (data) => ({ data, mock: !isConfigured })

export async function fetchStats() {
  if (!isConfigured) return wrap(demo.stats)
  const count = async (table, filter) => {
    let q = supabase.from(table).select('*', { count: 'exact', head: true })
    if (filter) q = q.eq(filter[0], filter[1])
    const { count: c } = await q
    return c ?? 0
  }
  return wrap({
    users: await count('users'),
    vendors: await count('vendor_profiles'),
    pending: await count('verification_queue', ['status', 'pending']),
    reviews: await count('reviews'),
  })
}

export async function fetchVendors(status) {
  if (!isConfigured) {
    const rows = status ? demo.vendors.filter((v) => v.verification_status === status) : demo.vendors
    return wrap(rows)
  }
  let q = supabase.from('vendor_profiles').select('*, fssai_records(*)').order('created_at', { ascending: false })
  if (status) q = q.eq('verification_status', status)
  const { data, error } = await q
  if (error) throw error
  return wrap(data ?? [])
}

export async function fetchUsers() {
  if (!isConfigured) return wrap(demo.users)
  const { data, error } = await supabase.from('users').select('*').order('created_at', { ascending: false })
  if (error) throw error
  return wrap(data ?? [])
}

export async function setVerification(vendorId, status, notes) {
  if (!isConfigured) throw new Error('Connect Supabase to approve or reject vendors.')
  const { error } = await supabase
    .from('vendor_profiles')
    .update({ verification_status: status, verification_notes: notes ?? null })
    .eq('vendor_id', vendorId)
  if (error) throw error

  await supabase
    .from('verification_queue')
    .update({ status: status === 'verified' ? 'approved' : 'rejected', admin_notes: notes ?? null })
    .eq('vendor_id', vendorId)

  await supabase.from('audit_log').insert({
    action: `vendor_${status}`, entity: 'vendor_profiles', entity_id: vendorId, notes: notes ?? null,
  })
}

export async function setUserSuspended(userId, suspended) {
  if (!isConfigured) throw new Error('Connect Supabase to suspend users.')
  const { error } = await supabase.from('users').update({ suspended }).eq('id', userId)
  if (error) throw error
}

export async function fetchFlagged() {
  if (!isConfigured) return wrap(demo.flagged)
  const [reviews, posts, comments] = await Promise.all([
    supabase.from('reviews').select('*').eq('flagged', true),
    supabase.from('posts').select('*').eq('flagged', true),
    supabase.from('post_comments').select('*').eq('flagged', true),
  ])
  return wrap([
    ...(reviews.data ?? []).map((r) => ({ ...r, kind: 'review', body: r.text })),
    ...(posts.data ?? []).map((p) => ({ ...p, kind: 'post', body: p.caption })),
    ...(comments.data ?? []).map((c) => ({ ...c, kind: 'comment', body: c.text })),
  ])
}

export async function resolveFlag(kind, id, action) {
  if (!isConfigured) throw new Error('Connect Supabase to moderate content.')
  const table = kind === 'review' ? 'reviews' : kind === 'post' ? 'posts' : 'post_comments'
  if (action === 'delete') {
    const { error } = await supabase.from(table).delete().eq('id', id)
    if (error) throw error
  } else {
    const { error } = await supabase.from(table).update({ flagged: false }).eq('id', id)
    if (error) throw error
  }
}

export async function fetchRewards() {
  if (!isConfigured) return wrap(demo.rewards)
  const { data, error } = await supabase.from('rewards').select('*').order('created_at', { ascending: false }).limit(200)
  if (error) throw error
  return wrap(data ?? [])
}

export async function fetchSettings() {
  if (!isConfigured) return wrap(demo.settings)
  const { data, error } = await supabase.from('platform_settings').select('*').order('key')
  if (error) throw error
  return wrap(data ?? [])
}

export async function saveSetting(key, value) {
  if (!isConfigured) throw new Error('Connect Supabase to change settings.')
  const { error } = await supabase.from('platform_settings').upsert({ key, value, updated_at: new Date().toISOString() })
  if (error) throw error
}

/** Signed URL for a private FSSAI certificate — expires in 5 minutes. */
export async function certificateUrl(path) {
  if (!isConfigured || !path) return null
  const { data } = await supabase.storage.from('documents').createSignedUrl(path, 300)
  return data?.signedUrl ?? null
}

export function exportCsv(rows, filename) {
  if (!rows?.length) return
  const cols = Object.keys(rows[0])
  const escape = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const csv = [cols.join(','), ...rows.map((r) => cols.map((c) => escape(r[c])).join(','))].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  a.click()
  URL.revokeObjectURL(a.href)
}
