import api from './axios'

export const profileApi = {
  getProfile() {
    return api.get('/profile')
  },
  updateProfile(data) {
    return api.put('/profile', data)
  },
  changePassword(data) {
    return api.post('/profile/pass', data)
  }
}
