<script setup lang="ts">
import { useNotifications } from '@/utils/notify'

const notifications = useNotifications()
</script>

<template>
  <Teleport to="body">
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
  </Teleport>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 120;
  display: grid;
  gap: 12px;
  width: min(460px, calc(100vw - 24px));
}

.toast {
  padding: 16px 18px;
  border-radius: 20px;
  max-height: min(48vh, 420px);
  overflow: auto;
}

.toast strong,
.toast p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.toast p {
  margin-top: 6px;
  color: var(--sl-ink-soft);
  line-height: 1.6;
}

@media (max-width: 640px) {
  .toast-stack {
    right: 12px;
    left: 12px;
    bottom: 12px;
    width: auto;
  }
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
