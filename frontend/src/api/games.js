// API-методы для работы с играми: CRUD, поиск, скрапинг, рейтинги, комментарии, обзоры, теги, перевод
import api from './axios'

export const gamesApi = {
  // Получение списка игр с фильтрацией, пагинацией и сортировкой
  getGames(params) {
    return api.get('/games', { params })
  },
  // Получение детальной информации об игре
  getGame(id) {
    return api.get(`/games/${id}`)
  },
  // Получение опций фильтрации (платформы, жанры, годы, теги)
  getFilterOptions() {
    return api.get('/games/filter-options', { skipToast: true })
  },
  // Редактирование игры
  editGame(id, data) {
    return api.post(`/games/${id}/edit`, data)
  },
  // Скрапинг данных игры с внешнего источника
  grabGame(id, data) {
    return api.post(`/games/${id}/grab`, data)
  },
  // Информация о доступных загрузках игры
  getDownloadInfo(id) {
    return api.get(`/games/${id}/download-info`)
  },
  // Прямой URL для скачивания игры
  getDownloadUrl(id) {
    return `/game-library/api/games/${id}/download`
  },
  // Список доступных скраперов
  getScrapers() {
    return api.get('/games/scrapers')
  },
  // Случайная игра
  getRandomGame() {
    return api.get('/games/random')
  },
  // Получение рейтинга игры
  getRating(id) {
    return api.get(`/games/${id}/rating`)
  },
  // Сохранение оценки пользователя
  saveRating(id, rating) {
    return api.post(`/games/${id}/rating`, { rating })
  },
  // Проверка, добавлена ли игра в избранное
  getFavorite(id) {
    return api.get(`/games/${id}/favorite`)
  },
  // Переключение избранного
  toggleFavorite(id) {
    return api.post(`/games/${id}/favorite`)
  },
  // Список избранных игр
  getFavorites() {
    return api.get('/games/favorites')
  },
  // Комментарии к игре
  getComments(id) {
    return api.get(`/games/${id}/comments`)
  },
  addComment(id, text) {
    return api.post(`/games/${id}/comments`, { text })
  },
  deleteComment(gameId, commentId) {
    return api.delete(`/games/${gameId}/comments/${commentId}`)
  },
  // Похожие игры
  getRelated(id) {
    return api.get(`/games/${id}/related`, { skipToast: true })
  },
  // Обзоры игры
  getReviews(id) {
    return api.get(`/games/${id}/reviews`)
  },
  addReview(id, data) {
    return api.post(`/games/${id}/reviews`, data)
  },
  deleteReview(gameId, reviewId) {
    return api.delete(`/games/${gameId}/reviews/${reviewId}`)
  },
  // Авто-тегирование: предложить теги для игры
  suggestTags(id) {
    return api.post(`/games/${id}/suggest-tags`)
  },
  // Предпросмотр авто-тегов по тексту
  autoTagPreview(text) {
    return api.post('/games/auto-tag-preview', { text })
  },
  // Перевод описания игры
  translateGame(id) {
    return api.post(`/games/${id}/translate`)
  },
  // Статус задачи перевода (polling)
  translateGameStatus(id, taskId) {
    return api.get(`/games/${id}/translate/status/${taskId}`)
  },
  // Асинхронный перевод произвольного текста
  translateTextAsync(text) {
    return api.post('/games/translate-text-async', { text })
  },
  // Статус задачи перевода текста (polling)
  translateTextStatus(taskId) {
    return api.get(`/games/translate-text/status/${taskId}`)
  },
  // Перевод произвольного текста (синхронный)
  translateText(text) {
    return api.post('/games/translate-text', { text })
  },
  // AI-анализ скриншотов через CLIP
  analyzeScreenshots(id, maxScreenshots = 10) {
    return api.post(`/games/${id}/analyze-screenshots`, null, { params: { maxScreenshots } })
  },
  // Проверка доступности анализа скриншотов
  checkAnalyzeScreenshotsAvailable() {
    return api.get('/games/analyze-screenshots/available', { skipToast: true })
  }
}
