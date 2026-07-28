<!-- Страница регистрации нового пользователя. Валидация пароля (мин. 8 символов, буквы + цифры), редирект на логин через 1.5с после успеха. -->
<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <template v-if="isTerminalTheme">
          <RetroCrtDisplay :screen-lines="crtLines" v-bind="terminalPalette" prompt="root@glib:~$ register --new-player" />
        </template>
        <template v-else>
          <img :src="'/game-library/img/logo.jpg'" height="56" alt="logo" class="auth-logo" />
          <h2 class="auth-title">{{ t('register.create_account') }}</h2>
          <p class="auth-sub">{{ t('register.subtitle') }}</p>
        </template>
      </div>
      <Message v-if="error" severity="error" :closable="false" class="auth-msg mb-3">{{ error }}</Message>
      <Message v-if="success" severity="success" :closable="false" class="auth-msg mb-3">{{ success }}</Message>
      <form @submit.prevent="handleRegister">
        <div class="field">
          <label for="reg-username" class="field-label">{{ isTerminalTheme ? 'username:' : t('login.username') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-user" /></InputIcon>
            <InputText id="reg-username" v-model="username" class="w-full auth-input" :placeholder="isTerminalTheme ? 'choose_name' : ''" autofocus />
          </IconField>
        </div>
        <div class="field">
          <label for="reg-password" class="field-label">{{ isTerminalTheme ? 'password:' : t('login.password') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-lock" /></InputIcon>
            <Password id="reg-password" v-model="password" class="w-full auth-password" inputClass="w-full auth-input" toggleMask :feedback="true" :placeholder="isTerminalTheme ? '••••••••' : ''" />
          </IconField>
          <small class="field-hint">{{ isTerminalTheme ? '└─ min 8 chars, letters + digits required' : t('login.password_requirements') }}</small>
        </div>
        <Button type="submit" :label="isTerminalTheme ? '$ execute register' : t('login.register')" :icon="isTerminalTheme ? undefined : 'pi pi-user-plus'" class="w-full mt-2 auth-btn" :loading="loading" />
      </form>
      <div class="auth-footer">
        <span class="footer-text">{{ isTerminalTheme ? 'existing user?' : t('register.already_have_account') }}</span>
        <Button :label="isTerminalTheme ? 'run login.sh' : t('login.signin')" link class="footer-link" @click="$router.push('/login')" />
      </div>
    </div>
  </div>
</template>

<script setup>
// Регистрация: валидация пароля, вызов AuthStore.register, авто-редирект на /login через 1.5с
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

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()
const { currentThemeId, isTerminalTheme } = useTheme()

const terminalPalette = computed(() => {
  if (currentThemeId.value === 'yellowed-crt') {
    return { primary: '#ccaa33', muted: '#aa8833', bg: '#2a1e00', bgInner: '#1a1000' }
  }
  return { primary: '#00ff88', muted: '#00cc6a', bg: '#003d22', bgInner: '#002a18' }
})
const crtLines = computed(() => [
  { text: 'GAME-LIBRARY', y: 50, size: 7, color: terminalPalette.value.muted, opacity: 0.8 },
  { text: 'AUTH v2.4.1', y: 64, size: 6, opacity: 0.6 },
  { text: '', y: 78, size: 6 },
  { text: 'register --new-player', y: 92, size: 6, opacity: 0.5 },
  { text: '', y: 106, size: 6 },
  { text: 'choose credentials...', y: 120, size: 6, color: terminalPalette.value.muted, opacity: 0.9 },
])

const username = ref('')
const password = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)
let redirectTimer = null

onBeforeUnmount(() => {
  if (redirectTimer) clearTimeout(redirectTimer)
})

// Обработчик регистрации: валидация пароля, вызов API, редирект
async function handleRegister() {
  if (!username.value || !password.value) {
    error.value = t('login.fill_fields')
    return
  }
  if (password.value.length < 8 || !/[A-Za-z]/.test(password.value) || !/\d/.test(password.value)) {
    error.value = t('login.password_requirements')
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await authStore.register(username.value, password.value)
    success.value = t('register.success')
    redirectTimer = setTimeout(() => router.push('/login'), 1500)
  } catch (e) {
    error.value = e.response?.data?.message || t('register.failed')
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
