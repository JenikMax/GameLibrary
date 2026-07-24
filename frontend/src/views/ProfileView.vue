<template>
  <div class="profile-container">
    <Card>
      <template #content>
        <div class="profile-header">
          <Avatar :image="authStore.avatarUrl" size="xlarge" shape="circle" class="profile-avatar" />
          <h2 class="profile-name">
            <span v-if="currentThemeId === 'retro-terminal'">&gt; {{ terminalTitle }}<span class="t-cursor" /></span>
            <span v-else>{{ authStore.username }}</span>
          </h2>
          <div class="profile-tags flex gap-2 mb-3">
            <Tag
              :value="authStore.isAdmin ? t('profile.role_admin') : t('profile.role_user')"
              :severity="authStore.isAdmin ? 'success' : 'info'"
              rounded
            />
            <Tag
              v-if="profileData?.memberSince"
              :value="t('profile.member_since', { year: profileData.memberSince })"
              severity="info"
              rounded
            />
          </div>
          <div v-if="profileData" class="profile-stats">
            <div class="stat-item">
              <span class="stat-value">{{ profileData.gamesCount }}</span>
              <span class="stat-label">{{ t('profile.games') }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ profileData.ratingsCount }}</span>
              <span class="stat-label">{{ t('profile.ratings') }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ profileData.collectionsCount }}</span>
              <span class="stat-label">{{ t('profile.collections') }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ profileData.reviewsCount }}</span>
              <span class="stat-label">{{ t('profile.reviews') }}</span>
            </div>
          </div>
        </div>

        <Divider />

        <div class="profile-tabs">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'avatar' }"
            @click="activeTab = 'avatar'"
          >{{ t('profile.avatar_tab') }}</button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'password' }"
            @click="activeTab = 'password'"
          >{{ t('profile.password_tab') }}</button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'info' }"
            @click="activeTab = 'info'"
          >{{ t('profile.info_tab') }}</button>
        </div>

        <div class="profile-tab-content">
          <Message v-if="message" :severity="messageSeverity" :closable="false" class="mb-3">{{ message }}</Message>

          <div v-if="activeTab === 'avatar'" class="flex flex-column align-items-center gap-3">
            <Avatar :image="previewUrl || authStore.avatarUrl" size="xlarge" shape="circle" />
            <FileUpload
              mode="basic"
              accept="image/*"
              :maxFileSize="2097152"
              @select="onFileSelect"
              :chooseLabel="t('profile.select_image')"
            />
            <Button
              v-if="previewUrl"
              :label="t('profile.upload_avatar')"
              icon="pi pi-upload"
              @click="uploadAvatar"
              :loading="saving"
            />
          </div>

          <div v-if="activeTab === 'password'" class="flex flex-column gap-2" style="max-width:360px">
            <div class="field">
              <label for="newPass">{{ t('profile.new_password') }}</label>
              <Password id="newPass" v-model="newPassword" class="w-full" toggleMask :feedback="true" />
              <small class="text-muted">{{ t('login.password_requirements') }}</small>
            </div>
            <small v-if="passError" class="p-error">{{ passError }}</small>
            <Button :label="t('profile.change_password_btn')" icon="pi pi-key" @click="changePassword" :loading="changingPass" />
          </div>

          <div v-if="activeTab === 'info' && profileData" class="profile-info">
            <div class="info-row">
              <span class="info-label">{{ t('login.username') }}</span>
              <span class="info-value">{{ authStore.username }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">{{ t('profile.registered') }}</span>
              <span class="info-value">{{ profileData.memberSince ? t('profile.member_since', { year: profileData.memberSince }) : '-' }}</span>
            </div>
            <Divider />
            <div class="font-semibold mb-2">{{ t('profile.stats') }}</div>
            <div class="info-row">
              <span class="info-label">{{ t('profile.ratings') }}</span>
              <span class="info-value">{{ profileData.ratingsCount }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">{{ t('profile.reviews') }}</span>
              <span class="info-value">{{ profileData.reviewsCount }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">{{ t('profile.comments') }}</span>
              <span class="info-value">{{ profileData.commentsCount }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">{{ t('profile.favorites') }}</span>
              <span class="info-value">{{ profileData.favoritesCount }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">{{ t('profile.collections') }}</span>
              <span class="info-value">{{ profileData.collectionsCount }}</span>
            </div>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import { useTheme } from '../composables/useTheme'
import { profileApi } from '../api/profile'
import Card from 'primevue/card'
import Avatar from 'primevue/avatar'
import Tag from 'primevue/tag'
import FileUpload from 'primevue/fileupload'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Divider from 'primevue/divider'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const { currentThemeId } = useTheme()
const authStore = useAuthStore()
const toast = useToast()

const terminalTitle = computed(() => authStore.username.replace(/ /g, '_'))

const activeTab = ref('avatar')
const profileData = ref(null)
const newPassword = ref('')
const passError = ref('')
const selectedFile = ref(null)
const previewUrl = ref('')
const saving = ref(false)
const changingPass = ref(false)
const message = ref('')
const messageSeverity = ref('info')

onMounted(async () => {
  try {
    const res = await profileApi.getProfile()
    profileData.value = res.data.data
  } catch {
    message.value = 'Failed to load profile'
    messageSeverity.value = 'error'
  }
})

function onFileSelect(event) {
  const file = event.files[0]
  if (file) {
    selectedFile.value = file
    const reader = new FileReader()
    reader.onload = (e) => {
      previewUrl.value = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

async function uploadAvatar() {
  if (!previewUrl.value) return
  saving.value = true
  message.value = ''
  try {
    const res = await profileApi.updateProfile({ avatar: previewUrl.value })
    profileData.value = res.data.data
    await authStore.checkAuth()
    toast.add({ severity: 'success', summary: t('profile.avatar_updated'), life: 3000 })
    previewUrl.value = ''
  } catch {
    message.value = t('profile.avatar_update_failed')
    messageSeverity.value = 'error'
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  passError.value = ''
  if (!newPassword.value) return
  if (newPassword.value.length < 8 || !/[A-Za-z]/.test(newPassword.value) || !/\d/.test(newPassword.value)) {
    passError.value = t('login.password_requirements')
    return
  }
  changingPass.value = true
  message.value = ''
  try {
    await profileApi.changePassword({ currentPassword: '', newPassword: newPassword.value })
    toast.add({ severity: 'success', summary: t('profile.password_changed'), life: 3000 })
    newPassword.value = ''
  } catch {
    message.value = t('profile.password_change_failed')
    messageSeverity.value = 'error'
  } finally {
    changingPass.value = false
  }
}
</script>

<style scoped>
.profile-container {
  max-width: 520px;
  margin: 0 auto;
  padding: 1rem;
}
.profile-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}
.profile-avatar {
  margin-bottom: 0.25rem;
}
.profile-name {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 600;
}
.profile-tags {
  justify-content: center;
}
.profile-stats {
  display: flex;
  gap: 1.5rem;
  justify-content: center;
  padding: 0.5rem 0;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.1rem;
}
.stat-value {
  font-size: 1.3rem;
  font-weight: 700;
  line-height: 1.2;
}
.stat-label {
  font-size: 0.7rem;
  color: var(--p-text-muted-color);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.field {
  margin-bottom: 1rem;
}
.field label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.25rem;
}
.p-fileupload input[type="file"] {
  display: none;
}
.profile-tabs {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--p-content-border-color, var(--p-surface-200));
  margin-bottom: 1.25rem;
}
.tab-btn {
  padding: 0.6rem 1.2rem;
  font-size: 0.88rem;
  cursor: pointer;
  color: var(--p-text-muted-color);
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.15s;
  font-family: var(--font-family, inherit);
  background: none;
  border-top: none;
  border-left: none;
  border-right: none;
}
.tab-btn:hover {
  color: var(--p-text-color);
}
.tab-btn.active {
  color: var(--p-primary-color);
  border-bottom-color: var(--p-primary-color);
  font-weight: 600;
}
.profile-tab-content {
  min-height: 140px;
}
.profile-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
}
.info-label {
  color: var(--p-text-muted-color);
}
.info-value {
  font-weight: 500;
}
[data-theme="retro-terminal"] .tab-btn {
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-size: 0.78rem;
  font-family: 'Courier New', monospace;
  border-radius: 0;
}
[data-theme="retro-terminal"] .tab-btn.active {
  color: #00ff88;
  border-bottom-color: #00ff88;
}
[data-theme="retro-terminal"] .stat-value {
  text-shadow: 0 0 10px rgba(0,255,136,0.5);
  color: #00ff88;
}
[data-theme="retro-terminal"] .stat-label {
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-size: 0.7rem;
  color: #00cc6a;
}
[data-theme="retro-terminal"] .info-row {
  border-bottom: 1px dashed rgba(0,255,136,0.15);
  padding-bottom: 0.5rem;
}
</style>