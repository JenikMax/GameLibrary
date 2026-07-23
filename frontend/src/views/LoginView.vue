<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <template v-if="isRetroTerminal">
          <pre class="terminal-ascii">
 ┌─┐┌─┐┌ ┐┌─┐┌─┐
 │ ┌┘││││┌┘├┤ │ │
 └─┘└─┘└└┘└ └└─┘└─┘
          </pre>
          <div class="terminal-prompt">game-library@auth:~$ login --user<span class="t-cursor" /></div>
        </template>
        <template v-else>
          <img :src="'/game-library/img/logo.jpg'" height="56" alt="logo" class="auth-logo" />
          <h2 class="auth-title">{{ t('login.signin') }}</h2>
          <p class="auth-sub">{{ t('login.subtitle') }}</p>
        </template>
      </div>
      <Message v-if="error" severity="error" :closable="false" class="auth-msg mb-3">{{ error }}</Message>
      <form @submit.prevent="handleLogin">
        <div class="field">
          <label for="login-username" class="field-label">{{ isRetroTerminal ? 'username:' : t('login.username') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-user" /></InputIcon>
            <InputText id="login-username" v-model="username" class="w-full auth-input" :placeholder="isRetroTerminal ? 'enter_username' : ''" autofocus />
          </IconField>
        </div>
        <div class="field">
          <label for="login-password" class="field-label">{{ isRetroTerminal ? 'password:' : t('login.password') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-lock" /></InputIcon>
            <Password id="login-password" v-model="password" :feedback="false" class="w-full auth-password" inputClass="w-full auth-input" :inputClass="'w-full auth-input'" toggleMask :placeholder="isRetroTerminal ? '••••••••' : ''" />
          </IconField>
        </div>
        <Button type="submit" :label="isRetroTerminal ? '$ execute login' : t('login.signin')" :icon="isRetroTerminal ? undefined : 'pi pi-sign-in'" class="w-full mt-2 auth-btn" :loading="loading" />
      </form>
      <div class="auth-footer">
        <span class="footer-text">{{ isRetroTerminal ? 'no account?' : t('login.no_account') }}</span>
        <Button :label="isRetroTerminal ? 'run register.sh' : t('login.register')" link class="footer-link" @click="$router.push('/register')" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import { useTheme } from '../composables/useTheme'
import InputText from 'primevue/inputtext'
import InputIcon from 'primevue/inputicon'
import IconField from 'primevue/iconfield'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()
const { currentThemeId } = useTheme()
const isRetroTerminal = computed(() => currentThemeId.value === 'retro-terminal')

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

<style scoped>
.terminal-ascii {
  font-size: 0.7rem;
  color: #00ff88;
  line-height: 1.2;
  text-shadow: 0 0 8px rgba(0,255,136,0.5);
  display: inline-block;
  margin-bottom: 0.5rem;
}
.terminal-prompt {
  font-size: 0.8rem;
  color: #00cc6a;
  margin-bottom: 1.5rem;
}
</style>
