<template>
  <div class="flex align-items-center justify-content-center min-h-screen">
    <Card class="register-card">
      <template #title>
        <div class="text-center">
          <h2>{{ t('register.create_account') }}</h2>
        </div>
      </template>
      <template #content>
        <Message v-if="error" severity="error" :closable="false" class="mb-3">{{ error }}</Message>
        <Message v-if="success" severity="success" :closable="false" class="mb-3">{{ success }}</Message>
        <form @submit.prevent="handleRegister">
          <div class="field">
            <label for="username">{{ t('login.username') }}</label>
            <InputText id="username" v-model="username" class="w-full" autofocus />
          </div>
          <div class="field">
            <label for="password">{{ t('login.password') }}</label>
            <Password id="password" v-model="password" class="w-full" toggleMask :feedback="true" />
            <small class="text-muted">{{ t('login.password_requirements') }}</small>
          </div>
          <Button type="submit" :label="t('login.register')" icon="pi pi-user-plus" class="w-full mt-2" :loading="loading" />
        </form>
      </template>
      <template #footer>
        <div class="text-center">
          <span class="text-sm">{{ t('register.already_have_account') }}</span>
          <Button :label="t('login.signin')" link @click="$router.push('/login')" />
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

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

<style scoped>
.register-card {
  width: 400px;
  max-width: 90vw;
}
.field {
  margin-bottom: 1rem;
}
.field label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.25rem;
}
</style>
