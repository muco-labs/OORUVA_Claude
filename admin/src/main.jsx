import React, { useEffect, useState } from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route, NavLink, Navigate } from 'react-router-dom'
import './index.css'

import { currentAdmin, signOut } from './services/session'
import Login from './pages/Login'
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

function Shell({ admin, onSignOut, children }) {
  return (
    <div className="min-h-screen flex">
      <aside className="w-60 shrink-0 border-r border-outline bg-white p-6 hidden md:flex md:flex-col">
        <div className="mb-8">
          <div className="eyebrow">Muco Labs</div>
          <h1 className="text-2xl font-display mt-1">OORUVA</h1>
          <div className="text-xs text-warm mt-1">Admin console</div>
        </div>
        <nav className="space-y-1 flex-1">
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

        {admin && (
          <div className="pt-4 border-t border-outline">
            <div className="text-xs text-warm truncate" title={admin.email}>{admin.email}</div>
            <button onClick={onSignOut} className="text-xs text-brick hover:underline mt-1">
              Sign out
            </button>
          </div>
        )}
      </aside>

      <main className="flex-1 min-w-0">
        <div className="p-6 md:p-10 max-w-6xl">{children}</div>
      </main>
    </div>
  )
}

function App() {
  const [admin, setAdmin] = useState(null)
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    ;(async () => {
      setAdmin(await currentAdmin())
      setChecking(false)
    })()
  }, [])

  // Nothing renders until the session is resolved. Showing the shell first and
  // swapping it out would flash the console at someone who is not signed in.
  if (checking) {
    return (
      <div className="min-h-screen flex items-center justify-center text-warm text-sm">
        Checking your session…
      </div>
    )
  }

  // The demo-data path is gone. It let anyone who opened the URL browse a
  // console full of plausible rows with no sign-in at all, which is a worse
  // first impression than an honest lock — and a habit that would eventually
  // ship. Configuration problems are now explained on the login screen.
  if (!admin) return <Login onSignedIn={setAdmin} />

  return (
    <BrowserRouter>
      <Shell
        admin={admin}
        onSignOut={async () => {
          await signOut()
          setAdmin(null)
        }}
      >
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
  )
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
