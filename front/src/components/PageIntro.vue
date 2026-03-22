<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description: string
}>()
</script>

<template>
  <section class="surface-card page-intro">
    <div class="page-intro__copy">
      <span class="eyebrow">{{ eyebrow }}</span>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
    </div>

    <div v-if="$slots.actions" class="page-intro__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<style scoped>
.page-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 28px;
}

.page-intro__copy {
  display: grid;
  gap: 12px;
}

.page-intro__copy h1,
.page-intro__copy p {
  margin: 0;
}

.page-intro__copy h1 {
  font-size: clamp(2.1rem, 4vw, 3.6rem);
  line-height: 0.96;
  max-width: 12ch;
}

.page-intro__copy p {
  max-width: 62ch;
  color: var(--sl-ink-soft);
  line-height: 1.8;
}

.page-intro__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 900px) {
  .page-intro {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
