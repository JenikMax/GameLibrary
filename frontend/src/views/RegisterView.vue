<template>
  <div class="flex align-items-center justify-content-center min-h-screen">
    <Card class="register-card">
      <template #title>
        <div class="text-center">
          <h2>Create Account</h2>
        </div>
      </template>
      <template #content>
        <Message v-if="error" severity="error" :closable="false" class="mb-3">{{ error }}</Message>
        <Message v-if="success" severity="success" :closable="false" class="mb-3">{{ success }}</Message>
        <form @submit.prevent="handleRegister">
          <div class="field">
            <label for="username">Username</label>
            <InputText id="username" v-model="username" class="w-full" autofocus />
          </div>
          <div class="field">
            <label for="password">Password</label>
            <Password id="password" v-model="password" class="w-full" toggleMask />
          </div>
          <Button type="submit" label="Register" icon="pi pi-user-plus" class="w-full mt-2" :loading="loading" />
        </form>
      </template>
      <template #footer>
        <div class="text-center">
          <span class="text-sm">Already have an account?</span>
          <Button label="Sign In" link @click="$router.push('/login')" />
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
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)

async function handleRegister() {
  if (!username.value || !password.value) {
    error.value = 'Please fill in all fields'
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await authStore.register(username.value, password.value)
    success.value = 'Registration successful! Redirecting to login...'
    setTimeout(() => router.push('/login'), 1500)
  } catch (e) {
    error.value = e.response?.data?.message || 'Registration failed'
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
