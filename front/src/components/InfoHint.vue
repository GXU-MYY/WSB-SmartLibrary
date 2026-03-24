<script setup lang="ts">
defineProps<{
  content: string
}>()
</script>

<template>
  <span class="info-hint">
    <button class="info-hint__trigger" type="button" :aria-label="content">
      <svg viewBox="0 0 20 20" aria-hidden="true">
        <circle cx="10" cy="10" r="7.25" fill="none" stroke="currentColor" stroke-width="1.5" />
        <path
          d="M10 8.2v4.1"
          fill="none"
          stroke="currentColor"
          stroke-linecap="round"
          stroke-width="1.7"
        />
        <circle cx="10" cy="5.7" r="0.9" fill="currentColor" />
      </svg>
    </button>

    <span class="info-hint__bubble" role="tooltip">{{ content }}</span>
  </span>
</template>

<style scoped>
.info-hint {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.info-hint__trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: 999px;
  color: var(--sl-ink-soft);
  background: transparent;
  cursor: help;
  transition: color 180ms ease, background-color 180ms ease;
}

.info-hint__trigger:hover,
.info-hint__trigger:focus-visible {
  color: var(--sl-brand-strong);
  background: rgba(31, 95, 107, 0.08);
  outline: none;
}

.info-hint__trigger svg {
  width: 16px;
  height: 16px;
}

.info-hint__bubble {
  position: absolute;
  top: calc(100% + 10px);
  left: 50%;
  z-index: 15;
  width: min(240px, 42vw);
  padding: 10px 12px;
  border: 1px solid var(--sl-line);
  border-radius: 14px;
  color: var(--sl-ink);
  background: linear-gradient(180deg, var(--sl-surface-start), var(--sl-surface-end));
  box-shadow: var(--sl-shadow);
  line-height: 1.6;
  font-size: 0.82rem;
  text-transform: none;
  letter-spacing: normal;
  opacity: 0;
  pointer-events: none;
  transform: translateX(-50%) translateY(-4px);
  transition: opacity 180ms ease, transform 180ms ease;
}

.info-hint__bubble::before {
  content: '';
  position: absolute;
  bottom: 100%;
  left: 50%;
  width: 10px;
  height: 10px;
  border-top: 1px solid var(--sl-line);
  border-left: 1px solid var(--sl-line);
  background: var(--sl-paper-strong);
  transform: translateX(-50%) rotate(45deg);
}

.info-hint:hover .info-hint__bubble,
.info-hint:focus-within .info-hint__bubble {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

@media (max-width: 720px) {
  .info-hint__bubble {
    left: auto;
    right: 0;
    width: min(220px, calc(100vw - 48px));
    transform: translateY(-4px);
  }

  .info-hint__bubble::before {
    left: auto;
    right: 10px;
    transform: rotate(45deg);
  }

  .info-hint:hover .info-hint__bubble,
  .info-hint:focus-within .info-hint__bubble {
    transform: translateY(0);
  }
}
</style>
