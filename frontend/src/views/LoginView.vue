<template>
  <div class="flex align-items-center justify-content-center min-h-screen">
    <Card class="login-card">
      <template #title>
        <div class="text-center">
          <img :src="'/game-library/img/logo.jpg'" height="64" alt="logo" />
          <h2>Sign In</h2>
        </div>
      </template>
      <template #content>
        <Message v-if="error" severity="error" :closable="false" class="mb-3">{{ error }}</Message>
        <form @submit.prevent="handleLogin">
          <div class="field">
            <label for="username">Username</label>
            <IconField>
              <InputIcon><i class="pi pi-user" /></InputIcon>
              <InputText id="username" v-model="username" class="w-full" autofocus />
            </IconField>
          </div>
          <div class="field">
            <label for="password">Password</label>
            <IconField>
              <InputIcon><i class="pi pi-lock" /></InputIcon>
              <Password id="password" v-model="password" :feedback="false" class="w-full" toggleMask />
            </IconField>
          </div>
          <Button type="submit" label="Sign In" icon="pi pi-sign-in" class="w-full mt-2" :loading="loading" />
        </form>
      </template>
      <template #footer>
        <div class="text-center">
          <span class="text-sm">Don't have an account?</span>
          <Button label="Register" link @click="$router.push('/register')" />
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import InputIcon from 'primevue/inputicon'
import IconField from 'primevue/iconfield'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!username.value || !password.value) {
    error.value = 'Please fill in all fields'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await authStore.login(username.value, password.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || 'Invalid username or password'
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
