import type { AuthSnapshot } from '@/types/models'

export const AUTH_STORAGE_KEY = 'WSB_SMARTLIB_AUTH'
export const DEFAULT_TOKEN_NAME = 'satoken'

export const getAuthSnapshot = () => {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthSnapshot
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export const setAuthSnapshot = (snapshot: AuthSnapshot) => {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(snapshot))
}

export const clearAuthSnapshot = () => {
  localStorage.removeItem(AUTH_STORAGE_KEY)
}

export const isAuthenticated = () => Boolean(getAuthSnapshot()?.tokenValue)
