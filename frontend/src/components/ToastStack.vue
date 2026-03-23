<script setup lang="ts">
import { useNotifications } from '@/utils/notify'

const notifications = useNotifications()
</script>

<template>
  <div class="toast-stack" aria-live="polite" aria-atomic="true">
    <transition-group name="toast">
      <article
        v-for="item in notifications.items"
        :key="item.id"
        class="toast surface-card"
        :class="`toast--${item.kind}`"
      >
        <strong>{{ item.title }}</strong>
        <p v-if="item.message">{{ item.message }}</p>
      </article>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 40;
  display: grid;
  gap: 12px;
  width: min(360px, calc(100vw - 32px));
}

.toast {
  padding: 16px 18px;
  border-radius: 20px;
}

.toast strong,
.toast p {
  margin: 0;
}

.toast p {
  margin-top: 6px;
  color: var(--sl-ink-soft);
  line-height: 1.6;
}

.toast--success {
  border-color: rgba(47, 125, 99, 0.22);
}

.toast--error {
  border-color: rgba(179, 76, 65, 0.22);
}

.toast-enter-active,
.toast-leave-active {
  transition: all 180ms ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
