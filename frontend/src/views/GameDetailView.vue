<template>
  <div v-if="loading" class="flex justify-content-center p-5">
    <ProgressSpinner />
  </div>
  <div v-else-if="game" class="game-detail-container">
    <div class="game-main">
      <div class="game-poster">
        <Image
          :src="game.logoUrl || '/game-library/img/default.jpg'"
          :alt="game.name"
          width="300"
          preview
        />
      </div>
      <div class="game-info">
        <h1>{{ game.name }}</h1>
        <div class="flex gap-2 flex-wrap mb-3">
          <Tag :value="game.platform" severity="info" />
          <Tag :value="game.releaseDate" severity="warn" />
          <Tag v-for="g in game.genres" :key="g" :value="genreName(g)" severity="secondary" />
        </div>
        <div class="flex gap-2 mb-3 flex-wrap">
          <Button :label="t('game.download')" icon="pi pi-download" severity="success" @click="downloadGame" />
          <Button
            :label="t('game.seed')"
            icon="pi pi-seed-inverse"
            severity="info"
            @click="seedGame"
            :loading="seeding"
            v-tooltip.bottom="t('game.seed_tooltip')"
          />
          <Button
            v-if="authStore.isAdmin"
            :label="t('game.edit')"
            icon="pi pi-pencil"
            severity="help"
            @click="$router.push(`/game/${game.id}/edit`)"
          />
        </div>
        <div v-if="seedTaskId || seeding" class="seed-progress-section mb-3">
          <ProgressBar :value="seedProgress" class="mb-1" />
          <small class="text-muted" v-if="seedCurrentFile">{{ seedCurrentFile }}</small>
          <small class="text-muted" v-else>{{ t('game.seeding_started') }}</small>
        </div>
        <Divider />
        <h3>{{ t('game.description') }}</h3>
        <p v-html="game.description" class="description-text"></p>
        <div v-if="trailerEmbedUrl" class="video-wrapper">
          <iframe :src="trailerEmbedUrl" frameborder="0" allowfullscreen></iframe>
        </div>
        <Divider v-if="game.instruction" />
        <h3 v-if="game.instruction">{{ t('game.instructions') }}</h3>
        <p v-if="game.instruction" v-html="game.instruction" class="description-text"></p>
      </div>
    </div>

    <Divider v-if="game.screenshotUrls?.length" />
    <div v-if="game.screenshotUrls?.length" class="screenshots-section">
      <h3>{{ t('game.screenshots') }}</h3>
      <div class="screenshot-grid">
        <img
          v-for="(url, i) in game.screenshotUrls"
          :key="i"
          :src="url"
          alt="screenshot"
          class="screenshot-thumb"
          @click="openGallery(i)"
        />
      </div>
    </div>

    <Dialog v-model:visible="viewerVisible" modal :style="{ width: '90vw', maxWidth: '1000px' }"
      :header="screenshotHeader"
      :dismissableMask="true"
    >
      <div class="viewer-body" @keydown="onViewerKeydown" tabindex="0" ref="viewerRef">
        <Button icon="pi pi-chevron-left" text severity="secondary" rounded
          class="nav-btn" @click="prevImage" :disabled="viewerIndex <= 0" />
        <div class="viewer-image-wrap">
          <img :src="game.screenshotUrls[viewerIndex]" class="viewer-image" alt="screenshot" />
        </div>
        <Button icon="pi pi-chevron-right" text severity="secondary" rounded
          class="nav-btn" @click="nextImage" :disabled="viewerIndex >= game.screenshotUrls.length - 1" />
      </div>
    </Dialog>
  </div>
  <div v-else class="text-center p-5">
    <i class="pi pi-exclamation-triangle text-6xl"></i>
    <p>{{ t('game.not_found') }}</p>
    <Button :label="t('game.back_to_library')" @click="$router.push('/')" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useLibraryStore } from '../stores/library'
import { useI18n } from '../composables/useI18n'
import { gamesApi } from '../api/games'
import ProgressSpinner from 'primevue/progressspinner'
import ProgressBar from 'primevue/progressbar'
import Image from 'primevue/image'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Divider from 'primevue/divider'
import Dialog from 'primevue/dialog'
import { useToast } from 'primevue/usetoast'
import { downloadsApi } from '../api/downloads'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const libraryStore = useLibraryStore()
const { t } = useI18n()

const game = ref(null)
const loading = ref(true)
const seeding = ref(false)
const seedTaskId = ref(null)
const seedProgress = ref(0)
const seedCurrentFile = ref('')
let seedPollTimer = null
const toast = useToast()
const viewerVisible = ref(false)
const viewerIndex = ref(0)
const viewerRef = ref(null)

function genreName(code) {
  return libraryStore.genreMap[code] || code
}

const screenshotHeader = computed(() => {
  if (!game.value?.screenshotUrls) return ''
  return t('game.screenshots') + ' ' + (viewerIndex.value + 1) + ' / ' + game.value.screenshotUrls.length
})

