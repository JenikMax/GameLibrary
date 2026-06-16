<template>
  <div :class="{ 'app-dark': isDarkMode }">
    <Toast />
    <ConfirmDialog />
    <AppHeader v-if="authStore.isAuthenticated" />
    <div class="main-container" :class="{ 'with-header': authStore.isAuthenticated }">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useAuthStore } from './stores/auth'
import { useDarkMode } from './composables/useDarkMode'
import AppHeader from './components/AppHeader.vue'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'

const authStore = useAuthStore()
const { isDarkMode } = useDarkMode()
</script>

<style>
.main-container {
  min-height: 100vh;
}
.main-container.with-header {
  min-height: calc(100vh - 60px);
  padding-top: 60px;
}
</style>
