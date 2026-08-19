import React, { useEffect, useState } from 'react'
import { fetchFlagged, resolveFlag } from '../services/dataService'

export default function Moderation() {
  const [items, setItems] = useState([])
  const [error, setError] = useState(null)

  const load = async () => {
    try {
      const data = await fetchFlagged()
      setItems(data)
    } catch (e) {
      setError(e.message)
    }
  }
  useEffect(() => { load() }, [])

  const act = async (item, action) => {
    try {
      await resolveFlag(item.kind, item.id, action)
      await load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div>
      <div className="eyebrow">Content</div>
      <h1 className="text-4xl mt-2 mb-8">Moderation</h1>
      {error && <p className="mb-4 text-brick text-sm">{error}</p>}

      {items.length === 0 ? (
        <div className="card p-10 text-center">
          <p className="font-display text-2xl mb-2">Nothing flagged</p>
          <p className="text-warm text-sm">
            Reviews, posts and comments reported by users land here.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {items.map((item) => (
            <div key={`${item.kind}-${item.id}`} className="card p-5">
              <div className="flex items-center justify-between mb-2">
                <span className="eyebrow">{item.kind}</span>
                <span className="text-xs text-warm">
                  {new Date(item.created_at).toLocaleString()}
                </span>
              </div>
              <p className="mb-4">{item.body || <em className="text-warm">No text</em>}</p>
              <div className="flex gap-3">
                <button className="btn-ghost" onClick={() => act(item, 'keep')}>
                  Keep, clear flag
                </button>
                <button className="btn-danger" onClick={() => act(item, 'delete')}>
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
