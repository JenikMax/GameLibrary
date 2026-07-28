// Композабл для переключения тем оформления (4 темы: светлая, тёмная, 2 ретро-терминала)
import { ref, computed, watch } from 'vue'
import { themes, themesById, DEFAULT_THEME } from '../themes'

// Чтение темы из localStorage с миграцией со старого ключа darkMode
function readThemeFromStorage() {
  try {
    const oldDark = localStorage.getItem('darkMode')
    if (oldDark !== null) {
      const migrated = oldDark === 'true' ? 'default-dark' : 'default-light'
      localStorage.setItem('theme', migrated)
      localStorage.removeItem('darkMode')
      return migrated
    }
    const stored = localStorage.getItem('theme')
    if (stored && themesById[stored]) return stored
    if (window.matchMedia?.('(prefers-color-scheme: dark)').matches) return 'default-dark'
    return DEFAULT_THEME
  } catch {
    return DEFAULT_THEME
  }
}

const currentThemeId = ref(readThemeFromStorage())

export function useTheme() {
  // Применение темы: установка data-атрибутов и colorScheme на <html>
  function applyTheme(themeId) {
    const theme = themesById[themeId]
    if (!theme) return
    document.documentElement.setAttribute('data-theme', themeId)
    document.documentElement.setAttribute('data-color-scheme', theme.colorScheme || (theme.isDark ? 'dark' : 'light'))
    document.documentElement.style.colorScheme = theme.isDark ? 'dark' : 'light'
  }

  // Смена темы с сохранением в localStorage
  function setTheme(themeId) {
    if (!themesById[themeId]) return
    currentThemeId.value = themeId
    try { localStorage.setItem('theme', themeId) } catch {}
    applyTheme(themeId)
  }

  const currentTheme = computed(() => themesById[currentThemeId.value])
  const isDark = computed(() => currentTheme.value?.isDark ?? false)
  // Проверка, является ли текущая тема терминальной (CRT-стиль)
  const isTerminalTheme = computed(() =>
    currentThemeId.value === 'retro-terminal' || currentThemeId.value === 'yellowed-crt'
  )

  watch(currentThemeId, applyTheme, { immediate: true })

  return { currentThemeId, currentTheme, isDark, isTerminalTheme, setTheme, availableThemes: themes }
}
