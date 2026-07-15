import api from './axios'

export const gamesApi = {
  getGames(params) {
    return api.get('/games', { params })
  },
  getGame(id) {
    return api.get(`/games/${id}`)
  },
  getFilterOptions() {
    return api.get('/games/filter-options')
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
  }
}
