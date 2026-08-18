import React, { useEffect, useState } from 'react'
import { fetchVendors, setVerification, certificateUrl } from '../services/dataService'

/**
 * The screen this whole platform hinges on: a human deciding whether a food
 * stall is who it says it is. Certificate first, decision second, reason
 * recorded either way.
 */
export default function Verification() {
  const [vendors, setVendors] = useState([])
  const [selected, setSelected] = useState(null)
  const [certUrl, setCertUrl] = useState(null)
  const [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  const load = async () => {
    try {
      const { data } = await fetchVendors('pending')
      setVendors(data)
      setSelected((prev) => data.find((v) => v.vendor_id === prev?.vendor_id) ?? data[0] ?? null)
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => { load() }, [])

  useEffect(() => {
    const path = selected?.fssai_records?.[0]?.certificate_url
    if (!path) { setCertUrl(null); return }
    certificateUrl(path).then(setCertUrl).catch(() => setCertUrl(null))
  }, [selected])

  const decide = async (status) => {
    if (status === 'rejected' && !notes.trim()) {
      setError('A rejection needs a reason — the vendor sees it.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await setVerification(selected.vendor_id, status, notes.trim() || null)
      setNotes('')
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const fssai = selected?.fssai_records?.[0]

  return (
    <div>
      <div className="eyebrow">Needs a human</div>
      <h1 className="text-4xl mt-2 mb-8">Verification queue</h1>

      {error && <p className="mb-4 text-brick text-sm">{error}</p>}

      {vendors.length === 0 ? (
        <div className="card p-10 text-center">
          <p className="font-display text-2xl mb-2">Queue is clear</p>
          <p className="text-warm text-sm">Nothing is waiting for review.</p>
        </div>
      ) : (
        <div className="grid lg:grid-cols-3 gap-6">
          <div className="card divide-y divide-outline overflow-hidden">
            {vendors.map((v) => (
              <button
                key={v.vendor_id}
                onClick={() => setSelected(v)}
                className={`w-full text-left p-4 hover:bg-ivory ${
                  selected?.vendor_id === v.vendor_id ? 'bg-gold-container' : ''
                }`}
              >
                <div className="font-semibold">{v.business_name}</div>
                <div className="text-xs text-warm">{v.business_category}</div>
                <div className="text-[11px] text-warm mt-1">
                  Joined {new Date(v.created_at).toLocaleDateString()}
                </div>
              </button>
            ))}
          </div>

          {selected && (
            <div className="lg:col-span-2 card p-6">
              <h2 className="text-2xl mb-1">{selected.business_name}</h2>
              <p className="text-sm text-warm mb-6">{selected.business_category}</p>

              <dl className="grid sm:grid-cols-2 gap-4 mb-6 text-sm">
                <div><dt className="eyebrow">Owner</dt><dd>{selected.owner_name ?? '—'}</dd></div>
                <div><dt className="eyebrow">Phone</dt><dd>{selected.phone ?? '—'}</dd></div>
                <div className="sm:col-span-2"><dt className="eyebrow">Address</dt><dd>{selected.address ?? '—'}</dd></div>
                <div><dt className="eyebrow">Hours</dt><dd>{selected.opening_hours ?? '—'}</dd></div>
                <div>
                  <dt className="eyebrow">Coordinates</dt>
                  <dd>{selected.location_lat?.toFixed(4)}, {selected.location_lng?.toFixed(4)}</dd>
                </div>
              </dl>

              <div className="border border-outline rounded-2xl p-4 mb-6">
                <div className="eyebrow mb-2">FSSAI</div>
                {!fssai && <p className="text-sm text-warm">No record submitted.</p>}
                {fssai && (
                  <>
                    <p className="text-sm mb-1">
                      Number: <span className="font-semibold">{fssai.fssai_number ?? 'not provided'}</span>
                    </p>
                    <p className="text-sm mb-3">
                      Status: <span className="font-semibold">{fssai.status}</span>
                      {fssai.status === 'needs_assistance' &&
                        ' — vendor asked for help obtaining a certificate'}
                    </p>
                    {certUrl ? (
                      certUrl.toLowerCase().includes('.pdf') ? (
                        <iframe title="FSSAI certificate" src={certUrl} className="w-full h-96 rounded-lg border border-outline" />
                      ) : (
                        <img src={certUrl} alt="FSSAI certificate" className="max-h-96 rounded-lg border border-outline" />
                      )
                    ) : (
                      <p className="text-sm text-warm">
                        {fssai.certificate_url ? 'Generating a signed link…' : 'No certificate uploaded.'}
                      </p>
                    )}
                  </>
                )}
              </div>

              <label className="block eyebrow mb-2">Notes to the vendor</label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={3}
                placeholder="Required when rejecting. Say exactly what to fix."
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
