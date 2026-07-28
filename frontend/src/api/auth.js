// API-методы аутентификации: логин, регистрация, проверка текущего пользователя
import api from './axios'

export const authApi = {
  login(username, password) {
    return api.post('/auth/login', { username, password })
  },
  register(username, password) {
    return api.post('/auth/register', { username, password })
  },
  // Получение данных текущего аутентифицированного пользователя
  me() {
    return api.get('/auth/me')
  }
}
