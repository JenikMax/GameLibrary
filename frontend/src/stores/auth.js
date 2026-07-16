import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const token = ref(localStorage.getItem('token') || '')

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.admin || false)
  const username = computed(() => user.value?.name || '')
  const userId = computed(() => user.value?.id || null)
  const avatarUrl = computed(() => user.value?.avatarUrl || '')

  async function login(username, password) {
    const response = await authApi.login(username, password)
    const data = response.data
    if (data.success) {
      token.value = data.data.token
      user.value = data.data.user
      localStorage.setItem('token', data.data.token)
      localStorage.setItem('user', JSON.stringify(data.data.user))
    } else {
      throw new Error(data.message || 'Login failed')
    }
  }

  async function register(username, password) {
    const response = await authApi.register(username, password)
    const data = response.data
    if (!data.success) {
      throw new Error(data.message || 'Registration failed')
    }
  }

  async function checkAuth() {
    if (!token.value) return false
    try {
      const response = await authApi.me()
      const data = response.data
      if (data.success) {
        user.value = data.data
        localStorage.setItem('user', JSON.stringify(data.data))
        return true
      }
    } catch {
      logout()
    }
    return false
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { user, token, isAuthenticated, isAdmin, username, userId, avatarUrl, login, register, checkAuth, logout }
})
