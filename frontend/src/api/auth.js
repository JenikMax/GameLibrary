import api from './axios'

export const authApi = {
  login(username, password) {
    return api.post('/auth/login', { username, password })
  },
  register(username, password) {
    return api.post('/auth/register', { username, password })
  },
  me() {
    return api.get('/auth/me')
  }
}
