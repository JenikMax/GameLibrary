<template>
  <div v-if="loading" class="game-detail-container">
    <div class="game-main">
      <div class="game-poster">
        <Skeleton width="300px" height="450px" />
      </div>
      <div class="game-info">
        <Skeleton width="60%" height="2rem" class="mb-3" />
        <Skeleton width="30%" height="1.5rem" class="mb-3" />
        <Skeleton width="100%" height="1rem" class="mb-2" />
        <Skeleton width="100%" height="1rem" class="mb-2" />
        <Skeleton width="80%" height="1rem" class="mb-2" />
      </div>
    </div>
  </div>
  <div v-else-if="game" class="game-detail-container">
    <div class="game-main">
      <div class="game-poster poster-fade">
        <Image
          :src="game.logo || game.logoUrl || '/game-library/img/default.jpg'"
          :alt="game.name"
          width="300"
          preview
        />
      </div>
      <div class="game-info">
        <div class="flex align-items-center gap-2 mb-2">
          <Button icon="pi pi-arrow-left" text @click="$router.push('/')" />
          <h1 class="m-0">{{ game.name }}</h1>
          <Button
            v-if="authStore.isAuthenticated"
            :icon="game.favorited ? 'pi pi-heart-fill' : 'pi pi-heart'"
            :severity="game.favorited ? 'danger' : 'secondary'"
            rounded
            text
            @click="toggleFav"
          />
        </div>
        <div class="flex gap-2 flex-wrap mb-3">
          <Tag :value="game.platform" severity="info" />
          <Tag :value="game.releaseDate" severity="warn" />
          <Tag v-for="g in game.genres" :key="g" :value="genreName(g)" severity="secondary" />
        </div>
        <div class="flex align-items-center gap-3 mb-3">
          <div class="flex align-items-center gap-1">
            <Rating :modelValue="game.avgRating || 0" :stars="10" :cancel="false" readonly />
            <span v-if="game.avgRating" class="text-sm font-semibold">{{ game.avgRating }}</span>
            <small class="text-color-secondary">({{ game.ratingsCount || 0 }})</small>
          </div>
          <div v-if="authStore.isAuthenticated" class="flex align-items-center gap-1">
            <span class="text-sm">{{ t('rating.my_rating') }}:</span>
            <Rating :modelValue="userRating" :stars="10" @update:modelValue="saveRating" />
          </div>
          <small v-else class="text-color-secondary">{{ t('rating.login_to_rate') }}</small>
        </div>
        <div class="flex gap-2 mb-3 flex-wrap">
          <Button :label="t('game.download')" icon="pi pi-download" severity="success"
            @click="downloadGame" :loading="preparing" />
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
        <div v-if="preparing" class="seed-progress-section mb-3">
          <ProgressBar :value="prepareProgress" class="mb-1" />
          <small class="text-muted" v-if="prepareCurrentFile">{{ prepareCurrentFile }}</small>
          <small class="text-muted" v-else>{{ t('game.download_preparing') }}</small>
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
          class="screenshot-thumb img-fade"
          :class="{ loaded: screenshotLoaded[i] }"
          @click="openGallery(i)"
          @load="screenshotLoaded[i] = true"
          @error="screenshotLoaded[i] = true"
        />
      </div>
    </div>

    <Divider />
    <div class="comments-section">
      <h3>{{ t('game.comments') }}</h3>

      <div v-if="authStore.isAuthenticated" class="flex gap-2 mb-3">
        <Textarea
          v-model="newCommentText"
          :placeholder="t('game.comment_placeholder')"
          rows="2"
          class="w-full"
          autoResize
        />
        <Button
          :label="t('game.comment_add')"
          icon="pi pi-send"
          severity="primary"
          @click="submitComment"
          :disabled="!newCommentText.trim()"
          class="comment-submit-btn"
        />
      </div>
      <small v-else class="text-color-secondary">{{ t('game.comment_login') }}</small>

      <div v-if="comments.length === 0 && !commentsLoading" class="text-color-secondary text-sm">
        {{ t('game.comment_empty') }}
      </div>
      <div v-else-if="commentsLoading" class="flex flex-column gap-2">
        <Skeleton v-for="i in 3" :key="i" width="100%" height="4rem" />
      </div>
      <div v-else class="comments-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <Avatar :label="comment.username?.[0]?.toUpperCase()" size="small" shape="circle" />
            <span class="font-semibold text-sm">{{ comment.username }}</span>
            <small class="text-color-secondary">{{ comment.createdAt }}</small>
          </div>
          <p class="comment-text">{{ comment.text }}</p>
          <Button
            v-if="comment.canDelete"
            :label="t('game.comment_delete')"
            icon="pi pi-trash"
            size="small"
            severity="danger"
            text
            @click="deleteComment(comment.id)"
          />
        </div>
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
          <img :src="game.screenshotUrls[viewerIndex]" class="viewer-image viewer-fade" :key="viewerIndex" alt="screenshot" />
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
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useLibraryStore } from '../stores/library'
import { useI18n } from '../composables/useI18n'
import { gamesApi } from '../api/games'
import Skeleton from 'primevue/skeleton'
import ProgressBar from 'primevue/progressbar'
import Image from 'primevue/image'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Divider from 'primevue/divider'
import Dialog from 'primevue/dialog'
import Rating from 'primevue/rating'
import Textarea from 'primevue/textarea'
import Avatar from 'primevue/avatar'
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
const preparing = ref(false)
const prepareTaskId = ref(null)
const prepareProgress = ref(0)
const prepareCurrentFile = ref('')
let preparePollTimer = null
let isUnmounted = false
const toast = useToast()
const viewerVisible = ref(false)
const viewerIndex = ref(0)
const viewerRef = ref(null)
const screenshotLoaded = reactive({})
const userRating = ref(0)
const comments = ref([])
const commentsLoading = ref(false)
const newCommentText = ref('')

