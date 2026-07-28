// Pinia- store для управления языком интерфейса (RU/EN)
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLocaleStore = defineStore('locale', () => {
  // Определение языка браузера по настройкам ОС
  function detectBrowserLocale() {
    const lang = navigator.language || ''
    return lang.startsWith('ru') ? 'ru' : 'en'
  }

  // Инициализация из localStorage или авто-определение
  const locale = ref(localStorage.getItem('locale') || detectBrowserLocale())

  function setLocale(lang) {
    locale.value = lang
    localStorage.setItem('locale', lang)
  }

  return { locale, setLocale, detectBrowserLocale }
})
