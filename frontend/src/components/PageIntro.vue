<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description: string
}>()
</script>

<template>
  <section v-if="$slots.actions" class="page-intro">
    <div class="page-intro__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<style scoped>
.page-intro {
  display: flex;
  justify-content: flex-end;
  gap: 20px;
  padding: 0;
}

.page-intro__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 900px) {
  .page-intro {
    justify-content: flex-start;
  }

  .page-intro__actions {
    justify-content: flex-start;
  }
}
</style>
