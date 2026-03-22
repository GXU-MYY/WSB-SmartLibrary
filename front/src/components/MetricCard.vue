<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    value: string | number
    detail?: string
    tone?: 'brand' | 'accent' | 'plain'
  }>(),
  {
    detail: '',
    tone: 'plain',
  },
)
</script>

<template>
  <article class="surface-card metric-card" :class="`metric-card--${tone}`">
    <span>{{ label }}</span>
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
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.1), rgba(255, 253, 248, 0.92));
}

.metric-card--accent {
  background: linear-gradient(180deg, rgba(201, 119, 46, 0.12), rgba(255, 253, 248, 0.92));
}
</style>
