<template>
  <div class="app-header-wrapper">
    <Menubar :model="items" class="app-header">

      <template #start>
        <span v-if="currentThemeId === 'retro-terminal'" class="terminal-title">~$<span class="t-cursor" /></span>
      </template>

      <template #end>
        <div class="flex align-items-center gap-2">
          <div v-if="authStore.isAuthenticated" ref="notificationAreaRef" class="notification-area" @click="toggleNotifications">
            <Button
              icon="pi pi-bell"
              :severity="unreadCount > 0 ? 'warning' : 'secondary'"
              text
              rounded
              class="p-overlay-badge"
            >
              <Badge v-if="unreadCount > 0" :value="unreadCount" severity="danger" />
            </Button>
          </div>
          <LocaleSwitcher />
          <Button
            :icon="currentTheme?.icon || 'pi pi-palette'"
            severity="secondary"
            text
            rounded
            @click="toggleThemeMenu"
            v-tooltip.left="t('nav.theme')"
          />
          <Popover ref="themeMenuRef" :pt="{ content: { style: 'padding: 0.25rem' } }">
            <div class="theme-list">
              <div
                v-for="theme in availableThemes"
                :key="theme.id"
                class="theme-option"
                :class="{ active: currentThemeId === theme.id }"
                @click="selectTheme(theme.id)"
              >
                <i :class="theme.icon" style="width: 1.2rem;" />
                <span>{{ t('theme.' + theme.id) }}</span>
                <span class="theme-dot" :style="{ background: theme.previewColor }" />
              </div>
            </div>
          </Popover>
          <Avatar
            :image="authStore.avatarUrl || '/game-library/img/user.png'"
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

    <div v-if="showNotifications" class="notification-panel p-panel p-component">
      <div class="p-panel-header px-3 pt-3 pb-0">
        <div class="flex align-items-center justify-content-between w-full">
          <span class="font-semibold">{{ t('notifications.title') }}</span>
          <Button
            v-if="unreadCount > 0"
            :label="t('notifications.read_all')"
            size="small"
            text
            @click="handleMarkAllRead"
          />
        </div>
      </div>
      <div class="p-panel-content p-2">
        <div v-if="notifications.length === 0" class="text-center text-color-secondary p-3">
          {{ t('notifications.empty') }}
        </div>
        <div v-else class="notification-list">
          <div
            v-for="n in notifications"
            :key="n.id"
            class="notification-item"
            :class="{ unread: !n.read }"
            @click="handleClickNotification(n)"
          >
            <div class="flex align-items-start gap-2">
              <i v-if="n.type === 'seed_complete' || n.type === 'download_ready'" class="pi pi-check-circle text-green-500 mt-1" />
              <i v-else-if="n.type === 'scan_complete'" class="pi pi-refresh text-blue-500 mt-1" />
              <i v-else class="pi pi-exclamation-triangle text-red-500 mt-1" />
              <div class="flex-1">
                <div class="font-semibold text-sm">{{ n.title }}</div>
                <div class="text-xs text-color-secondary">{{ n.message }}</div>
                <div class="text-xs text-color-secondary mt-1">{{ n.createdAt }}</div>
              </div>
              <Button
                v-if="!n.read"
                icon="pi pi-check"
                size="small"
                text
                rounded
                @click.stop="handleMarkRead(n.id)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useLibraryStore } from '../stores/library'
import { useTheme } from '../composables/useTheme'
import { useI18n } from '../composables/useI18n'
import { adminApi } from '../api/admin'
import { notificationsApi } from '../api/notifications'
import LocaleSwitcher from './LocaleSwitcher.vue'
import Menubar from 'primevue/menubar'
import Button from 'primevue/button'
import Avatar from 'primevue/avatar'
import Badge from 'primevue/badge'
import Popover from 'primevue/popover'
import { useToast } from 'primevue/usetoast'

const router = useRouter()
const authStore = useAuthStore()
const { currentThemeId, currentTheme, setTheme, availableThemes } = useTheme()
const { t } = useI18n()
const toast = useToast()
const themeMenuRef = ref(null)

function toggleThemeMenu(e) {
  themeMenuRef.value?.toggle(e)
}

function selectTheme(themeId) {
  setTheme(themeId)
  themeMenuRef.value?.hide()
}

const showNotifications = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
let pollTimer = null
let isUnmounted = false
const notificationAreaRef = ref(null)

function onClickOutside(e) {
  if (!showNotifications.value) return
  const el = notificationAreaRef.value
  if (el && !el.contains(e.target)) {
    showNotifications.value = false
  }
}

