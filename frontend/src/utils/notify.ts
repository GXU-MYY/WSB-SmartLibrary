import { reactive } from 'vue'

export type NotificationKind = 'success' | 'error' | 'info'

export interface NotificationItem {
  id: string
  title: string
  message?: string
  kind: NotificationKind
}

const notificationState = reactive({
  items: [] as NotificationItem[],
})

const removeNotification = (id: string) => {
  notificationState.items = notificationState.items.filter((item) => item.id !== id)
}

export const useNotifications = () => notificationState

export const pushNotification = (
  kind: NotificationKind,
  title: string,
  message?: string,
  timeout = 3200,
) => {
  const id = `${Date.now()}-${Math.random().toString(16).slice(2)}`

  notificationState.items.push({
    id,
    kind,
    title,
    message,
  })

  window.setTimeout(() => removeNotification(id), timeout)
}

export const notifySuccess = (title: string, message?: string) =>
  pushNotification('success', title, message)

export const notifyError = (title: string, message?: string) =>
  pushNotification('error', title, message, 4400)

export const notifyInfo = (title: string, message?: string) =>
  pushNotification('info', title, message)
