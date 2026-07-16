import api from './axios'

export const collectionsApi = {
  list() {
    return api.get('/collections')
  },
  listWithHero() {
    return api.get('/collections/with-hero')
  },
  get(id) {
    return api.get(`/collections/${id}`)
  },
  create(data) {
    return api.post('/collections', data)
  },
  update(id, data) {
    return api.put(`/collections/${id}`, data)
  },
  delete(id) {
    return api.delete(`/collections/${id}`)
  },
  getGames(id) {
    return api.get(`/collections/${id}/games`)
  },
  addGame(id, gameId) {
    return api.post(`/collections/${id}/games`, { gameId })
  },
  removeGame(id, gameId) {
    return api.delete(`/collections/${id}/games/${gameId}`)
  },
  reorder(id, order) {
    return api.put(`/collections/${id}/games/reorder`, { order })
  }
}
