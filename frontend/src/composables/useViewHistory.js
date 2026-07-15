import { ref } from 'vue'

const STORAGE_KEY = 'gameViewHistory'
const MAX_ITEMS = 20

const history = ref(loadHistory())

function loadHistory() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
  } catch {
    return []
  }
}

function saveHistory() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(history.value))
}

export function useViewHistory() {
  function addToHistory(game) {
    history.value = history.value.filter(g => g.id !== game.id)
    history.value.unshift({
      id: game.id,
      name: game.name,
      logoUrl: game.logoUrl || null,
      logo: game.logo || null,
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
