import { ref, watch } from 'vue'

const isDarkMode = ref(localStorage.getItem('darkMode') === 'true')

export function useDarkMode() {
  function toggleDarkMode() {
    isDarkMode.value = !isDarkMode.value
    localStorage.setItem('darkMode', isDarkMode.value)
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
