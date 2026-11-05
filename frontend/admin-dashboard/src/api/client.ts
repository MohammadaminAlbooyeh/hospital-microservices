import axios from 'axios'

export const TOKEN_KEY = 'admin_dashboard_token'
export const PROFILE_KEY = 'admin_dashboard_profile'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(PROFILE_KEY)
      const path = window.location.pathname
      if (!path.startsWith('/login')) {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

export function extractApiMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as
      | { message?: string; error?: string; detail?: unknown; title?: string }
      | undefined
    if (data) {
      if (Array.isArray(data.detail)) {
        return data.detail
          .map((item) =>
            typeof item === 'object' && item !== null && 'msg' in item
              ? String((item as { msg: unknown }).msg)
              : String(item),
          )
          .join('; ')
      }
      if (typeof data.detail === 'string') return data.detail
      if (data.message) return data.message
      if (data.error) return data.error
      if (data.title) return data.title
    }
    if (error.code === 'ECONNABORTED') return 'The request timed out. Please try again.'
    if (!error.response) return 'Cannot reach the hospital API. Check your connection and try again.'
    return `Request failed (${error.response.status}).`
  }
  if (error instanceof Error) return error.message
  return 'Something went wrong. Please try again.'
}

export function unwrapObject<T>(data: unknown): T {
  const value = data as { data?: unknown } | null
  if (value && typeof value === 'object' && 'data' in value && value.data) {
    return value.data as T
  }
  return data as T
}

export function unwrapList<T>(data: unknown): T[] {
  if (Array.isArray(data)) return data as T[]
  if (!data || typeof data !== 'object') return []
  const obj = data as Record<string, unknown>
  const directKeys = ['data', 'content', 'items', 'results', 'records', 'rows', 'values', 'entries']
  for (const key of directKeys) {
    if (Array.isArray(obj[key])) return obj[key] as T[]
  }
  const embedded = obj._embedded
  if (embedded && typeof embedded === 'object') {
    for (const value of Object.values(embedded as Record<string, unknown>)) {
      if (Array.isArray(value)) return value as T[]
    }
  }
  return []
}

export default api
