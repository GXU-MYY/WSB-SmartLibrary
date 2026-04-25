import { computed, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'

type RefreshHandler = () => void | Promise<void>

const activeRefreshHandler = shallowRef<RefreshHandler | null>(null)
const refreshing = ref(false)

export const useRegisterPageRefresh = (handler: RefreshHandler) => {
  onMounted(() => {
    activeRefreshHandler.value = handler
  })

  onBeforeUnmount(() => {
    if (activeRefreshHandler.value === handler) {
      activeRefreshHandler.value = null
    }
  })
}

export const usePageRefreshController = () => {
  const hasRefreshHandler = computed(() => Boolean(activeRefreshHandler.value))

  const triggerRefresh = async () => {
    if (!activeRefreshHandler.value || refreshing.value) {
      return
    }

    refreshing.value = true

    try {
      await activeRefreshHandler.value()
    } finally {
      refreshing.value = false
    }
  }

  return {
    hasRefreshHandler,
    refreshing,
    triggerRefresh,
  }
}
