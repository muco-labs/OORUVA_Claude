import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchStats, fetchVendors } from '../services/dataService'

function Stat({ label, value, accent }) {
  return (
    <div className="card p-6">
      <div className="eyebrow">{label}</div>
      <div className={`font-display text-4xl mt-2 ${accent ? 'text-gold' : ''}`}>{value}</div>
    </div>
  )
}

export default function Dashboard() {
  const [stats, setStats] = useState(null)
  const [recent, setRecent] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    ;(async () => {
      try {
        const s = await fetchStats()
        const v = await fetchVendors()
        setStats(s.data)
        setRecent(v.data.slice(0, 5))
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
        <Stat label="Vendors" value={stats.vendors} />
        <Stat label="Pending review" value={stats.pending} accent />
        <Stat label="Reviews" value={stats.reviews} />
      </div>

      <div className="grid lg:grid-cols-2 gap-6">
        <div className="card p-6">
          <div className="flex items-baseline justify-between mb-4">
            <h2 className="text-xl">Newest vendors</h2>
            <Link to="/vendors" className="text-sm text-gold hover:underline">All vendors</Link>
          </div>
          {recent.length === 0 && <p className="text-warm text-sm">No vendors yet.</p>}
          <ul className="divide-y divide-outline">
            {recent.map((v) => (
              <li key={v.id} className="py-3 flex items-center justify-between">
                <div>
                  <div className="font-semibold">{v.business_name}</div>
                  <div className="text-xs text-warm">{v.business_category} · {v.address}</div>
                </div>
                <span
                  className={`text-[11px] px-2 py-1 rounded-full border ${
                    v.verification_status === 'verified'
                      ? 'border-forest text-forest'
                      : v.verification_status === 'rejected'
                      ? 'border-brick text-brick'
                      : 'border-gold text-gold'
                  }`}
                >
                  {v.verification_status}
                </span>
              </li>
            ))}
          </ul>
        </div>

        <div className="card p-6">
          <h2 className="text-xl mb-4">Waiting on you</h2>
          <p className="font-display text-5xl text-gold">{stats.pending}</p>
          <p className="text-sm text-warm mt-2 mb-5">
            {stats.pending === 1 ? 'vendor is' : 'vendors are'} waiting for verification.
            The published SLA is 48 hours.
          </p>
          <Link to="/verification" className="btn-gold inline-block">Open the queue</Link>
        </div>
      </div>
    </div>
  )
}
