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
  getScanStatus(taskId) {
    return api.get(`/scan/status/${taskId}`, { skipToast: true })
  },
  getScraperConfigs() {
    return api.get('/admin/scraper-config')
  },
  getScraperConfig(type) {
    return api.get(`/admin/scraper-config/${type}`)
  },
  updateScraperConfig(type, config) {
    return api.put(`/admin/scraper-config/${type}`, config)
  },
  reloadScraperConfig() {
    return api.post('/admin/scraper-config/reload')
  },
  generateEmbeddings(force = false) {
    return api.post('/embeddings/generate', null, { params: { force } })
  },
  getEmbeddingStatus(taskId) {
    return api.get(`/embeddings/status/${taskId}`, { skipToast: true })
  }
}
