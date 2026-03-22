<script setup lang="ts">
import InfoHint from '@/components/InfoHint.vue'

withDefaults(
  defineProps<{
    label: string
    value: string | number
    detail?: string
    hint?: string
    tone?: 'brand' | 'accent' | 'plain'
  }>(),
  {
    detail: '',
    hint: '',
    tone: 'plain',
  },
)
</script>

<template>
  <article class="surface-card metric-card" :class="`metric-card--${tone}`">
    <div class="metric-card__head">
      <span>{{ label }}</span>
      <InfoHint v-if="hint" :content="hint" />
    </div>
    <strong>{{ value }}</strong>
    <p v-if="detail">{{ detail }}</p>
  </article>
</template>

<style scoped>
.metric-card {
  display: grid;
  gap: 10px;
  padding: 22px;
}

.metric-card__head {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.metric-card span {
  font-size: 0.86rem;
  color: var(--sl-ink-soft);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.metric-card strong {
  font-family: 'Newsreader', serif;
  font-size: 2.2rem;
  line-height: 0.95;
}

.metric-card p {
  margin: 0;
  color: var(--sl-ink-soft);
  line-height: 1.7;
}

.metric-card--brand {
  background: linear-gradient(180deg, var(--sl-metric-brand-start), var(--sl-metric-brand-end));
}

.metric-card--accent {
  background: linear-gradient(180deg, var(--sl-metric-accent-start), var(--sl-metric-accent-end));
}
</style>
