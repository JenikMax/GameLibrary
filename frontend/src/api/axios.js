// Axios-инстанс для API-запросов к бэкенду
// Содержит перехватчики для JWT-авторизации, локали, повторных попыток и обработки ошибок
import axios from 'axios'
import { useLocaleStore } from '../stores/locale'

const api = axios.create({
  baseURL: '/game-library/api',
  headers: {
    'Content-Type': 'application/json'
  },
  // Сериализация параметров: массивы преобразуются в повторяющиеся ключи
  paramsSerializer: (params) => {
    const searchParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (Array.isArray(value)) {
        value.forEach(v => searchParams.append(key, v))
      } else if (value !== undefined && value !== null) {
        searchParams.append(key, value)
      }
    })
    return searchParams.toString()
  }
})

// Настройки повторных попыток при ошибках сервера/сети
const MAX_RETRIES = 1
const RETRY_DELAY = 1000

let isRefreshing = false
let failedQueue = []

function processQueue(error, token = null) {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

// Перехватчик запросов: добавляет JWT-токен и язык
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const localeStore = useLocaleStore()
  if (config.params) {
    config.params.lang = localeStore.locale
  } else {
    config.params = { lang: localeStore.locale }
  }
  config._retryCount = config._retryCount || 0
  return config
})

// Перехватчик ответов: обработка ошибок, авторетрян, редирект при 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config
    if (!config) return Promise.reject(error)

    const status = error.response?.status
    const url = config.url || ''
    const isAuthUrl = url.includes('/auth/login') || url.includes('/auth/register') || url.includes('/auth/refresh')

    // При 401 (кроме auth-эндпоинтов) — пробуем refresh
    if (status === 401 && !isAuthUrl && !config._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(token => {
          config.headers.Authorization = `Bearer ${token}`
          return api(config)
        }).catch(err => Promise.reject(err))
      }

      isRefreshing = true
      config._retry = true

      try {
        const { useAuthStore } = await import('../stores/auth.js')
        const authStore = useAuthStore()
        const refreshed = await authStore.refreshAccessToken()
        if (refreshed) {
          const newToken = localStorage.getItem('token')
          processQueue(null, newToken)
          config.headers.Authorization = `Bearer ${newToken}`
          return api(config)
        }
        processQueue(new Error('refresh failed'))
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/game-library/login'
        return Promise.reject(error)
      } catch (refreshError) {
        processQueue(refreshError)
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/game-library/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    const isGet = config.method === 'get' || config.method === 'GET'
    const isNetworkError = !error.response
    const isServerOrNetworkError = status >= 500 || isNetworkError

    // Повтор при 5xx или сетевой ошибке для GET-запросов
    if (isServerOrNetworkError && isGet && config._retryCount < MAX_RETRIES) {
      config._retryCount++
      await new Promise(r => setTimeout(r, RETRY_DELAY))
      return api(config)
    }

    if (config.skipToast !== true && status !== 401) {
      const message = getErrorMessage(error)
      window.dispatchEvent(new CustomEvent('api-error', { detail: message }))
    }

    return Promise.reject(error)
  }
)

// Формирование человекочитаемого сообщения об ошибке по статусу
function getErrorMessage(error) {
  const status = error.response?.status
  if (status === 403) return 'Доступ запрещён'
  if (status === 404) return 'Ресурс не найден'
  if (status === 429) return 'Слишком много запросов, попробуйте позже'
  if (status === 413) return 'Файл слишком большой'
  if (status >= 500) return 'Ошибка сервера. Попробуйте позже'
  if (!error.response) return 'Ошибка сети. Проверьте подключение'
  return error.response?.data?.message || 'Произошла ошибка'
}

export default api
