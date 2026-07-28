import { ref, onUnmounted } from 'vue'
import { notificationsApi } from '../api/notifications'

export function useNotifications() {
  const notifications = ref([])
  const unreadCount = ref(0)
  let eventSource = null
  let fallbackTimer = null
  let isUnmounted = false
  let authToken = null

  function subscribe(token) {
    authToken = token
    if (!token) return

    const url = `/game-library/api/notifications/subscribe?token=${encodeURIComponent(token)}`
    eventSource = new EventSource(url)

    eventSource.addEventListener('notification', (e) => {
      try {
        const notif = JSON.parse(e.data)
        notifications.value.unshift(notif)
        if (!notif.read) unreadCount.value++
      } catch {}
    })

    eventSource.addEventListener('connected', () => {
      if (fallbackTimer) {
        clearInterval(fallbackTimer)
        fallbackTimer = null
      }
    })

    eventSource.onerror = () => {
      eventSource?.close()
      eventSource = null
      startFallbackPolling()
    }
  }

  function startFallbackPolling() {
    if (fallbackTimer || !authToken) return
    fallbackTimer = setInterval(() => {
      if (!isUnmounted) fetchNotifications()
    }, 15000)
  }

  async function fetchNotifications() {
    try {
      const res = await notificationsApi.getNotifications()
      notifications.value = res.data.data.items || []
      unreadCount.value = res.data.data.unread || 0
    } catch {}
  }

  function unsubscribe() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    if (fallbackTimer) {
      clearInterval(fallbackTimer)
      fallbackTimer = null
    }
  }

  onUnmounted(() => {
    isUnmounted = true
    unsubscribe()
  })

  return { notifications, unreadCount, subscribe, unsubscribe, fetchNotifications }
}
