// API-методы для получения статистики библиотеки
import api from './axios'

export const statisticsApi = {
  // Получение агрегированной статистики (количество игр, размер, рейтинги, графики)
  get() {
    return api.get('/statistics')
  },
  // Сброс кэша размеров игр для пересчёта (ADMIN only)
  refreshSizes() {
    return api.post('/statistics/refresh-sizes')
  }
}
