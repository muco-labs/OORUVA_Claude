import React, { useEffect, useState } from 'react'
import { fetchSettings, saveSetting } from '../services/dataService'

export default function Settings() {
  const [rows, setRows] = useState([])
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(null)

  useEffect(() => {
    fetchSettings()
      .then(({ data }) => setRows(data))
      .catch((e) => setError(e.message))
  }, [])

  const update = async (key, value) => {
    try {
      await saveSetting(key, value)
      setSaved(key)
      setTimeout(() => setSaved(null), 1500)
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div>
      <div className="eyebrow">Platform</div>
      <h1 className="text-4xl mt-2 mb-8">Settings</h1>
      {error && <p className="mb-4 text-brick text-sm">{error}</p>}

      <div className="card p-6 space-y-4 max-w-2xl">
        {rows.map((s) => (
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
        {rows.length === 0 && (
          <p className="text-warm text-sm">No settings rows yet. Run 03_seed.sql.</p>
        )}
      </div>
    </div>
  )
}
