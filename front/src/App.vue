<script setup lang="ts">
import ToastStack from '@/components/ToastStack.vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

if (!themeStore.initialized) {
  themeStore.init()
}
</script>

<template>
  <router-view />
  <ToastStack />
</template>
