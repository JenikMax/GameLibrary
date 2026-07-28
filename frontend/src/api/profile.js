// API-методы для управления профилем пользователя
import api from './axios'

export const profileApi = {
  // Получение данных профиля + статистики
  getProfile() {
    return api.get('/profile')
  },
  // Обновление информации профиля (аватар, email и т.д.)
  updateProfile(data) {
    return api.put('/profile', data)
  },
  // Смена пароля
  changePassword(data) {
    return api.post('/profile/pass', data)
  }
}
