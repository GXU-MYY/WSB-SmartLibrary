<script setup lang="ts">
withDefaults(
  defineProps<{
    compact?: boolean
  }>(),
  {
    compact: false,
  },
)
</script>

<template>
  <div class="logo" :class="{ 'logo--compact': compact }">
    <div class="logo__mark" aria-hidden="true">
      <span />
      <span />
      <span />
    </div>

    <div class="logo__copy">
      <strong>微书包</strong>
      <span>SmartLibrary Workspace</span>
    </div>
  </div>
</template>

<style scoped>
.logo {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}

.logo__mark {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  width: 48px;
  height: 48px;
  padding: 6px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--sl-brand-strong), var(--sl-brand));
  box-shadow: 0 18px 30px rgba(31, 95, 107, 0.24);
}

.logo__mark span {
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.48));
}

.logo__copy {
  display: grid;
  gap: 2px;
}

.logo__copy strong {
  font-size: 1.05rem;
}

.logo__copy span {
  color: var(--sl-ink-soft);
  font-size: 0.82rem;
}

.logo--compact .logo__copy span {
  display: none;
}
</style>
