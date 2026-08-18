import React, { useEffect, useMemo, useState } from 'react'
import { fetchUsers, setUserSuspended, exportCsv } from '../services/dataService'

export default function Users() {
  const [rows, setRows] = useState([])
  const [role, setRole] = useState('')
  const [q, setQ] = useState('')
  const [error, setError] = useState(null)

  const load = async () => {
    try {
      const { data } = await fetchUsers()
      setRows(data)
    } catch (e) {
      setError(e.message)
    }
  }
  useEffect(() => { load() }, [])

  const filtered = useMemo(
    () => rows.filter((u) => !role || u.role === role).filter((u) => !q || u.phone?.includes(q)),
    [rows, role, q]
  )

  const toggle = async (u) => {
    try {
      await setUserSuspended(u.id, !u.suspended)
      await load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div>
      <div className="eyebrow">People</div>
      <h1 className="text-4xl mt-2 mb-8">Users</h1>
      {error && <p className="mb-4 text-brick text-sm">{error}</p>}

      <div className="flex flex-wrap gap-3 mb-6">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search phone"
          className="border border-outline rounded-[10px] px-3 py-2 text-sm w-56 focus:outline-none focus:border-gold"
        />
        <select
          value={role}
          onChange={(e) => setRole(e.target.value)}
          className="border border-outline rounded-[10px] px-3 py-2 text-sm"
        >
          <option value="">All roles</option>
          <option value="customer">Customer</option>
          <option value="vendor">Vendor</option>
          <option value="admin">Admin</option>
        </select>
        <button className="btn-ghost" onClick={() => exportCsv(filtered, 'ooruva-users.csv')}>
          Export CSV
        </button>
      </div>

      <div className="card overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-ivory">
            <tr className="text-left">
              <th className="p-3 eyebrow">Phone</th>
              <th className="p-3 eyebrow">Role</th>
              <th className="p-3 eyebrow">Joined</th>
              <th className="p-3 eyebrow">State</th>
              <th className="p-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline">
            {filtered.map((u) => (
              <tr key={u.id}>
                <td className="p-3 font-semibold">{u.phone}</td>
                <td className="p-3 capitalize">{u.role}</td>
                <td className="p-3 text-warm">{new Date(u.created_at).toLocaleDateString()}</td>
                <td className="p-3">
                  <span className={u.suspended ? 'text-brick' : 'text-forest'}>
                    {u.suspended ? 'Suspended' : 'Active'}
                  </span>
                </td>
                <td className="p-3 text-right">
                  <button className="text-xs hover:underline" onClick={() => toggle(u)}>
                    {u.suspended ? 'Restore' : 'Suspend'}
                  </button>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={5} className="p-6 text-center text-warm">No users match.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
