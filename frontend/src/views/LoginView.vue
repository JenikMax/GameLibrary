<template>
  <div class="auth-page" :class="{ 'auth-dark': isDarkMode }">
    <div class="auth-card">
      <div class="auth-header">
        <img :src="'/game-library/img/logo.jpg'" height="56" alt="logo" class="auth-logo" />
        <h2 class="auth-title">{{ t('login.signin') }}</h2>
        <p class="auth-sub">{{ t('login.subtitle') }}</p>
      </div>
      <Message v-if="error" severity="error" :closable="false" class="auth-msg mb-3">{{ error }}</Message>
      <form @submit.prevent="handleLogin">
        <div class="field">
          <label for="login-username" class="field-label">{{ t('login.username') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-user" /></InputIcon>
            <InputText id="login-username" v-model="username" class="w-full auth-input" autofocus />
          </IconField>
        </div>
        <div class="field">
          <label for="login-password" class="field-label">{{ t('login.password') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-lock" /></InputIcon>
            <Password id="login-password" v-model="password" :feedback="false" class="w-full auth-password" inputClass="w-full auth-input" toggleMask />
          </IconField>
        </div>
        <Button type="submit" :label="t('login.signin')" icon="pi pi-sign-in" class="w-full mt-2 auth-btn" :loading="loading" />
      </form>
      <div class="auth-footer">
        <span class="footer-text">{{ t('login.no_account') }}</span>
        <Button :label="t('login.register')" link class="footer-link" @click="$router.push('/register')" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import { useDarkMode } from '../composables/useDarkMode'
import InputText from 'primevue/inputtext'
import InputIcon from 'primevue/inputicon'
import IconField from 'primevue/iconfield'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()
const { isDarkMode } = useDarkMode()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
let errorTimer = null

onBeforeUnmount(() => {
  if (errorTimer) clearTimeout(errorTimer)
})

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
