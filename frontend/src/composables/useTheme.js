import { ref, computed, watch } from 'vue'
import { themes, themesById, DEFAULT_THEME } from '../themes'

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
  function applyTheme(themeId) {
    const theme = themesById[themeId]
    if (!theme) return
    document.documentElement.setAttribute('data-theme', themeId)
    document.documentElement.setAttribute('data-color-scheme', theme.colorScheme || (theme.isDark ? 'dark' : 'light'))
    document.documentElement.style.colorScheme = theme.isDark ? 'dark' : 'light'
  }

  function setTheme(themeId) {
    if (!themesById[themeId]) return
    currentThemeId.value = themeId
    try { localStorage.setItem('theme', themeId) } catch {}
    applyTheme(themeId)
  }

  const currentTheme = computed(() => themesById[currentThemeId.value])
  const isDark = computed(() => currentTheme.value?.isDark ?? false)

  watch(currentThemeId, applyTheme, { immediate: true })

  return { currentThemeId, currentTheme, isDark, setTheme, availableThemes: themes }
}
