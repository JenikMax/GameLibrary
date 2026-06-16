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
          <Tag v-for="g in game.genres" :key="g" :value="g" severity="secondary" />
        </div>
        <div v-if="game.trailerUrl" class="mb-3">
          <Button
            label="Watch Trailer"
            icon="pi pi-video"
            severity="info"
            @click="showTrailer = true"
          />
        </div>
        <div class="flex gap-2 mb-3 flex-wrap">
          <Button label="Download" icon="pi pi-download" severity="success" @click="downloadGame" />
          <Button
            label="Seed via aria2"
            icon="pi pi-seed-inverse"
            severity="info"
            @click="seedGame"
            :loading="seeding"
            v-tooltip.bottom="'Create torrent and start seeding on NAS'"
          />
          <Button
            v-if="authStore.isAdmin"
            label="Edit"
            icon="pi pi-pencil"
            severity="help"
            @click="$router.push(`/game/${game.id}/edit`)"
          />
        </div>
        <Divider />
        <h3>Description</h3>
        <p v-html="game.description" class="description-text"></p>
        <Divider v-if="game.instruction" />
        <h3 v-if="game.instruction">Installation Instructions</h3>
        <p v-if="game.instruction" v-html="game.instruction" class="description-text"></p>
      </div>
    </div>

    <Divider v-if="game.screenshotUrls && game.screenshotUrls.length" />
    <div v-if="game.screenshotUrls && game.screenshotUrls.length" class="screenshots-section">
      <h3>Screenshots</h3>
      <Galleria
        :value="game.screenshotUrls"
        :responsiveOptions="responsiveOptions"
        :numVisible="5"
        containerStyle="max-width: 800px"
        :showItemNavigators="true"
        :showThumbnails="false"
        :showItemNavigatorsOnHover="true"
      >
        <template #item="slotProps">
          <Image :src="slotProps.item" alt="screenshot" preview image-class="screenshot-img" />
        </template>
        <template #thumbnail="slotProps">
          <img :src="slotProps.item" alt="thumbnail" style="width:60px;height:40px;object-fit:cover" />
        </template>
      </Galleria>
    </div>

    <Dialog v-model:visible="showTrailer" :header="game.name" modal :style="{ width: '800px' }">
      <div style="position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden;">
        <iframe
          :src="trailerEmbedUrl"
          style="position: absolute; top: 0; left: 0; width: 100%; height: 100%;"
          frameborder="0"
          allowfullscreen
        ></iframe>
      </div>
    </Dialog>
  </div>
  <div v-else class="text-center p-5">
    <i class="pi pi-exclamation-triangle text-6xl"></i>
    <p>Game not found</p>
    <Button label="Back to Library" @click="$router.push('/')" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { gamesApi } from '../api/games'
import ProgressSpinner from 'primevue/progressspinner'
import Image from 'primevue/image'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Divider from 'primevue/divider'
import Galleria from 'primevue/galleria'
import Dialog from 'primevue/dialog'
import { useToast } from 'primevue/usetoast'
import { downloadsApi } from '../api/downloads'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const game = ref(null)
const loading = ref(true)
const showTrailer = ref(false)
const seeding = ref(false)
const toast = useToast()

const responsiveOptions = [
  { breakpoint: '1024px', numVisible: 3 },
  { breakpoint: '768px', numVisible: 2 },
  { breakpoint: '560px', numVisible: 1 }
]

const trailerEmbedUrl = computed(() => {
  if (!game.value?.trailerUrl) return ''
  const url = game.value.trailerUrl
  const match = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([a-zA-Z0-9_-]+)/)
  return match ? `https://www.youtube.com/embed/${match[1]}` : url
})

onMounted(async () => {
  try {
    const response = await gamesApi.getGame(route.params.id)
    game.value = response.data.data
  } finally {
    loading.value = false
  }
})

function downloadGame() {
  window.open(gamesApi.getDownloadUrl(game.value.id), '_blank')
}

async function seedGame() {
  seeding.value = true
  try {
    const res = await downloadsApi.seedGame(game.value.id)
    toast.add({
      severity: 'success',
      summary: 'Seeding started',
      detail: `GID: ${res.data.data.aria2Gid}`,
      life: 5000
    })
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Seeding failed',
      detail: 'aria2 may not be connected. Check Downloads page.',
      life: 5000
    })
  } finally {
    seeding.value = false
  }
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
.screenshots-section {
  text-align: center;
}
.screenshot-img {
  max-height: 500px;
  object-fit: contain;
}
@media (max-width: 768px) {
  .game-main {
    flex-direction: column;
    align-items: center;
  }
}
</style>
