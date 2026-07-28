// API-методы для управления коллекциями игр (плейлистами)
import api from './axios'

export const collectionsApi = {
  // Список коллекций (свои + публичные чужие)
  list() {
    return api.get('/collections')
  },
  // Коллекции с данными главной игры для превью-карточек
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
  // Игры внутри коллекции
  getGames(id) {
    return api.get(`/collections/${id}/games`)
  },
  addGame(id, gameId) {
    return api.post(`/collections/${id}/games`, { gameId })
  },
  removeGame(id, gameId) {
    return api.delete(`/collections/${id}/games/${gameId}`)
  },
  // Изменение порядка игр в коллекции
  reorder(id, order) {
    return api.put(`/collections/${id}/games/reorder`, { order })
  },
  // Проверка членства игры в коллекциях
  membership(gameId) {
    return api.get('/collections/membership', { params: { gameId } })
  }
}