watch(viewerVisible, (val) => {
  if (val) nextTick(() => viewerRef.value?.focus())
})

const trailerEmbedUrl = computed(() => {
  if (!game.value?.trailerUrl) return ''
  const url = game.value.trailerUrl
  const match = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([a-zA-Z0-9_-]+)/)
  return match ? `https://www.youtube.com/embed/${match[1]}` : (url && url !== 'N/A' ? url : '')
})

onMounted(async () => {
  try {
    const [gameRes] = await Promise.all([
      gamesApi.getGame(route.params.id),
      libraryStore.filterOptions.genres?.length ? Promise.resolve() : libraryStore.fetchFilterOptions()
    ])
    game.value = gameRes.data.data
  } finally {
    loading.value = false
  }
})

function downloadGame() {
  window.open(gamesApi.getDownloadUrl(game.value.id), '_blank')
}

function pollSeedStatus() {
  if (!seedTaskId.value) return
  seedPollTimer = setInterval(async () => {
    try {
      const res = await downloadsApi.getSeedStatus(seedTaskId.value)
      const task = res.data.data
      if (task.status === 'COMPLETED') {
        clearInterval(seedPollTimer)
        seedPollTimer = null
        seeding.value = false
        seedTaskId.value = null
        toast.add({
          severity: 'success',
          summary: t('game.seeding_started'),
          detail: `ID: ${task.seedId}`,
          life: 5000
        })
      } else if (task.status === 'FAILED') {
        clearInterval(seedPollTimer)
        seedPollTimer = null
        seeding.value = false
        seedTaskId.value = null
        toast.add({
          severity: 'error',
          summary: t('game.seeding_failed'),
          detail: task.errorMessage || t('game.seeding_failed_detail'),
          life: 5000
        })
      } else {
        seedProgress.value = task.progress || 0
        seedCurrentFile.value = task.currentFile || ''
      }
    } catch {
      clearInterval(seedPollTimer)
      seedPollTimer = null
      seeding.value = false
      seedTaskId.value = null
    }
  }, 1000)
}

async function seedGame() {
  seeding.value = true
  seedProgress.value = 0
  seedCurrentFile.value = ''
  try {
    const res = await downloadsApi.seedGame(game.value.id)
    seedTaskId.value = res.data.data.taskId
    pollSeedStatus()
  } catch {
    seeding.value = false
    toast.add({
      severity: 'error',
      summary: t('game.seeding_failed'),
      detail: t('game.seeding_failed_detail'),
      life: 5000
    })
  }
}

onUnmounted(() => {
  if (seedPollTimer) {
    clearInterval(seedPollTimer)
    seedPollTimer = null
  }
})

function openGallery(index) {
  viewerIndex.value = index
  viewerVisible.value = true
}

function prevImage() {
  if (viewerIndex.value > 0) viewerIndex.value--
}

function nextImage() {
  if (viewerIndex.value < game.value.screenshotUrls.length - 1) viewerIndex.value++
}

function onViewerKeydown(e) {
  if (e.key === 'ArrowLeft') { prevImage(); e.preventDefault() }
  if (e.key === 'ArrowRight') { nextImage(); e.preventDefault() }
}
</script>

<style scoped>
.game-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}
.game-main {
  display: flex;
  gap: 2rem;
}
.game-poster {
  flex-shrink: 0;
}
.game-info {
  flex: 1;
}
.description-text {
  line-height: 1.6;
  white-space: pre-wrap;
}
.description-text :deep(ol) {
  padding-left: 1.5rem;
  margin-bottom: 0.5rem;
  list-style-type: none;
}
.description-text :deep(li[data-list="ordered"]) {
  list-style-type: decimal;
}
.description-text :deep(li[data-list="bullet"]) {
  list-style-type: disc;
}
.screenshots-section {
  margin-top: 1rem;
}
.screenshot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 0.5rem;
  margin-top: 1rem;
}
.screenshot-thumb {
  width: 100%;
  height: 150px;
  object-fit: cover;
  cursor: pointer;
  border-radius: 6px;
  transition: transform 0.2s;
}
.screenshot-thumb:hover {
  transform: scale(1.03);
}
.viewer-body {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  outline: none;
}
.viewer-image-wrap {
  flex: 1;
  display: flex;
  justify-content: center;
}
.viewer-image {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 4px;
}
.nav-btn {
  flex-shrink: 0;
}
.seed-progress-section {
  max-width: 400px;
}
.video-wrapper {
  position: relative;
  padding-bottom: 56.25%;
  height: 0;
  overflow: hidden;
  max-width: 800px;
  margin-bottom: 1rem;
  border-radius: 8px;
}
.video-wrapper iframe {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
@media (max-width: 768px) {
  .game-main {
    flex-direction: column;
    align-items: center;
  }
}
</style>
