<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    description?: string
    rows?: number
  }>(),
  {
    title: '内容正在整理中',
    description: '请稍候，我们正在把这部分数据铺到工作台上。',
    rows: 3,
  },
)
</script>

<template>
  <section class="surface-card loading-state">
    <div class="loading-state__copy">
      <span class="eyebrow">Loading</span>
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
    </div>

    <div class="loading-state__blocks">
      <div class="skeleton loading-state__cover" />
      <div class="skeleton loading-state__line loading-state__line--title" />
      <div
        v-for="index in rows"
        :key="index"
        class="skeleton loading-state__line"
      />
    </div>
  </section>
</template>

<style scoped>
.loading-state {
  display: grid;
  gap: 16px;
  padding: 22px;
}

.loading-state__copy {
  display: grid;
  gap: 8px;
}

.loading-state__copy h3,
.loading-state__copy p {
  margin: 0;
}

.loading-state__copy p {
  color: var(--sl-ink-soft);
  line-height: 1.7;
}

.loading-state__blocks {
  display: grid;
  gap: 10px;
}

.loading-state__cover {
  height: 160px;
  border-radius: 18px;
}

.loading-state__line {
  height: 12px;
  border-radius: 999px;
}

.loading-state__line--title {
  width: 44%;
}
</style>
