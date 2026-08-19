import React, { useState } from 'react'
import { signIn } from '../services/session'
import { isConfigured } from '../services/supabaseClient'

export default function Login({ onSignedIn }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      onSignedIn(await signIn(email, password))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-6">
      <div className="card p-8 w-full max-w-sm">
        <div className="eyebrow">Muco Labs</div>
        <h1 className="text-3xl font-display mt-1">OORUVA</h1>
        <div className="text-xs text-warm mt-1 mb-8">Admin console</div>

        {!isConfigured && (
          <div className="bg-gold-container border border-outline rounded-[10px] px-3 py-2 text-sm mb-6">
            Supabase is not configured. Add <code>VITE_SUPABASE_URL</code> and{' '}
            <code>VITE_SUPABASE_ANON_KEY</code> to <code>admin/.env</code>, then reload.
          </div>
        )}

        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="eyebrow block mb-1" htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full border border-outline rounded-[10px] px-3 py-2 text-sm"
            />
          </div>

          <div>
            <label className="eyebrow block mb-1" htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full border border-outline rounded-[10px] px-3 py-2 text-sm"
            />
          </div>

          {error && <p className="text-brick text-sm">{error}</p>}

          <button type="submit" className="btn-gold w-full" disabled={busy || !isConfigured}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="text-xs text-warm mt-6">
          Admin accounts are created by an operator, never self-registered.
          See <code>supabase/08_admin_access.sql</code>.
        </p>
      </div>
    </div>
  )
}
