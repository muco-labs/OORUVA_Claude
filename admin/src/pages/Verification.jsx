import React, { useEffect, useState } from 'react'
import { fetchQueue, decideVerification, decideDocument, documentUrl, fetchCatalogue } from '../services/dataService'

/**
 * The screen this whole platform hinges on: a human deciding whether a business
 * is who it says it is. Documents first, decision second, reason recorded
 * either way.
 *
 * OORUVA has no authorised government verification API. Nothing here checks a
 * licence number against a registry, and nothing pretends to — the outcome is
 * recorded as manual_review because that is what it is.
 */
export default function Verification() {
  const [queue, setQueue] = useState([])
  const [selected, setSelected] = useState(null)
  const [catalogue, setCatalogue] = useState([])
  const [docUrls, setDocUrls] = useState({})
  const [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  const load = async () => {
    try {
      const data = await fetchQueue()
      setQueue(data)
      setSelected((prev) => data.find((b) => b.id === prev?.id) ?? data[0] ?? null)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  // Signed URLs are minted per selection and never stored. They expire in five
  // minutes; a certificate link that outlives the review does not.
  useEffect(() => {
    setDocUrls({})
    setCatalogue([])
    if (!selected) return

    ;(async () => {
      const entries = await Promise.all(
        (selected.business_documents ?? []).map(async (d) => [d.id, await documentUrl(d.storage_path)])
      )
      setDocUrls(Object.fromEntries(entries))

      try {
        setCatalogue(await fetchCatalogue(selected.id))
      } catch {
        setCatalogue([])
      }
    })()
  }, [selected])

  const decide = async (status) => {
    if (status !== 'verified' && !notes.trim()) {
      setError('A rejection or change request needs a reason — the vendor sees it.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await decideVerification(selected.id, status, notes.trim() || null)
      setNotes('')
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const markDocument = async (doc, status) => {
    setBusy(true)
    try {
      await decideDocument(doc.id, status, notes.trim() || null)
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const type = selected?.business_types
  const category = type?.business_categories

  if (loading) return <p className="text-warm">Loading the queue…</p>

  return (
    <div>
      <div className="eyebrow">Needs a human</div>
      <h1 className="text-4xl mt-2 mb-8">Verification queue</h1>

      {error && <p className="mb-4 text-brick text-sm">{error}</p>}

      {queue.length === 0 ? (
        <div className="card p-10 text-center">
          <p className="font-display text-2xl mb-2">Queue is clear</p>
          <p className="text-warm text-sm">Nothing is waiting for review.</p>
        </div>
      ) : (
        <div className="grid lg:grid-cols-3 gap-6">
          <div className="card divide-y divide-outline overflow-hidden self-start">
            {queue.map((b) => (
              <button
                key={b.id}
                onClick={() => setSelected(b)}
                className={`w-full text-left p-4 hover:bg-ivory ${
                  selected?.id === b.id ? 'bg-gold-container' : ''
                }`}
              >
                <div className="font-semibold">{b.name}</div>
                <div className="text-xs text-warm">
                  {b.business_types?.name ?? 'Type not set'}
                </div>
                <div className="text-[11px] text-warm mt-1">
                  {b.submitted_at
                    ? `Submitted ${new Date(b.submitted_at).toLocaleDateString()}`
                    : 'Submission date unknown'}
                  {' · '}{b.profile_completeness ?? 0}% complete
                </div>
              </button>
            ))}
          </div>

          {selected && (
            <div className="lg:col-span-2 card p-6">
              <h2 className="text-2xl mb-1">{selected.name}</h2>
              <p className="text-sm text-warm mb-6">
                {category?.name ?? 'Uncategorised'}
                {type ? ` · ${type.name}` : ''}
              </p>

              <dl className="grid sm:grid-cols-2 gap-4 mb-6 text-sm">
                <div><dt className="eyebrow">Owner</dt><dd>{selected.owner_name ?? '—'}</dd></div>
                <div><dt className="eyebrow">Phone</dt><dd>{selected.phone ?? '—'}</dd></div>
                <div className="sm:col-span-2">
                  <dt className="eyebrow">Address</dt>
                  <dd>{selected.address ?? '—'}{selected.district ? `, ${selected.district}` : ''}</dd>
                </div>
                <div><dt className="eyebrow">Hours</dt><dd>{selected.opening_hours ?? '—'}</dd></div>
                <div>
                  <dt className="eyebrow">Coordinates</dt>
                  <dd>
                    {selected.location_lat != null && selected.location_lng != null
                      ? `${selected.location_lat.toFixed(4)}, ${selected.location_lng.toFixed(4)}`
                      : 'Not provided — place from the address'}
                  </dd>
                </div>
                <div className="sm:col-span-2">
                  <dt className="eyebrow">Description</dt>
                  <dd>{selected.description ?? '—'}</dd>
                </div>
              </dl>

              <div className="border border-outline rounded-2xl p-4 mb-6">
                <div className="eyebrow mb-3">Catalogue ({catalogue.length})</div>
                {catalogue.length === 0 ? (
                  <p className="text-sm text-warm">Nothing listed yet.</p>
                ) : (
                  <ul className="text-sm divide-y divide-outline">
                    {catalogue.map((item) => (
                      <li key={item.id} className="py-2 flex justify-between">
                        <span>{item.name} <span className="text-warm text-xs">({item.kind})</span></span>
                        <span className="font-semibold">₹{item.price}{item.unit ? ` ${item.unit}` : ''}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              <div className="border border-outline rounded-2xl p-4 mb-6">
                <div className="eyebrow mb-3">Documents</div>

                {(selected.business_documents ?? []).length === 0 && (
                  <p className="text-sm text-warm">
                    None submitted. That is not automatically a problem — the vendor may have
                    said none applies to their business type.
                  </p>
                )}

                {(selected.business_documents ?? []).map((doc) => (
                  <div key={doc.id} className="mb-5 last:mb-0">
                    <p className="text-sm mb-1">
                      <span className="font-semibold uppercase">{doc.document_type}</span>
                      {' · '}
                      {doc.document_number ?? 'no number given'}
                      {' · '}
                      <span className="text-warm">{doc.status}</span>
                    </p>

                    {docUrls[doc.id] ? (
                      docUrls[doc.id].toLowerCase().includes('.pdf') ? (
                        <iframe
                          title={`${doc.document_type} document`}
                          src={docUrls[doc.id]}
                          className="w-full h-96 rounded-lg border border-outline"
                        />
                      ) : (
                        <img
                          src={docUrls[doc.id]}
                          alt={`${doc.document_type} document`}
                          className="max-h-96 rounded-lg border border-outline"
                        />
                      )
                    ) : (
                      <p className="text-sm text-warm">
                        {doc.storage_path ? 'Generating a signed link…' : 'No file uploaded.'}
                      </p>
                    )}

                    <div className="flex gap-2 mt-2">
                      <button
                        disabled={busy}
                        onClick={() => markDocument(doc, 'verified')}
                        className="btn-ghost text-xs"
                      >
                        Mark checked
                      </button>
                      <button
                        disabled={busy}
                        onClick={() => markDocument(doc, 'needs_action')}
                        className="btn-ghost text-xs"
                      >
                        Needs action
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              <label className="block eyebrow mb-2" htmlFor="notes">Notes to the vendor</label>
              <textarea
                id="notes"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={3}
                placeholder="Required when rejecting or requesting changes. Say exactly what to fix."
                className="w-full border border-outline rounded-[10px] p-3 text-sm mb-4 focus:outline-none focus:border-gold"
              />

              <div className="flex flex-wrap gap-3">
                <button disabled={busy} onClick={() => decide('verified')} className="btn-gold">
                  Approve
                </button>
                <button disabled={busy} onClick={() => decide('needs_changes')} className="btn-ghost">
                  Request changes
                </button>
                <button disabled={busy} onClick={() => decide('rejected')} className="btn-danger">
                  Reject
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
