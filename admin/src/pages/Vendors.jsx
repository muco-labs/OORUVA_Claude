import React, { useEffect, useMemo, useState } from 'react'
import { fetchBusinesses, decideVerification, exportCsv } from '../services/dataService'
import { StatusPill } from './Dashboard'

// Mirrors the businesses.status check constraint in migration 04.
const statuses = ['', 'draft', 'submitted', 'verified', 'needs_changes', 'rejected', 'suspended']

export default function Vendors() {
  const [rows, setRows] = useState([])
  const [status, setStatus] = useState('')
  const [q, setQ] = useState('')
  const [error, setError] = useState(null)

  const load = async () => {
    try {
      setRows(await fetchBusinesses(status || undefined))
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => { load() }, [status])

  const filtered = useMemo(
    () => rows.filter((b) =>
      !q ||
      b.name?.toLowerCase().includes(q.toLowerCase()) ||
      b.business_types?.name?.toLowerCase().includes(q.toLowerCase()) ||
      b.district?.toLowerCase().includes(q.toLowerCase())
    ),
    [rows, q]
  )

  const revoke = async (b) => {
    // A reason is mandatory: the vendor sees it, and "your listing came down"
    // with no explanation is how a small business decides the platform is
    // arbitrary and leaves.
    const reason = window.prompt(`Revoke verification for ${b.name}? Reason:`)
    if (!reason?.trim()) return
    try {
      await decideVerification(b.id, 'needs_changes', reason.trim())
      await load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div>
      <div className="eyebrow">Directory</div>
      <h1 className="text-4xl mt-2 mb-8">Businesses</h1>

      {error && <p className="mb-4 text-brick text-sm">{error}</p>}

      <div className="flex flex-wrap gap-3 mb-6">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search name, type or district"
          className="border border-outline rounded-[10px] px-3 py-2 text-sm w-64 focus:outline-none focus:border-gold"
        />
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="border border-outline rounded-[10px] px-3 py-2 text-sm"
        >
          {statuses.map((s) => (
            <option key={s} value={s}>{s === '' ? 'All statuses' : s}</option>
          ))}
        </select>
        <button className="btn-ghost" onClick={() => exportCsv(filtered, 'ooruva-businesses.csv')}>
          Export CSV
        </button>
      </div>

      <div className="card overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-ivory">
            <tr className="text-left">
              <th className="p-3 eyebrow">Business</th>
              <th className="p-3 eyebrow">Type</th>
              <th className="p-3 eyebrow">District</th>
              <th className="p-3 eyebrow">Phone</th>
              <th className="p-3 eyebrow">Status</th>
              <th className="p-3 eyebrow">Created</th>
              <th className="p-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline">
            {filtered.map((b) => (
              <tr key={b.id}>
                <td className="p-3 font-semibold">{b.name}</td>
                <td className="p-3">{b.business_types?.name ?? '—'}</td>
                <td className="p-3">{b.district ?? '—'}</td>
                <td className="p-3">{b.phone ?? '—'}</td>
                <td className="p-3"><StatusPill status={b.status} /></td>
                <td className="p-3 text-warm">{new Date(b.created_at).toLocaleDateString()}</td>
                <td className="p-3 text-right">
                  {b.status === 'verified' && (
                    <button className="text-brick text-xs hover:underline" onClick={() => revoke(b)}>
                      Revoke
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr><td colSpan={7} className="p-6 text-center text-warm">No businesses match.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
