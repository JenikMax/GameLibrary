// Композабл для подписки на push-уведомления через SSE с fallback polling каждые 15с
import { ref, onUnmounted } from 'vue'
import { notificationsApi } from '../api/notifications'

export function useNotifications() {
  const notifications = ref([])
  const unreadCount = ref(0)
  let eventSource = null
  let fallbackTimer = null
  let isUnmounted = false
  let authToken = null

  // Подписка через Server-Sent Events (SSE)
  function subscribe(token) {
    authToken = token
    if (!token) return

    const url = `/game-library/api/notifications/subscribe?token=${encodeURIComponent(token)}`
    eventSource = new EventSource(url)

    // Обработка входящего уведомления
    eventSource.addEventListener('notification', (e) => {
      try {
        const notif = JSON.parse(e.data)
        notifications.value.unshift(notif)
        if (!notif.read) unreadCount.value++
      } catch {}
    })

    // При успешном подключении отключаем fallback polling
    eventSource.addEventListener('connected', () => {
      if (fallbackTimer) {
        clearInterval(fallbackTimer)
        fallbackTimer = null
      }
    })

    // При ошибке SSE переключаемся на polling
    eventSource.onerror = () => {
      eventSource?.close()
      eventSource = null
      startFallbackPolling()
    }
  }

  // Fallback polling каждые 15 секунд при недоступности SSE
  function startFallbackPolling() {
    if (fallbackTimer || !authToken) return
    fallbackTimer = setInterval(() => {
      if (!isUnmounted) fetchNotifications()
    }, 15000)
  }

  // Принудительная загрузка уведомлений через REST API
  async function fetchNotifications() {
    try {
      const res = await notificationsApi.getNotifications()
      notifications.value = res.data.data.items || []
      unreadCount.value = res.data.data.unread || 0
    } catch {}
  }

  // Отписка: закрытие SSE и остановка polling
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
