<template>
  <div class="admin-dashboard">
    <h2 class="mb-3">{{ t('admin.dashboard') }}</h2>

    <div class="admin-grid">
      <Card>
        <template #title>
          <div class="flex align-items-center gap-2">
            <i class="pi pi-refresh"></i>
            {{ t('admin.scan_section') }}
          </div>
        </template>
        <template #content>
          <Button
            :label="t('library.scan')"
            icon="pi pi-refresh"
            severity="warning"
            @click="scanLibrary"
            :loading="scanning"
            :disabled="!!scanTaskId"
            class="w-full"
          />
          <div v-if="scanTaskId" class="scan-progress mt-3">
            <div class="flex align-items-center justify-content-between mb-1">
              <small>{{ scanPhaseLabel }}</small>
              <small>{{ scanProgress }}%</small>
            </div>
            <ProgressBar :value="scanProgress" class="mb-1" />
            <small class="text-muted" v-if="scanCurrentGame">{{ scanCurrentGame }}</small>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>
          <div class="flex align-items-center gap-2">
            <i class="pi pi-bolt"></i>
            {{ t('admin.embeddings_section') }}
          </div>
        </template>
        <template #content>
          <div class="flex flex-column gap-2">
            <Button
              :label="t('admin.embeddings_generate')"
              icon="pi pi-bolt"
              severity="info"
              @click="generateEmbeddings(false)"
              :loading="embeddingGenerating"
              :disabled="!!embeddingTaskId"
              class="w-full"
            />
            <Button
              :label="t('admin.embeddings_regenerate')"
              icon="pi pi-refresh"
              severity="danger"
              @click="generateEmbeddings(true)"
              :loading="embeddingGenerating"
              :disabled="!!embeddingTaskId"
              class="w-full"
            />
          </div>
          <div v-if="embeddingTaskId" class="scan-progress mt-3">
            <div class="flex align-items-center justify-content-between mb-1">
              <small>{{ t('admin.embeddings_progress') }}</small>
              <small>{{ embeddingProgress }}%</small>
            </div>
            <ProgressBar :value="embeddingProgress" class="mb-1" />
            <small class="text-muted" v-if="embeddingCurrentGame">{{ embeddingCurrentGame }}</small>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>
          <div class="flex align-items-center gap-2">
            <i class="pi pi-th-large"></i>
            {{ t('admin.quick_links') }}
          </div>
        </template>
        <template #content>
          <div class="flex flex-column gap-2">
            <Button
              :label="t('nav.users')"
              icon="pi pi-users"
              severity="secondary"
              @click="$router.push('/admin/users')"
              class="w-full"
            />
            <Button
              :label="t('nav.scrapers')"
              icon="pi pi-cloud-download"
              severity="secondary"
              @click="$router.push('/admin/scrapers')"
              class="w-full"
            />
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { useI18n } from '../composables/useI18n'
import { adminApi } from '../api/admin'
import Card from 'primevue/card'
import Button from 'primevue/button'
import ProgressBar from 'primevue/progressbar'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const toast = useToast()

const scanning = ref(false)
const scanTaskId = ref(null)
const scanProgress = ref(0)
const scanCurrentGame = ref('')
const scanPhase = ref('')
let scanPollTimer = null

const embeddingTaskId = ref(null)
const embeddingProgress = ref(0)
const embeddingCurrentGame = ref('')
const embeddingGenerating = ref(false)
let embeddingPollTimer = null

const scanPhaseLabel = computed(() => {
  const phaseMap = {
    'PENDING': t('scan.phase_scanning'),
    'SCANNING_DIRS': t('scan.phase_scanning'),
    'STORING_METADATA': t('scan.phase_metadata'),
    'LOADING_IMAGES': t('scan.phase_images'),
    'GENERATING_EMBEDDINGS': t('scan.phase_embeddings'),
    'REFRESHING_SIZES': t('scan.phase_sizes'),
    'COMPLETED': '',
    'FAILED': ''
  }
  return phaseMap[scanPhase.value] || ''
})

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

async function generateEmbeddings(force) {
  embeddingGenerating.value = true
  try {
    const res = await adminApi.generateEmbeddings(force)
    const { taskId } = res.data.data
    embeddingTaskId.value = taskId
    embeddingProgress.value = 0
    embeddingCurrentGame.value = ''
    pollEmbeddingStatus()
  } catch {
    embeddingGenerating.value = false
    toast.add({ severity: 'error', summary: t('admin.embeddings_failed'), life: 3000 })
  }
}

function pollEmbeddingStatus() {
  if (!embeddingTaskId.value) return
  embeddingPollTimer = setInterval(async () => {
    try {
      const res = await adminApi.getEmbeddingStatus(embeddingTaskId.value)
      const task = res.data.data
      embeddingProgress.value = task.progress || 0
      embeddingCurrentGame.value = task.currentGame || ''
      if (task.status === 'COMPLETED') {
        clearInterval(embeddingPollTimer)
        embeddingPollTimer = null
        embeddingTaskId.value = null
        embeddingGenerating.value = false
        toast.add({ severity: 'success', summary: t('admin.embeddings_complete'), life: 3000 })
      } else if (task.status === 'FAILED') {
        clearInterval(embeddingPollTimer)
        embeddingPollTimer = null
        embeddingTaskId.value = null
        embeddingGenerating.value = false
        toast.add({ severity: 'error', summary: t('admin.embeddings_failed'), detail: task.errorMessage || '', life: 5000 })
      }
    } catch {
      clearInterval(embeddingPollTimer)
      embeddingPollTimer = null
      embeddingTaskId.value = null
      embeddingGenerating.value = false
    }
  }, 500)
}

onBeforeUnmount(() => {
  if (scanPollTimer) {
    clearInterval(scanPollTimer)
    scanPollTimer = null
  }
  if (embeddingPollTimer) {
    clearInterval(embeddingPollTimer)
    embeddingPollTimer = null
  }
})
</script>

<style scoped>
.admin-dashboard {
  max-width: 1000px;
  margin: 0 auto;
  padding: 1rem;
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.scan-progress {
  width: 100%;
}
</style>