const items = computed(() => {
  const menu = [
    {
      label: t('nav.library'),
      icon: 'pi pi-th-large',
      items: [
        {
          label: t('nav.library_list'),
          icon: 'pi pi-list',
          command: goToLibrary
        },
        {
          label: t('nav.library_favorites'),
          icon: 'pi pi-heart',
          command: goToFavorites
        }
      ]
    },
    {
      label: t('nav.downloads'),
      icon: 'pi pi-download',
      command: () => router.push('/downloads')
    },
    {
      label: t('nav.collections'),
      icon: 'pi pi-folder',
      command: goToCollections
    },
    {
      label: t('nav.statistics'),
      icon: 'pi pi-chart-bar',
      command: goToStatistics
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
          label: t('nav.scrapers'),
          icon: 'pi pi-cloud-download',
          command: () => router.push('/admin/scrapers')
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
                  fetchNotifications()
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

async function fetchNotifications() {
  if (!authStore.isAuthenticated) return
  try {
    const res = await notificationsApi.getNotifications()
    notifications.value = res.data.data.items || []
    unreadCount.value = res.data.data.unread || 0
  } catch {
    // ignore
  }
}

function toggleNotifications() {
  showNotifications.value = !showNotifications.value
  if (showNotifications.value) fetchNotifications()
}

async function handleMarkRead(id) {
  await notificationsApi.markAsRead(id)
  const n = notifications.value.find(x => x.id === id)
  if (n) n.read = true
  unreadCount.value = Math.max(0, unreadCount.value - 1)
}

async function handleMarkAllRead() {
  await notificationsApi.markAllAsRead()
  notifications.value.forEach(n => { n.read = true })
  unreadCount.value = 0
}

function handleClickNotification(n) {
  if (!n.read) handleMarkRead(n.id)
  if (n.gameId) {
    showNotifications.value = false
    router.push(`/game/${n.gameId}`)
  }
}

async function goToLibrary() {
  sessionStorage.removeItem('libraryState')
  const libStore = useLibraryStore()
  libStore.resetFilters()
  if (router.currentRoute.value.path === '/' && !router.currentRoute.value.query.favorites) {
    libStore.fetchGames(1)
  } else {
    await router.push('/')
    libStore.fetchGames(1)
  }
}

async function goToFavorites() {
  sessionStorage.removeItem('libraryState')
  const libStore = useLibraryStore()
  libStore.resetFilters()
  libStore.favoritesOnly = true
  if (router.currentRoute.value.path === '/' && router.currentRoute.value.query.favorites === '1') {
    libStore.fetchGames(1)
  } else {
    await router.push('/?favorites=1')
    libStore.fetchGames(1)
  }
}

async function goToCollections() {
  await router.push('/collections')
}

async function goToStatistics() {
  await router.push('/statistics')
}

function goToProfile() {
  router.push('/profile')
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

onMounted(() => {
  document.addEventListener('click', onClickOutside, true)
  if (authStore.isAuthenticated) {
    fetchNotifications()
    pollTimer = setInterval(() => {
      if (!isUnmounted) fetchNotifications()
    }, 15000)
  }
})

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside, true)
  isUnmounted = true
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.app-header-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: var(--p-menubar-background, var(--p-surface-0, #ffffff));
  border-bottom: 1px solid var(--p-menubar-border-color, var(--p-surface-200, #e5e7eb));
}
.app-header {
  max-width: 1400px;
  margin: 0 auto;
  border-radius: 0;
  width: 100%;
  border: none;
}
.notification-area {
  position: relative;
}
.notification-panel {
  position: absolute;
  top: 100%;
  right: 0;
  width: 360px;
  z-index: 2000;
  background: var(--p-panel-background, var(--p-surface-0));
  border: 1px solid var(--p-panel-border-color, var(--p-surface-200));
  border-radius: var(--p-panel-border-radius, 6px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
[data-theme="default-dark"] .notification-panel {
  background: var(--p-surface-800);
  border-color: var(--p-surface-700);
}
.notification-list {
  max-height: 400px;
  overflow-y: auto;
}
.notification-item {
  padding: 0.5rem;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s;
}
.notification-item:hover {
  background: var(--p-surface-200);
}
.notification-item.unread {
  background: var(--p-primary-50);
}
[data-theme="default-dark"] .notification-item:hover {
  background: var(--p-surface-700);
}
[data-theme="default-dark"] .notification-item.unread {
  background: var(--p-primary-900);
}
.theme-list {
  display: flex;
  flex-direction: column;
  min-width: 180px;
}
.theme-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  border-radius: 4px;
  font-size: 0.85rem;
  transition: background 0.15s;
}
.theme-option:hover {
  background: var(--p-surface-100);
}
.theme-option.active {
  background: var(--p-primary-50);
  font-weight: 600;
}
.theme-dot {
  margin-left: auto;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid var(--p-surface-300);
}
.terminal-title {
  display: none;
  color: #00cc6a;
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  font-weight: bold;
  margin-right: 0.75rem;
  text-shadow: 0 0 8px rgba(0,255,136,0.4);
}
[data-theme="retro-terminal"] .terminal-title {
  display: inline-flex;
  align-items: center;
}
@keyframes rt-header-blink { 50% { opacity: 0; } }
.terminal-title .t-cursor {
  display: inline-block;
  width: 8px;
  height: 14px;
  background: #00ff88;
  animation: rt-header-blink 1s step-end infinite;
  vertical-align: middle;
  margin-left: 4px;
}
</style>
