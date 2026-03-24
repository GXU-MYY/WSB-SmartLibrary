export type ThemeMode = 'light' | 'dark'

export const THEME_STORAGE_KEY = 'WSB_SMARTLIB_THEME'

const isThemeMode = (value: unknown): value is ThemeMode => value === 'light' || value === 'dark'

export const getStoredTheme = () => {
  if (typeof window === 'undefined') {
    return null
  }

  const value = window.localStorage.getItem(THEME_STORAGE_KEY)
  return isThemeMode(value) ? value : null
}

export const resolvePreferredTheme = (): ThemeMode => {
  const stored = getStoredTheme()
  if (stored) {
    return stored
  }

  if (typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark'
  }

  return 'light'
}

export const applyTheme = (mode: ThemeMode) => {
  if (typeof document === 'undefined') {
    return
  }

  document.documentElement.dataset.theme = mode
  document.documentElement.style.colorScheme = mode
}

export const persistTheme = (mode: ThemeMode) => {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(THEME_STORAGE_KEY, mode)
}
