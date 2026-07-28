// Композабл для хранения истории просмотренных игр в localStorage (макс. 20)
import { ref } from 'vue'

const STORAGE_KEY = 'gameViewHistory'
const MAX_ITEMS = 20

const history = ref(loadHistory())

// Загрузка истории из localStorage
function loadHistory() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
  } catch {
    return []
  }
}

// Сохранение с обработкой ошибок превышения квоты localStorage
function saveHistory() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(history.value))
  } catch {
    // При превышении квоты — урезаем до 5
    history.value = history.value.slice(0, 5)
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(history.value))
    } catch {
      // Если и 5 не влазит — очищаем полностью
      history.value = []
      localStorage.removeItem(STORAGE_KEY)
    }
  }
}

export function useViewHistory() {
  // Добавление игры в историю (удаляем дубликат, вставляем в начало)
  function addToHistory(game) {
    history.value = history.value.filter(g => g.id !== game.id)
    history.value.unshift({
      id: game.id,
      name: game.name,
      logoUrl: game.logoUrl || null,
      platform: game.platform || null,
      timestamp: Date.now()
    })
    if (history.value.length > MAX_ITEMS) {
      history.value = history.value.slice(0, MAX_ITEMS)
    }
    saveHistory()
  }

  function clearHistory() {
    history.value = []
    localStorage.removeItem(STORAGE_KEY)
  }

  return { history, addToHistory, clearHistory }
}
