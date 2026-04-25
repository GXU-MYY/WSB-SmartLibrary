<script setup lang="ts">
import { computed } from 'vue'

import { useThemeStore } from '@/stores/theme'

withDefaults(
  defineProps<{
    compact?: boolean
    iconOnly?: boolean
  }>(),
  {
    compact: false,
    iconOnly: false,
  },
)

const themeStore = useThemeStore()

const buttonLabel = computed(() =>
  themeStore.isDark ? '切换到浅色模式' : '切换到暗色模式',
)

const currentModeLabel = computed(() => (themeStore.isDark ? '暗色' : '浅色'))
</script>

<template>
  <button
    class="button button--ghost theme-toggle"
    :class="{ 'theme-toggle--compact': compact, 'theme-toggle--icon-only': iconOnly }"
    type="button"
    :title="buttonLabel"
    :aria-label="buttonLabel"
    :aria-pressed="themeStore.isDark"
    @click="themeStore.toggleMode"
  >
    <span class="theme-toggle__icon" aria-hidden="true">
      <svg v-if="themeStore.isDark" viewBox="0 0 24 24" class="theme-toggle__svg">
        <path
          d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z"
          fill="currentColor"
        />
      </svg>
      <svg v-else viewBox="0 0 24 24" class="theme-toggle__svg">
        <circle cx="12" cy="12" r="4.5" fill="currentColor" />
        <path
          d="M12 1.75v3.1M12 19.15v3.1M1.75 12h3.1M19.15 12h3.1M4.75 4.75l2.2 2.2M17.05 17.05l2.2 2.2M19.25 4.75l-2.2 2.2M6.95 17.05l-2.2 2.2"
          fill="none"
          stroke="currentColor"
          stroke-linecap="round"
          stroke-width="1.7"
        />
      </svg>
    </span>
    <span v-if="!iconOnly" class="theme-toggle__copy">
      <strong>{{ currentModeLabel }}</strong>
      <small>{{ buttonLabel }}</small>
    </span>
  </button>
</template>

<style scoped>
.theme-toggle {
  width: 100%;
  justify-content: flex-start;
  min-height: 46px;
  padding-inline: 14px;
}

.theme-toggle__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  color: var(--sl-brand-strong);
}

.theme-toggle__svg {
  width: 18px;
  height: 18px;
}

.theme-toggle__copy {
  display: grid;
  gap: 2px;
  text-align: left;
}

.theme-toggle__copy strong {
  font-size: 0.94rem;
  font-weight: 600;
}

.theme-toggle__copy small {
  color: var(--sl-ink-soft);
  font-size: 0.78rem;
}

.theme-toggle--compact {
  width: auto;
  min-height: 40px;
  padding-inline: 12px;
}

.theme-toggle--compact .theme-toggle__copy small {
  display: none;
}

.theme-toggle--compact.theme-toggle--icon-only {
  min-width: 40px;
  padding-inline: 0;
  justify-content: center;
}
</style>
