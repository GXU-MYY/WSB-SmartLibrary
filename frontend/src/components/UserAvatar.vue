<script setup lang="ts">
import { computed } from 'vue'

import { initialsFromName, resolvePictureUrl } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    src?: string
    name?: string
    size?: number
  }>(),
  {
    src: '',
    name: '',
    size: 40,
  },
)

const avatarStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
}))
</script>

<template>
  <div class="avatar" :style="avatarStyle" :aria-label="name || '用户头像'">
    <img v-if="src" :src="resolvePictureUrl(src)" :alt="name || 'avatar'" loading="lazy" />
    <span v-else>{{ initialsFromName(name) }}</span>
  </div>
</template>

<style scoped>
.avatar {
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(31, 95, 107, 0.18), rgba(201, 119, 46, 0.2));
  color: var(--sl-brand-strong);
  font-weight: 700;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
