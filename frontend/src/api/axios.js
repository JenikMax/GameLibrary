import axios from 'axios'

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

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      const url = error.config.url || ''
      if (!url.includes('/auth/login') && !url.includes('/auth/register')) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/game-library/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
