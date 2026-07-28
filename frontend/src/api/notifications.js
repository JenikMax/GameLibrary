// API-методы для работы с уведомлениями: получение, отметка о прочтении
import api from './axios'

export const notificationsApi = {
  // Получение списка уведомлений (без тост-уведомления об ошибке)
  getNotifications() {
    return api.get('/notifications', { skipToast: true })
  },
  // Отметить одно уведомление как прочитанное
  markAsRead(id) {
    return api.put(`/notifications/${id}/read`)
  },
  // Отметить все уведомления как прочитанные
  markAllAsRead() {
    return api.put('/notifications/read-all')
  }
}
