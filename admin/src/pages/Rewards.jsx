import React, { useEffect, useState } from 'react'
import { fetchRewards, fetchSettings, saveSetting, exportCsv } from '../services/dataService'

const editable = [
  'points_per_review',
  'points_per_photo',
  'points_per_post',
  'points_per_checkin',
  'points_to_rupee',
]

export default function Rewards() {
  const [rows, setRows] = useState([])
  const [settings, setSettings] = useState([])
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(null)

  const load = async () => {
    try {
      const r = await fetchRewards()
      setRows(r.data)
      const s = await fetchSettings()
      setSettings(s.data.filter((x) => editable.includes(x.key)))
    } catch (e) {
      setError(e.message)
    }
  }
  useEffect(() => { load() }, [])

  const update = async (key, value) => {
    try {
      await saveSetting(key, value)
      setSaved(key)
      setTimeout(() => setSaved(null), 1500)
    } catch (e) {
      setError(e.message)
    }
  }

  const totals = rows.reduce((acc, r) => {
    acc[r.status] = (acc[r.status] ?? 0) + r.points
    return acc
  }, {})

  return (
    <div>
      <div className="eyebrow">Loyalty</div>
      <h1 className="text-4xl mt-2 mb-8">Rewards</h1>
      {error && <p className="mb-4 text-brick text-sm">{error}</p>}

      <div className="grid sm:grid-cols-3 gap-4 mb-10">
        {['pending', 'verified', 'credited'].map((s) => (
          <div key={s} className="card p-5">
            <div className="eyebrow">{s}</div>
            <div className="font-display text-3xl mt-1">{totals[s] ?? 0}</div>
          </div>
        ))}
      </div>

      <div className="card p-6 mb-8">
        <h2 className="text-xl mb-4">Earning rates</h2>
        <div className="grid sm:grid-cols-2 gap-4">
          {settings.map((s) => (
            <label key={s.key} className="block">
              <span className="eyebrow">{s.key.replaceAll('_', ' ')}</span>
              <div className="flex gap-2 mt-1">
                <input
                  defaultValue={s.value}
                  onBlur={(e) => update(s.key, e.target.value)}
                  className="border border-outline rounded-[10px] px-3 py-2 text-sm w-full focus:outline-none focus:border-gold"
                />
                {saved === s.key && <span className="text-forest text-xs self-center">saved</span>}
              </div>
            </label>
          ))}
        </div>
        <p className="text-xs text-warm mt-4">
          Points are written server-side only. Clients cannot insert into the rewards table under
          RLS, which is what stops the scheme being farmed.
        </p>
      </div>

      <div className="flex items-center justify-between mb-3">
        <h2 className="text-xl">Ledger</h2>
        <button className="btn-ghost" onClick={() => exportCsv(rows, 'ooruva-rewards.csv')}>
          Export CSV
        </button>
      </div>

      <div className="card overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-ivory">
            <tr className="text-left">
              <th className="p-3 eyebrow">Customer</th>
              <th className="p-3 eyebrow">Activity</th>
              <th className="p-3 eyebrow">Points</th>
              <th className="p-3 eyebrow">Status</th>
              <th className="p-3 eyebrow">When</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline">
            {rows.map((r) => (
              <tr key={r.id}>
                <td className="p-3 font-mono text-xs">{r.customer_id?.slice(0, 8)}</td>
                <td className="p-3">{r.activity_type}</td>
                <td className="p-3 font-semibold">+{r.points}</td>
                <td className="p-3">{r.status}</td>
                <td className="p-3 text-warm">{new Date(r.created_at).toLocaleString()}</td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan={5} className="p-6 text-center text-warm">No reward activity yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
