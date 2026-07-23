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
          <div class="terminal-prompt">game-library@auth:~$ register --new-player<span class="t-cursor" /></div>
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
          <label for="reg-username" class="field-label">{{ isRetroTerminal ? 'username:' : t('login.username') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-user" /></InputIcon>
            <InputText id="reg-username" v-model="username" class="w-full auth-input" :placeholder="isRetroTerminal ? 'choose_name' : ''" autofocus />
          </IconField>
        </div>
        <div class="field">
          <label for="reg-password" class="field-label">{{ isRetroTerminal ? 'password:' : t('login.password') }}</label>
          <IconField class="w-full">
            <InputIcon><i class="pi pi-lock" /></InputIcon>
            <Password id="reg-password" v-model="password" class="w-full auth-password" inputClass="w-full auth-input" toggleMask :feedback="true" :placeholder="isRetroTerminal ? '••••••••' : ''" />
          </IconField>
          <small class="field-hint">{{ isRetroTerminal ? '└─ min 8 chars, letters + digits required' : t('login.password_requirements') }}</small>
        </div>
        <Button type="submit" :label="isRetroTerminal ? '$ execute register' : t('login.register')" :icon="isRetroTerminal ? undefined : 'pi pi-user-plus'" class="w-full mt-2 auth-btn" :loading="loading" />
      </form>
      <div class="auth-footer">
        <span class="footer-text">{{ isRetroTerminal ? 'existing user?' : t('register.already_have_account') }}</span>
        <Button :label="isRetroTerminal ? 'run login.sh' : t('login.signin')" link class="footer-link" @click="$router.push('/login')" />
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

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()
const { currentThemeId } = useTheme()
const isRetroTerminal = computed(() => currentThemeId.value === 'retro-terminal')

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
