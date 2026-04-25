<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section class="surface-card empty-state">
    <div class="empty-state__icon" aria-hidden="true">
      <span />
      <span />
      <span />
    </div>

    <div class="empty-state__copy">
      <h3>{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
    </div>

    <slot />
  </section>
</template>

<style scoped>
.empty-state {
  display: grid;
  justify-items: center;
  gap: 14px;
  padding: 28px 20px;
  text-align: center;
}

.empty-state__icon {
  display: grid;
  grid-template-columns: repeat(3, 14px);
  gap: 6px;
  padding: 12px;
  border-radius: 20px;
  background: rgba(31, 95, 107, 0.08);
}

.empty-state__icon span {
  width: 14px;
  height: 40px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.28), rgba(201, 119, 46, 0.28));
}

.empty-state__copy {
  display: grid;
  gap: 8px;
}

.empty-state__copy h3,
.empty-state__copy p {
  margin: 0;
}

.empty-state__copy p {
  max-width: 34ch;
  color: var(--sl-ink-soft);
  line-height: 1.75;
}
</style>
