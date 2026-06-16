import api from './axios'

export const adminApi = {
  getUsers() {
    return api.get('/admin/users')
  },
  toggleAdmin(id, isAdmin) {
    return api.post(`/admin/users/${id}/toggle-admin`, null, { params: { isAdmin } })
  },
  toggleActive(id, isActive) {
    return api.post(`/admin/users/${id}/toggle-active`, null, { params: { isActive } })
  },
  resetPassword(id) {
    return api.post(`/admin/users/${id}/reset-pass`)
  },
  scanLibrary() {
    return api.post('/scan')
  },
  migrateImages() {
    return api.post('/admin/migrate-images')
  }
}
