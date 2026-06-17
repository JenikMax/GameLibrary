import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLocaleStore = defineStore('locale', () => {
  function detectBrowserLocale() {
    const lang = navigator.language || ''
    return lang.startsWith('ru') ? 'ru' : 'en'
  }

  const locale = ref(localStorage.getItem('locale') || detectBrowserLocale())

  function setLocale(lang) {
    locale.value = lang
    localStorage.setItem('locale', lang)
  }

  return { locale, setLocale, detectBrowserLocale }
})
