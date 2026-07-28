// API-методы для управления загрузками, торрент-раздачей и подготовкой скачивания
import api from './axios'

export const downloadsApi = {
  // Запуск торрент-раздачи игры через Transmission
  seedGame(id) {
    return api.post(`/games/${id}/seed`)
  },
  getSeedStatus(taskId) {
    return api.get(`/seed/status/${taskId}`, { skipToast: true })
  },
  // Асинхронная подготовка .torrent-файла для больших игр
  prepareDownload(id) {
    return api.post(`/games/${id}/prepare-download`)
  },
  getPrepareStatus(taskId) {
    return api.get(`/download/prepare-status/${taskId}`, { skipToast: true })
  },
  // Список активных загрузок
  getActive() {
    return api.get('/downloads/active')
  },
  // Ожидающие загрузки
  getWaiting(offset = 0, num = 50) {
    return api.get('/downloads/waiting', { params: { offset, num } })
  },
  // Завершённые/остановленные загрузки
  getStopped(offset = 0, num = 50) {
    return api.get('/downloads/stopped', { params: { offset, num } })
  },
  // Статус конкретной загрузки по GID
  getStatus(gid) {
    return api.get(`/downloads/${gid}/status`)
  },
  // Управление загрузкой: удалить, приостановить, возобновить
  remove(gid) {
    return api.post(`/downloads/${gid}/remove`)
  },
  pause(gid) {
    return api.post(`/downloads/${gid}/pause`)
  },
  unpause(gid) {
    return api.post(`/downloads/${gid}/unpause`)
  },
  // Глобальная статистика загрузок
  getGlobalStat() {
    return api.get('/downloads/global-stat')
  },
  // Проверка подключения к Transmission (legacy название)
  getAria2Version() {
    return api.get('/downloads/aria2-version')
  },
  // Прямая ссылка на скачивание игры
  getDownloadUrl(id) {
    return `/game-library/api/games/${id}/download`
  }
}
