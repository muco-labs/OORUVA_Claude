import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route, NavLink, Navigate } from 'react-router-dom'
import './index.css'
import { isConfigured } from './services/supabaseClient'
import Dashboard from './pages/Dashboard'
import Verification from './pages/Verification'
import Vendors from './pages/Vendors'
import Users from './pages/Users'
import Moderation from './pages/Moderation'
import Rewards from './pages/Rewards'
import Settings from './pages/Settings'

const nav = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/verification', label: 'Verification' },
  { to: '/vendors', label: 'Vendors' },
  { to: '/users', label: 'Users' },
  { to: '/moderation', label: 'Moderation' },
  { to: '/rewards', label: 'Rewards' },
  { to: '/settings', label: 'Settings' },
]

function Shell({ children }) {
  return (
    <div className="min-h-screen flex">
      <aside className="w-60 shrink-0 border-r border-outline bg-white p-6 hidden md:block">
        <div className="mb-8">
          <div className="eyebrow">Muco Labs</div>
          <h1 className="text-2xl font-display mt-1">OORUVA</h1>
          <div className="text-xs text-warm mt-1">Admin console</div>
        </div>
        <nav className="space-y-1">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `block px-3 py-2 rounded-[10px] text-sm ${
                  isActive ? 'bg-gold-container text-espresso font-semibold' : 'text-warm hover:bg-ivory'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <main className="flex-1 min-w-0">
        {!isConfigured && (
          <div className="bg-gold-container border-b border-outline px-6 py-3 text-sm">
            <strong className="font-semibold">Demo data.</strong> Add
            <code className="mx-1 px-1 bg-white rounded">VITE_SUPABASE_URL</code> and
            <code className="mx-1 px-1 bg-white rounded">VITE_SUPABASE_ANON_KEY</code>
            to <code className="px-1 bg-white rounded">admin/.env</code> to connect the real project.
            Approvals and edits are disabled until then.
          </div>
        )}
        <div className="p-6 md:p-10 max-w-6xl">{children}</div>
      </main>
    </div>
  )
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <Shell>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/verification" element={<Verification />} />
          <Route path="/vendors" element={<Vendors />} />
          <Route path="/users" element={<Users />} />
          <Route path="/moderation" element={<Moderation />} />
          <Route path="/rewards" element={<Rewards />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Shell>
    </BrowserRouter>
  </React.StrictMode>
)
