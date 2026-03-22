<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section class="surface-card section-panel">
    <header class="section-panel__head">
      <div>
        <h2>{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>

      <div v-if="$slots.actions" class="section-panel__actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="section-panel__body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.section-panel {
  display: grid;
  gap: 18px;
  padding: 22px;
}

.section-panel__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.section-panel__head h2,
.section-panel__head p {
  margin: 0;
}

.section-panel__head h2 {
  font-size: 1.4rem;
}

.section-panel__head p {
  margin-top: 8px;
  color: var(--sl-ink-soft);
  line-height: 1.7;
}

.section-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.section-panel__body {
  min-width: 0;
}

@media (max-width: 768px) {
  .section-panel__head {
    flex-direction: column;
  }
}
</style>
