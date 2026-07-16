<template>
  <div class="profile-container">
    <Card>
      <template #title>
        <div class="flex align-items-center gap-3">
          <Avatar :image="authStore.avatarUrl" size="xlarge" shape="circle" />
          <div>
            <h2 class="m-0">{{ authStore.username }}</h2>
            <Tag
              :value="authStore.isAdmin ? t('profile.role_admin') : t('profile.role_user')"
              :severity="authStore.isAdmin ? 'danger' : 'info'"
            />
          </div>
        </div>
      </template>
      <template #content>
        <Message v-if="message" :severity="messageSeverity" :closable="false" class="mb-3">{{ message }}</Message>

        <Accordion :activeIndex="0">
          <AccordionTab :header="t('profile.change_avatar')">
            <div class="flex flex-column align-items-center gap-3">
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
          </AccordionTab>
          <AccordionTab :header="t('profile.change_password')">
            <div class="field">
              <label for="newPass">{{ t('profile.new_password') }}</label>
              <Password id="newPass" v-model="newPassword" class="w-full" toggleMask :feedback="true" />
              <small class="text-muted">{{ t('login.password_requirements') }}</small>
            </div>
            <small v-if="passError" class="p-error">{{ passError }}</small>
            <Button :label="t('profile.change_password_btn')" icon="pi pi-key" @click="changePassword" :loading="changingPass" />
          </AccordionTab>

          <AccordionTab v-if="authStore.isAdmin" :header="t('profile.admin_actions')">
            <div class="flex flex-column gap-2">
              <Button :label="t('library.scan')" icon="pi pi-refresh" severity="warning" @click="scanLibrary" :loading="scanning" :disabled="!!scanTaskId" />
              <div v-if="scanTaskId" class="scan-progress mt-2">
                <div class="flex align-items-center justify-content-between mb-1">
                  <small>{{ scanPhaseLabel }}</small>
                  <small>{{ scanProgress }}%</small>
                </div>
                <ProgressBar :value="scanProgress" class="mb-1" />
                <small class="text-muted" v-if="scanCurrentGame">{{ scanCurrentGame }}</small>
              </div>
            </div>
          </AccordionTab>
        </Accordion>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import { profileApi } from '../api/profile'
import { adminApi } from '../api/admin'
import Card from 'primevue/card'
import Avatar from 'primevue/avatar'
import Tag from 'primevue/tag'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import FileUpload from 'primevue/fileupload'
import Password from 'primevue/password'
import ProgressBar from 'primevue/progressbar'
import Button from 'primevue/button'
import Message from 'primevue/message'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const authStore = useAuthStore()
const toast = useToast()

const newPassword = ref('')
const passError = ref('')
const selectedFile = ref(null)
const previewUrl = ref('')
const saving = ref(false)
const changingPass = ref(false)
const scanning = ref(false)
const message = ref('')
const messageSeverity = ref('info')

const scanTaskId = ref(null)
const scanProgress = ref(0)
const scanCurrentGame = ref('')
const scanPhase = ref('')
let scanPollTimer = null

const scanPhaseLabel = computed(() => {
  const phaseMap = {
    'PENDING': t('scan.phase_scanning'),
    'SCANNING_DIRS': t('scan.phase_scanning'),
    'STORING_METADATA': t('scan.phase_metadata'),
    'LOADING_IMAGES': t('scan.phase_images'),
    'COMPLETED': '',
    'FAILED': ''
  }
  return phaseMap[scanPhase.value] || ''
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
    await profileApi.updateProfile({ avatar: previewUrl.value })
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

async function scanLibrary() {
  scanning.value = true
  try {
    const res = await adminApi.scanLibrary()
    const { taskId } = res.data.data
    scanTaskId.value = taskId
    scanProgress.value = 0
    scanCurrentGame.value = ''
    scanPhase.value = 'PENDING'
    pollScanStatus()
  } catch {
    scanning.value = false
    toast.add({ severity: 'error', summary: t('library.scan_failed'), life: 3000 })
  }
}

function pollScanStatus() {
  if (!scanTaskId.value) return
  scanPollTimer = setInterval(async () => {
    try {
      const res = await adminApi.getScanStatus(scanTaskId.value)
      const task = res.data.data
      scanProgress.value = task.progress || 0
      scanCurrentGame.value = task.currentGame || ''
      scanPhase.value = task.status
      if (task.status === 'COMPLETED') {
        clearInterval(scanPollTimer)
        scanPollTimer = null
        scanTaskId.value = null
        scanning.value = false
        toast.add({ severity: 'success', summary: t('library.scan_complete'), life: 3000 })
      } else if (task.status === 'FAILED') {
        clearInterval(scanPollTimer)
        scanPollTimer = null
        scanTaskId.value = null
        scanning.value = false
        toast.add({ severity: 'error', summary: t('library.scan_failed'), detail: task.errorMessage || '', life: 5000 })
      }
    } catch {
      clearInterval(scanPollTimer)
      scanPollTimer = null
      scanTaskId.value = null
      scanning.value = false
    }
  }, 500)
}

onBeforeUnmount(() => {
  if (scanPollTimer) {
    clearInterval(scanPollTimer)
    scanPollTimer = null
  }
})
</script>

<style scoped>
.profile-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 1rem;
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
</style>
