<!-- Страница входа. Поддерживает две темы оформления: стандартную (PrimeVue) и ретро-терминальную (RetroCrtDisplay). Форма с username/password, обработка ошибок с таймаутом очистки. -->
<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <!-- Терминальная тема: SVG-монитор с приглашением -->
        <template v-if="isTerminalTheme">
          <RetroCrtDisplay :screen-lines="crtLines" v-bind="terminalPalette" prompt="root@glib:~$ login --user" />
        </template>
        <!-- Обычная тема: логотип и заголовок -->
        <template v-else>
          <img :src="'/game-library/img/logo.jpg'" height="56" alt="logo" class="auth-logo" />
          <h2 class="auth-title">{{ t('login.signin') }}</h2>
          <p class="auth-sub">{{ t('login.subtitle') }}</p>
        </template>
      </div>
      <Message v-if="error" severity="error" :closable="false" class="auth-msg mb-3">{{ error }}</Message>
      <form @submit.prevent="handleLogin">
        <div class="field">
          <label for="login-username" class="field-label">{{ isTerminalTheme ? 'username:' : t('login.username') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-user" /></InputIcon>
            <InputText id="login-username" v-model="username" class="w-full auth-input" :placeholder="isTerminalTheme ? 'enter_username' : ''" autofocus />
          </IconField>
        </div>
        <div class="field">
          <label for="login-password" class="field-label">{{ isTerminalTheme ? 'password:' : t('login.password') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-lock" /></InputIcon>
            <Password id="login-password" v-model="password" :feedback="false" class="w-full auth-password" inputClass="w-full auth-input" :inputClass="'w-full auth-input'" toggleMask :placeholder="isTerminalTheme ? '••••••••' : ''" />
          </IconField>
        </div>
        <Button type="submit" :label="isTerminalTheme ? '$ execute login' : t('login.signin')" :icon="isTerminalTheme ? undefined : 'pi pi-sign-in'" class="w-full mt-2 auth-btn" :loading="loading" />
      </form>
      <div class="auth-footer">
        <span class="footer-text">{{ isTerminalTheme ? 'no account?' : t('login.no_account') }}</span>
        <Button :label="isTerminalTheme ? 'run register.sh' : t('login.register')" link class="footer-link" @click="$router.push('/register')" />
      </div>
    </div>
  </div>
</template>

<script setup>
// Страница входа: аутентификация через AuthStore, поддержка CRT-темы, авто-очистка ошибки через 7с
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import { useTheme } from '../composables/useTheme'
import RetroCrtDisplay from '../components/RetroCrtDisplay.vue'
import InputText from 'primevue/inputtext'
import InputIcon from 'primevue/inputicon'
import IconField from 'primevue/iconfield'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()
const { currentThemeId, isTerminalTheme } = useTheme()

// Палитра для ретро-терминала в зависимости от темы
const terminalPalette = computed(() => {
  if (currentThemeId.value === 'yellowed-crt') {
    return { primary: '#ccaa33', muted: '#aa8833', bg: '#2a1e00', bgInner: '#1a1000' }
  }
  return { primary: '#00ff88', muted: '#00cc6a', bg: '#003d22', bgInner: '#002a18' }
})
// Строки для SVG-дисплея ретро-монитора
const crtLines = computed(() => [
  { text: 'GAME-LIBRARY', y: 50, size: 7, color: terminalPalette.value.muted, opacity: 0.8 },
  { text: 'AUTH v2.4.1', y: 64, size: 6, opacity: 0.6 },
  { text: '', y: 78, size: 6 },
  { text: 'login --user', y: 92, size: 6, opacity: 0.5 },
  { text: '', y: 106, size: 6 },
  { text: 'enter username...', y: 120, size: 6, color: terminalPalette.value.muted, opacity: 0.9 },
])

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
let errorTimer = null // Таймер авто-очистки сообщения об ошибке

onBeforeUnmount(() => {
  if (errorTimer) clearTimeout(errorTimer)
})

// Обработчик входа: валидация, вызов AuthStore, редирект на главную
async function handleLogin() {
  if (!username.value || !password.value) {
    error.value = t('login.fill_fields')
    return
  }
  loading.value = true
  error.value = ''
  try {
    await authStore.login(username.value, password.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || t('login.invalid')
    errorTimer = setTimeout(() => { error.value = '' }, 7000)
  } finally {
    loading.value = false
  }
}
</script>

<style>
@import '../assets/styles/auth.css';
</style>

<style scoped>
</style>
