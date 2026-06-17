<template>
  <div class="flex align-items-center justify-content-center min-h-screen">
    <Card class="login-card">
      <template #title>
        <div class="text-center">
          <img :src="'/game-library/img/logo.jpg'" height="64" alt="logo" />
          <h2>{{ t('login.signin') }}</h2>
        </div>
      </template>
      <template #content>
        <Message v-if="error" severity="error" :closable="false" class="mb-3">{{ error }}</Message>
        <form @submit.prevent="handleLogin">
          <div class="field">
            <label for="username">{{ t('login.username') }}</label>
            <IconField>
              <InputIcon><i class="pi pi-user" /></InputIcon>
              <InputText id="username" v-model="username" class="w-full" autofocus />
            </IconField>
          </div>
          <div class="field">
            <label for="password">{{ t('login.password') }}</label>
            <IconField>
              <InputIcon><i class="pi pi-lock" /></InputIcon>
              <Password id="password" v-model="password" :feedback="false" class="w-full" toggleMask />
            </IconField>
          </div>
          <Button type="submit" :label="t('login.signin')" icon="pi pi-sign-in" class="w-full mt-2" :loading="loading" />
        </form>
      </template>
      <template #footer>
        <div class="text-center">
          <span class="text-sm">{{ t('login.no_account') }}</span>
          <Button :label="t('login.register')" link @click="$router.push('/register')" />
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import InputIcon from 'primevue/inputicon'
import IconField from 'primevue/iconfield'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

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
    setTimeout(() => { error.value = '' }, 7000)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-card {
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
