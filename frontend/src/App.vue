<template>
  <div :class="{ 'app-dark': isDarkMode }">
    <Toast />
    <ConfirmDialog />
    <LocaleSwitcher v-if="!authStore.isAuthenticated" class="lang-switcher-top" />
    <AppHeader v-if="authStore.isAuthenticated" />
    <div class="main-container" :class="{ 'with-header': authStore.isAuthenticated }">
      <router-view v-slot="{ Component, route }">
        <Transition name="route-fade" mode="out-in">
          <component :is="Component" :key="route.path" />
        </Transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from './stores/auth'
import { useDarkMode } from './composables/useDarkMode'
import AppHeader from './components/AppHeader.vue'
import LocaleSwitcher from './components/LocaleSwitcher.vue'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'

const authStore = useAuthStore()
const { isDarkMode } = useDarkMode()
</script>

<style>
.lang-switcher-top {
  position: fixed;
  top: 0.5rem;
  right: 0.5rem;
  z-index: 1100;
}
.main-container {
  min-height: 100vh;
}
.main-container.with-header {
  min-height: calc(100vh - 60px);
  padding-top: 60px;
}
</style>
