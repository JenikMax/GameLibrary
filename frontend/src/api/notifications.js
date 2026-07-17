import api from './axios'

export const notificationsApi = {
  getNotifications() {
    return api.get('/notifications', { skipToast: true })
  },
  markAsRead(id) {
    return api.put(`/notifications/${id}/read`)
  },
  markAllAsRead() {
    return api.put('/notifications/read-all')
  }
}
