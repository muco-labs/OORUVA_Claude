import React, { useEffect, useState } from 'react'
import { fetchRewardLedger, fetchRewardRules, saveRewardRule, exportCsv } from '../services/dataService'

/**
 * Reward rules and the ledger.
 *
 * The rates are rows in reward_rules, not constants in the app — changing what
 * a review is worth is an edit here, not a release. The ledger below is
 * append-only and shows both directions, so a balance is always reconstructible
 * from what is on screen.
 */
export default function Rewards() {
  const [ledger, setLedger] = useState([])
  const [rules, setRules] = useState([])
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(null)
  const [loading, setLoading] = useState(true)

  const load = async () => {
    try {
      setRules(await fetchRewardRules())
      setLedger(await fetchRewardLedger())
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => { load() }, [])

  const updateRule = async (activityType, field, raw) => {
    const value = field === 'points' || field === 'daily_cap'
      ? (raw === '' ? null : Number(raw))
      : raw

    if ((field === 'points' || field === 'daily_cap') && value !== null && Number.isNaN(value)) {
      setError('That needs to be a number.')
      return
    }
    if (field === 'points' && value < 0) {
      setError('Points cannot be negative. Use a debit transaction to take points away.')
      return
    }

    try {
      setError(null)
      await saveRewardRule(activityType, { [field]: value })
      setSaved(`${activityType}.${field}`)
      setTimeout(() => setSaved(null), 1500)
      await load()
    } catch (e) {
      setError(e.message)
    }
  }

  // Only credited rows move a balance; pending and reversed are shown but not
  // counted, which is the same rule reward_balance() applies in the database.
  const totals = ledger.reduce((acc, t) => {
    if (t.status !== 'credited') {
      acc.uncounted += t.points
      return acc
    }
    acc.net += t.direction === 'credit' ? t.points : -t.points
    return acc
  }, { net: 0, uncounted: 0 })

  if (loading) return <p className="text-warm">Loading…</p>

  return (
    <div>
      <div className="eyebrow">Loyalty</div>
      <h1 className="text-4xl mt-2 mb-8">Rewards</h1>
      {error && <p className="mb-4 text-brick text-sm">{error}</p>}

      <div className="grid sm:grid-cols-3 gap-4 mb-10">
        <div className="card p-5">
          <div className="eyebrow">Points outstanding</div>
          <div className="font-display text-3xl mt-1">{totals.net}</div>
        </div>
        <div className="card p-5">
          <div className="eyebrow">Not yet credited</div>
          <div className="font-display text-3xl mt-1">{totals.uncounted}</div>
        </div>
        <div className="card p-5">
          <div className="eyebrow">Ledger entries</div>
          <div className="font-display text-3xl mt-1">{ledger.length}</div>
        </div>
      </div>

      <div className="card p-6 mb-10">
        <h2 className="text-xl mb-1">Earning rates</h2>
        <p className="text-sm text-warm mb-5">
          These take effect immediately for new activity. Points already in the ledger are not
          recalculated — a customer who earned ten points keeps ten.
        </p>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-ivory">
              <tr className="text-left">
                <th className="p-3 eyebrow">Activity</th>
                <th className="p-3 eyebrow">Points</th>
                <th className="p-3 eyebrow">Daily cap</th>
                <th className="p-3 eyebrow">Active</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline">
              {rules.map((r) => (
                <tr key={r.activity_type}>
                  <td className="p-3">
                    <div className="font-semibold">{r.label}</div>
                    <div className="text-xs text-warm">{r.description}</div>
                  </td>
                  <td className="p-3">
                    <input
                      type="number"
                      min="0"
                      defaultValue={r.points}
                      onBlur={(e) => updateRule(r.activity_type, 'points', e.target.value)}
                      className="border border-outline rounded-[10px] px-2 py-1 text-sm w-24 focus:outline-none focus:border-gold"
                    />
                    {saved === `${r.activity_type}.points` && (
                      <span className="text-forest text-xs ml-2">saved</span>
                    )}
                  </td>
                  <td className="p-3">
                    <input
                      type="number"
                      min="0"
                      placeholder="none"
                      defaultValue={r.daily_cap ?? ''}
                      onBlur={(e) => updateRule(r.activity_type, 'daily_cap', e.target.value)}
                      className="border border-outline rounded-[10px] px-2 py-1 text-sm w-24 focus:outline-none focus:border-gold"
                    />
                    {saved === `${r.activity_type}.daily_cap` && (
                      <span className="text-forest text-xs ml-2">saved</span>
                    )}
                  </td>
                  <td className="p-3">
                    <input
                      type="checkbox"
                      defaultChecked={r.active}
                      onChange={(e) => updateRule(r.activity_type, 'active', e.target.checked)}
                    />
                  </td>
                </tr>
              ))}
              {rules.length === 0 && (
                <tr><td colSpan={4} className="p-6 text-center text-warm">No rules configured.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        <p className="text-xs text-warm mt-4">
          Points are minted server-side only. No client can insert into reward_transactions under
          RLS, which is what stops the scheme being farmed from a decompiled APK.
        </p>
      </div>

      <div className="flex items-center justify-between mb-3">
        <h2 className="text-xl">Ledger</h2>
        <button className="btn-ghost" onClick={() => exportCsv(ledger, 'ooruva-reward-ledger.csv')}>
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
            {ledger.map((t) => (
              <tr key={t.id}>
                <td className="p-3 font-mono text-xs">{t.customer_id?.slice(0, 8)}</td>
                <td className="p-3">{t.activity_type}</td>
                <td className={`p-3 font-semibold ${t.direction === 'debit' ? 'text-brick' : 'text-forest'}`}>
                  {t.direction === 'debit' ? '−' : '+'}{t.points}
                </td>
                <td className="p-3">{t.status}</td>
                <td className="p-3 text-warm">{new Date(t.created_at).toLocaleString()}</td>
              </tr>
            ))}
            {ledger.length === 0 && (
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
