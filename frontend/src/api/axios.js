import axios from 'axios'
import { useLocaleStore } from '../stores/locale'

const api = axios.create({
  baseURL: '/game-library/api',
  headers: {
    'Content-Type': 'application/json'
  },
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

const MAX_RETRIES = 1
const RETRY_DELAY = 1000

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

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config
    if (!config) return Promise.reject(error)

    const status = error.response?.status
    const isGet = config.method === 'get' || config.method === 'GET'
    const isNetworkError = !error.response
    const isServerOrNetworkError = status >= 500 || isNetworkError

    if (isServerOrNetworkError && isGet && config._retryCount < MAX_RETRIES) {
      config._retryCount++
      await new Promise(r => setTimeout(r, RETRY_DELAY))
      return api(config)
    }

    if (status === 401) {
      const url = config.url || ''
      if (!url.includes('/auth/login') && !url.includes('/auth/register')) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/game-library/login'
      }
    } else if (config.skipToast !== true) {
      const message = getErrorMessage(error)
      window.dispatchEvent(new CustomEvent('api-error', { detail: message }))
    }

    return Promise.reject(error)
  }
)

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
