<template>
  <div class="auth-page" :class="{ 'auth-dark': isDarkMode }">
    <div class="auth-card">
      <div class="auth-header">
        <img :src="'/game-library/img/logo.jpg'" height="56" alt="logo" class="auth-logo" />
        <h2 class="auth-title">{{ t('register.create_account') }}</h2>
        <p class="auth-sub">{{ t('register.subtitle') }}</p>
      </div>
      <Message v-if="error" severity="error" :closable="false" class="auth-msg mb-3">{{ error }}</Message>
      <Message v-if="success" severity="success" :closable="false" class="auth-msg mb-3">{{ success }}</Message>
      <form @submit.prevent="handleRegister">
        <div class="field">
          <label for="reg-username" class="field-label">{{ t('login.username') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-user" /></InputIcon>
            <InputText id="reg-username" v-model="username" class="w-full auth-input" autofocus />
          </IconField>
        </div>
        <div class="field">
          <label for="reg-password" class="field-label">{{ t('login.password') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-lock" /></InputIcon>
            <Password id="reg-password" v-model="password" class="w-full auth-password" inputClass="w-full auth-input" toggleMask :feedback="true" />
          </IconField>
          <small class="field-hint">{{ t('login.password_requirements') }}</small>
        </div>
        <Button type="submit" :label="t('login.register')" icon="pi pi-user-plus" class="w-full mt-2 auth-btn" :loading="loading" />
      </form>
      <div class="auth-footer">
        <span class="footer-text">{{ t('register.already_have_account') }}</span>
        <Button :label="t('login.signin')" link class="footer-link" @click="$router.push('/login')" />
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

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()
const { isDarkMode } = useDarkMode()

const username = ref('')
const password = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)
let redirectTimer = null

onBeforeUnmount(() => {
  if (redirectTimer) clearTimeout(redirectTimer)
})

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
