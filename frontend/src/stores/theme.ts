import { defineStore } from 'pinia'

import { applyTheme, persistTheme, resolvePreferredTheme, type ThemeMode } from '@/utils/theme'

export const useThemeStore = defineStore('theme', {
  state: () => ({
    mode: 'light' as ThemeMode,
    initialized: false,
  }),
  getters: {
    isDark: (state) => state.mode === 'dark',
  },
  actions: {
    init() {
      const mode = resolvePreferredTheme()
      this.mode = mode
      this.initialized = true
      applyTheme(mode)
    },
    setMode(mode: ThemeMode) {
      this.mode = mode
      this.initialized = true
      persistTheme(mode)
      applyTheme(mode)
    },
    toggleMode() {
      this.setMode(this.isDark ? 'light' : 'dark')
    },
  },
})
