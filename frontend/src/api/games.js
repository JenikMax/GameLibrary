import api from './axios'

export const gamesApi = {
  getGames(params) {
    return api.get('/games', { params })
  },
  getGame(id) {
    return api.get(`/games/${id}`)
  },
  getFilterOptions() {
    return api.get('/games/filter-options', { skipToast: true })
  },
  editGame(id, data) {
    return api.post(`/games/${id}/edit`, data)
  },
  grabGame(id, data) {
    return api.post(`/games/${id}/grab`, data)
  },
  getDownloadInfo(id) {
    return api.get(`/games/${id}/download-info`)
  },
  getDownloadUrl(id) {
    return `/game-library/api/games/${id}/download`
  },
  getScrapers() {
    return api.get('/games/scrapers')
  },
  getRandomGame() {
    return api.get('/games/random')
  },
  getRating(id) {
    return api.get(`/games/${id}/rating`)
  },
  saveRating(id, rating) {
    return api.post(`/games/${id}/rating`, { rating })
  },
  getFavorite(id) {
    return api.get(`/games/${id}/favorite`)
  },
  toggleFavorite(id) {
    return api.post(`/games/${id}/favorite`)
  },
  getFavorites() {
    return api.get('/games/favorites')
  },
  getComments(id) {
    return api.get(`/games/${id}/comments`)
  },
  addComment(id, text) {
    return api.post(`/games/${id}/comments`, { text })
  },
  deleteComment(gameId, commentId) {
    return api.delete(`/games/${gameId}/comments/${commentId}`)
  },
  getRelated(id) {
    return api.get(`/games/${id}/related`, { skipToast: true })
  },
  getReviews(id) {
    return api.get(`/games/${id}/reviews`)
  },
  addReview(id, data) {
    return api.post(`/games/${id}/reviews`, data)
  },
  deleteReview(gameId, reviewId) {
    return api.delete(`/games/${gameId}/reviews/${reviewId}`)
  },
  suggestTags(id) {
    return api.post(`/games/${id}/suggest-tags`)
  },
  autoTagPreview(text) {
    return api.post('/games/auto-tag-preview', { text })
  },
  translateGame(id) {
    return api.post(`/games/${id}/translate`)
  }
}