async function toggleFav() {
  try {
    const res = await gamesApi.toggleFavorite(game.value.id)
    game.value.favorited = res.data.data.favorited
  } catch {
    toast.add({ severity: 'error', summary: t('filter.favorites_off'), life: 3000 })
  }
}

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

async function loadComments() {
  commentsLoading.value = true
  try {
    const res = await gamesApi.getComments(route.params.id)
    comments.value = res.data.data || []
  } finally {
    commentsLoading.value = false
  }
}

onMounted(async () => {
  try {
    const [gameRes] = await Promise.all([
      gamesApi.getGame(route.params.id),
      libraryStore.filterOptions.genres?.length ? Promise.resolve() : libraryStore.fetchFilterOptions(),
      loadComments()
    ])
    game.value = gameRes.data.data
    userRating.value = game.value.userRating || 0
  } finally {
    loading.value = false
  }
})

async function downloadGame() {
  if (!game.value || preparing.value) return

  const infoRes = await gamesApi.getDownloadInfo(game.value.id)
  const info = infoRes.data.data

  const TORRENT_THRESHOLD = 5 * 1024 * 1024 * 1024

  if (info.gameSize < TORRENT_THRESHOLD) {
    window.open(gamesApi.getDownloadUrl(game.value.id), '_blank')
    return
  }

  if (info.torrentCached) {
    window.open(gamesApi.getDownloadUrl(game.value.id), '_blank')
    return
  }

  preparing.value = true
  prepareProgress.value = 0
  prepareCurrentFile.value = ''
  try {
    const res = await downloadsApi.prepareDownload(game.value.id)
    if (isUnmounted) return
    prepareTaskId.value = res.data.data.taskId
    if (!isUnmounted) pollPrepareStatus()
  } catch {
    preparing.value = false
    toast.add({
      severity: 'error',
      summary: t('game.download_failed'),
      life: 5000
    })
  }
}

