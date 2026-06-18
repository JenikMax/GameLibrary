<template>
  <Menubar :model="items" class="app-header">
    <template #start>
      <router-link to="/" style="display:flex;align-items:center;gap:0.5rem">
        <img :src="'/game-library/img/logo_w.jpg'" height="32" alt="logo" />
      </router-link>
    </template>
    <template #end>
      <div class="flex align-items-center gap-2">
        <LocaleSwitcher />
        <Button
          :icon="isDarkMode ? 'pi pi-sun' : 'pi pi-moon'"
          severity="secondary"
          text
          rounded
          @click="toggleDarkMode"
          v-tooltip.left="isDarkMode ? t('nav.dark') : t('nav.light')"
        />
        <Avatar
          v-if="authStore.avatarUrl"
          :image="authStore.avatarUrl"
          shape="circle"
          size="small"
          v-tooltip.left="authStore.username"
          @click="goToProfile"
          class="cursor-pointer"
        />
        <Button
          icon="pi pi-sign-out"
          severity="danger"
          text
          rounded
          @click="handleLogout"
          v-tooltip.left="t('nav.logout')"
        />
      </div>
    </template>
  </Menubar>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useDarkMode } from '../composables/useDarkMode'
import { useI18n } from '../composables/useI18n'
import { adminApi } from '../api/admin'
import LocaleSwitcher from './LocaleSwitcher.vue'
import Menubar from 'primevue/menubar'
import Button from 'primevue/button'
import Avatar from 'primevue/avatar'
import { useToast } from 'primevue/usetoast'

const router = useRouter()
const authStore = useAuthStore()
const { isDarkMode, toggleDarkMode } = useDarkMode()
const { t } = useI18n()
const toast = useToast()

const items = computed(() => {
  const menu = [
    {
      label: t('nav.library'),
      icon: 'pi pi-th-large',
      command: () => router.push('/')
    },
    {
      label: t('nav.downloads'),
      icon: 'pi pi-download',
      command: () => router.push('/downloads')
    }
  ]
  if (authStore.isAdmin) {
    menu.push({
      label: t('nav.admin'),
      icon: 'pi pi-cog',
      items: [
        {
          label: t('nav.users'),
          icon: 'pi pi-users',
          command: () => router.push('/admin/users')
        },
        {
          label: t('nav.settings'),
          icon: 'pi pi-sliders-h',
          items: [
            {
              label: t('nav.scan'),
              icon: 'pi pi-refresh',
              command: async () => {
                try {
                  await adminApi.scanLibrary()
                  toast.add({ severity: 'success', summary: t('library.scan_start'), life: 3000 })
                } catch {
                  toast.add({ severity: 'error', summary: t('library.scan_failed'), life: 3000 })
                }
              }
            }
          ]
        }
      ]
    })
  }
  return menu
})

function goToProfile() {
  router.push('/profile')
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  border-radius: 0;
}
</style>
