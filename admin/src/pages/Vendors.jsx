import React, { useEffect, useMemo, useState } from 'react'
import { fetchVendors, setVerification, exportCsv } from '../services/dataService'

const statuses = ['', 'pending', 'verified', 'rejected', 'needs_changes']

export default function Vendors() {
  const [rows, setRows] = useState([])
  const [status, setStatus] = useState('')
  const [q, setQ] = useState('')
  const [error, setError] = useState(null)

  const load = async () => {
    try {
      const { data } = await fetchVendors(status || undefined)
      setRows(data)
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => { load() }, [status])

  const filtered = useMemo(
    () => rows.filter((v) =>
      !q || v.business_name?.toLowerCase().includes(q.toLowerCase()) ||
      v.business_category?.toLowerCase().includes(q.toLowerCase())
    ),
    [rows, q]
  )

  const revoke = async (v) => {
    const reason = window.prompt(`Revoke verification for ${v.business_name}? Reason:`)
    if (!reason) return
    try {
      await setVerification(v.vendor_id, 'needs_changes', reason)
      await load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div>
      <div className="eyebrow">Directory</div>
      <h1 className="text-4xl mt-2 mb-8">Vendors</h1>

      {error && <p className="mb-4 text-brick text-sm">{error}</p>}

      <div className="flex flex-wrap gap-3 mb-6">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search name or category"
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
        <button className="btn-ghost" onClick={() => exportCsv(filtered, 'ooruva-vendors.csv')}>
          Export CSV
        </button>
      </div>

      <div className="card overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-ivory">
            <tr className="text-left">
              <th className="p-3 eyebrow">Business</th>
              <th className="p-3 eyebrow">Category</th>
              <th className="p-3 eyebrow">Phone</th>
              <th className="p-3 eyebrow">Status</th>
              <th className="p-3 eyebrow">Joined</th>
              <th className="p-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline">
            {filtered.map((v) => (
              <tr key={v.vendor_id}>
                <td className="p-3 font-semibold">{v.business_name}</td>
                <td className="p-3">{v.business_category}</td>
                <td className="p-3">{v.phone ?? '—'}</td>
                <td className="p-3">
                  <span className={`text-[11px] px-2 py-1 rounded-full border ${
                    v.verification_status === 'verified' ? 'border-forest text-forest'
                      : v.verification_status === 'rejected' ? 'border-brick text-brick'
                      : 'border-gold text-gold'
                  }`}>
                    {v.verification_status}
                  </span>
                </td>
                <td className="p-3 text-warm">{new Date(v.created_at).toLocaleDateString()}</td>
                <td className="p-3 text-right">
                  {v.verification_status === 'verified' && (
                    <button className="text-brick text-xs hover:underline" onClick={() => revoke(v)}>
                      Revoke
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr><td colSpan={6} className="p-6 text-center text-warm">No vendors match.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
