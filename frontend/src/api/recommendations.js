import api from './axios'

export const recommendationsApi = {
  checkAvailable() {
    return api.get('/recommendations/available', { skipToast: true })
  },
  getSimilar(gameId, limit = 10) {
    return api.get(`/recommendations/similar/${gameId}`, { params: { limit } })
  },
  getForYou(limit = 10) {
    return api.get('/recommendations/for-you', { params: { limit } })
  }
}
