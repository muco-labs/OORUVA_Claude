import { createClient } from '@supabase/supabase-js'

const url = import.meta.env.VITE_SUPABASE_URL
const key = import.meta.env.VITE_SUPABASE_ANON_KEY

export const isConfigured = Boolean(url && key)

// When the keys are absent the dashboard still renders, on demo data, with a
// banner saying so. Better than a blank screen during setup.
export const supabase = isConfigured ? createClient(url, key) : null
