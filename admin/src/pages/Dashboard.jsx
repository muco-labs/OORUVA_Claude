import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchStats, fetchBusinesses } from '../services/dataService'

function Stat({ label, value, accent }) {
  return (
    <div className="card p-6">
      <div className="eyebrow">{label}</div>
      <div className={`font-display text-4xl mt-2 ${accent ? 'text-gold' : ''}`}>{value}</div>
    </div>
  )
}

export function StatusPill({ status }) {
  const tone =
    status === 'verified' ? 'border-forest text-forest'
      : status === 'rejected' || status === 'suspended' ? 'border-brick text-brick'
      : status === 'draft' ? 'border-outline text-warm'
      : 'border-gold text-gold'
  return <span className={`text-[11px] px-2 py-1 rounded-full border shrink-0 ${tone}`}>{status}</span>
}

export default function Dashboard() {
  const [stats, setStats] = useState(null)
  const [recent, setRecent] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    ;(async () => {
      try {
        setStats(await fetchStats())
        setRecent((await fetchBusinesses()).slice(0, 5))
      } catch (e) {
        setError(e.message)
      }
    })()
  }, [])

  if (error) return <p className="text-brick">{error}</p>
  if (!stats) return <p className="text-warm">Loading…</p>

  return (
    <div>
      <div className="eyebrow">Overview</div>
      <h1 className="text-4xl mt-2 mb-8">Platform</h1>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
        <Stat label="Users" value={stats.users} />
        <Stat label="Businesses" value={stats.businesses} />
        <Stat label="Awaiting review" value={stats.pending} accent />
        <Stat label="Verified" value={stats.verified} />
      </div>

      <div className="grid lg:grid-cols-2 gap-6">
        <div className="card p-6">
          <div className="flex items-baseline justify-between mb-4">
            <h2 className="text-xl">Newest businesses</h2>
            <Link to="/vendors" className="text-sm text-gold hover:underline">All businesses</Link>
          </div>
          {recent.length === 0 && <p className="text-warm text-sm">No businesses yet.</p>}
          <ul className="divide-y divide-outline">
            {recent.map((b) => (
              <li key={b.id} className="py-3 flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <div className="font-semibold truncate">{b.name}</div>
                  <div className="text-xs text-warm truncate">
                    {b.business_types?.name ?? 'Type not set'}
                    {b.address ? ` · ${b.address}` : ''}
                  </div>
                </div>
                <StatusPill status={b.status} />
              </li>
            ))}
          </ul>
        </div>

        <div className="card p-6">
          <h2 className="text-xl mb-4">Waiting on you</h2>
          <p className="font-display text-5xl text-gold">{stats.pending}</p>
          <p className="text-sm text-warm mt-2 mb-5">
            {stats.pending === 1 ? 'business is' : 'businesses are'} waiting for verification.
            The published SLA is 48 hours.
          </p>
          <Link to="/verification" className="btn-gold inline-block">Open the queue</Link>
        </div>
      </div>
    </div>
  )
}