function downloadFile(url) {
  const a = document.createElement('a')
  a.href = url
  a.download = ''
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

function pollPrepareStatus() {
  if (!prepareTaskId.value) return
  preparePollTimer = setInterval(async () => {
    if (isUnmounted) {
      clearInterval(preparePollTimer)
      preparePollTimer = null
      return
    }
    try {
      const res = await downloadsApi.getPrepareStatus(prepareTaskId.value)
      if (isUnmounted) {
        clearInterval(preparePollTimer)
        preparePollTimer = null
        return
      }
      const task = res.data.data
      if (task.status === 'COMPLETED') {
        clearInterval(preparePollTimer)
        preparePollTimer = null
        preparing.value = false
        prepareTaskId.value = null
        toast.add({
          severity: 'success',
          summary: t('game.seeding_started'),
          detail: task.seedId ? `ID: ${task.seedId}` : '',
          life: 5000
        })
        downloadFile(gamesApi.getDownloadUrl(game.value.id))
      } else if (task.status === 'FAILED') {
        clearInterval(preparePollTimer)
        preparePollTimer = null
        preparing.value = false
        prepareTaskId.value = null
        toast.add({
          severity: 'error',
          summary: t('game.download_failed'),
          detail: task.errorMessage || '',
          life: 5000
        })
      } else {
        prepareProgress.value = task.progress || 0
        prepareCurrentFile.value = task.currentFile || ''
      }
    } catch {
      clearInterval(preparePollTimer)
      preparePollTimer = null
      preparing.value = false
      prepareTaskId.value = null
    }
  }, 1000)
}

function pollSeedStatus() {
  if (!seedTaskId.value) return
  seedPollTimer = setInterval(async () => {
    if (isUnmounted) {
      clearInterval(seedPollTimer)
      seedPollTimer = null
      return
    }
    try {
      const res = await downloadsApi.getSeedStatus(seedTaskId.value)
      if (isUnmounted) {
        clearInterval(seedPollTimer)
        seedPollTimer = null
        return
      }
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
    if (isUnmounted) return
    seedTaskId.value = res.data.data.taskId
    if (!isUnmounted) pollSeedStatus()
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
  isUnmounted = true
  if (seedPollTimer) {
    clearInterval(seedPollTimer)
    seedPollTimer = null
  }
  if (preparePollTimer) {
    clearInterval(preparePollTimer)
    preparePollTimer = null
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

async function submitComment() {
  const text = newCommentText.value.trim()
  if (!text) return
  try {
    const res = await gamesApi.addComment(route.params.id, text)
    comments.value.unshift(res.data.data)
    newCommentText.value = ''
  } catch {
    toast.add({ severity: 'error', summary: t('game.comment_add'), life: 3000 })
  }
}

async function deleteComment(commentId) {
  try {
    await gamesApi.deleteComment(route.params.id, commentId)
    comments.value = comments.value.filter(c => c.id !== commentId)
  } catch {
    toast.add({ severity: 'error', summary: t('game.comment_delete'), life: 3000 })
  }
}

async function saveRating(val) {
  userRating.value = val
  try {
    const res = await gamesApi.saveRating(game.value.id, val)
    game.value.avgRating = res.data.data.avgRating
    game.value.ratingsCount = res.data.data.ratingsCount
  } catch {
    userRating.value = game.value.userRating || 0
    toast.add({ severity: 'error', summary: t('rating.rate'), life: 3000 })
  }
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
.poster-fade {
  animation: posterEnter 0.4s ease-out;
}
@keyframes posterEnter {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
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
.viewer-fade {
  animation: viewerEnter 0.25s ease-out;
}
@keyframes viewerEnter {
  from { opacity: 0; }
  to { opacity: 1; }
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
.comments-section {
  margin-top: 1rem;
}
.comment-submit-btn {
  align-self: flex-end;
}
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.comment-item {
  background: var(--p-surface-100);
  border-radius: 8px;
  padding: 0.75rem;
}
.dark .comment-item {
  background: var(--p-surface-800);
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}
.comment-text {
  margin: 0.25rem 0;
  white-space: pre-wrap;
  word-break: break-word;
}
@media (max-width: 768px) {
  .game-main {
    flex-direction: column;
    align-items: center;
  }
}
</style>
