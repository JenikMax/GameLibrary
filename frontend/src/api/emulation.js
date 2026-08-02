// API-методы для браузерной эмуляции: информация о ROM, сейвы, запуск игрока
import api from './axios'

export const emulationApi = {
  // Информация об эмуляции игры: supported, system, core, список ROM-файлов
  getEmulation(id) {
    return api.get(`/games/${id}/emulation`, { skipToast: true })
  },
  // Список сейвов текущего пользователя
  getSaveList(id) {
    return api.get(`/games/${id}/save-list`, { skipToast: true })
  },
  // Прямая ссылка на страницу игрока EmulatorJS
  getPlayerUrl(id, filePath) {
    return `/game-library/emu/player.html?game=${id}&file=${encodeURIComponent(filePath)}`
  }
}
