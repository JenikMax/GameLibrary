import { ref, watch } from 'vue'

let initialDarkMode = false
try {
  initialDarkMode = localStorage.getItem('darkMode') === 'true'
} catch {
  initialDarkMode = false
}
const isDarkMode = ref(initialDarkMode)

export function useDarkMode() {
  function toggleDarkMode() {
    isDarkMode.value = !isDarkMode.value
    try { localStorage.setItem('darkMode', isDarkMode.value) } catch {}
    updateClass()
  }

  function updateClass() {
    if (isDarkMode.value) {
      document.documentElement.classList.add('app-dark')
    } else {
      document.documentElement.classList.remove('app-dark')
    }
  }

  watch(isDarkMode, updateClass, { immediate: true })

  return { isDarkMode, toggleDarkMode }
}
