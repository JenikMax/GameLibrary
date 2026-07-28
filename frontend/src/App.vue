<!-- Корневой компонент приложения. Содержит Toast, ConfirmDialog, LocaleSwitcher (для неавторизованных), AppHeader (для авторизованных) и router-view с анимацией перехода. Слушает глобальное событие api-error для показа всплывающих уведомлений. -->
<template>
  <div>
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
// Импорты Vue и хранилищ
import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from './stores/auth'
import { useTheme } from './composables/useTheme'
import AppHeader from './components/AppHeader.vue'
import LocaleSwitcher from './components/LocaleSwitcher.vue'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import { useToast } from 'primevue/usetoast'

const authStore = useAuthStore()
useTheme() // Инициализация темы из localStorage
const toast = useToast()

// Обработчик глобальной ошибки API — показывает тост
function onApiError(e) {
  toast.add({ severity: 'error', summary: e.detail, life: 5000 })
}

onMounted(() => {
  window.addEventListener('api-error', onApiError)
})

onUnmounted(() => {
  window.removeEventListener('api-error', onApiError)
})
</script>

<style>
/* Переключатель языка поверх контента на странице логина */
.lang-switcher-top {
  position: fixed;
  top: 0.5rem;
  right: 0.5rem;
  z-index: 1100;
}
.main-container {
  min-height: 100vh;
  background-color: var(--p-surface-50);
}
.main-container.with-header {
  min-height: calc(100vh - 60px);
  padding-top: 60px;
}
</style>
