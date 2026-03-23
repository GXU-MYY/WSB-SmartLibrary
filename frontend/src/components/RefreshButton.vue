<script setup lang="ts">
import { computed } from 'vue'

import { usePageRefreshController } from '@/composables/usePageRefresh'

const { hasRefreshHandler, refreshing, triggerRefresh } = usePageRefreshController()

const buttonLabel = computed(() => (refreshing.value ? '正在刷新当前模块' : '刷新当前模块'))
</script>

<template>
  <button
    class="button button--ghost refresh-button"
    :class="{ 'is-refreshing': refreshing }"
    type="button"
    :title="buttonLabel"
    :aria-label="buttonLabel"
    :disabled="!hasRefreshHandler || refreshing"
    @click="triggerRefresh"
  >
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M20 11a8 8 0 0 0-14.7-4.1"
        fill="none"
        stroke="currentColor"
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2.3"
      />
      <path
        d="M5 3.8v4.8h4.8"
        fill="none"
        stroke="currentColor"
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2.3"
      />
      <path
        d="M4 13a8 8 0 0 0 14.7 4.1"
        fill="none"
        stroke="currentColor"
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2.3"
      />
      <path
        d="M19 20.2v-4.8h-4.8"
        fill="none"
        stroke="currentColor"
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2.3"
      />
    </svg>
  </button>
</template>

<style scoped>
.refresh-button {
  min-width: 40px;
  min-height: 40px;
  padding: 0;
}

.refresh-button svg {
  width: 19px;
  height: 19px;
}

.refresh-button.is-refreshing svg {
  animation: refresh-spin 0.9s linear infinite;
}

@keyframes refresh-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
