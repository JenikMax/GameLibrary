<template>
  <Card class="game-card">
    <template #header>
      <div class="game-card-img-wrapper" @click="goToGame">
        <img
          :src="game.logo || game.logoUrl || '/game-library/img/default.jpg'"
          :alt="game.name"
          class="game-card-img"
        />
      </div>
    </template>
    <template #title>
      <router-link :to="`/game/${game.id}`" class="game-title">{{ game.name }}</router-link>
    </template>
    <template #subtitle>
      <div class="flex gap-2 align-items-center">
        <Tag :value="game.platform" severity="info" />
        <Tag :value="game.releaseDate" severity="warn" />
      </div>
    </template>
    <template #content>
      <div class="flex flex-wrap gap-1">
        <Tag
          v-for="genre in game.genres"
          :key="genre"
          :value="genreName(genre)"
          severity="secondary"
          rounded
          class="genre-tag"
        />
      </div>
    </template>
    <template #footer>
      <div class="flex gap-2 justify-content-between">
        <Button
          icon="pi pi-info-circle"
          :label="t('game.details')"
          severity="help"
          size="small"
          v-tooltip.left="t('game.details_tooltip')"
          @click="goToGame"
        />
        <Button
          icon="pi pi-download"
          severity="success"
          size="small"
          v-tooltip.left="t('game.download_tooltip')"
          @click="downloadGame"
        />
      </div>
    </template>
  </Card>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '../composables/useI18n'
import { useLibraryStore } from '../stores/library'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { gamesApi } from '../api/games'

const props = defineProps({
  game: { type: Object, required: true }
})

const router = useRouter()
const { t } = useI18n()
const libraryStore = useLibraryStore()

onMounted(() => {
  if (!libraryStore.filterOptions.genres?.length) {
    libraryStore.fetchFilterOptions()
  }
})

function genreName(code) {
  return libraryStore.genreMap[code] || code
}

function goToGame() {
  router.push(`/game/${props.game.id}`)
}

function downloadGame() {
  window.open(gamesApi.getDownloadUrl(props.game.id), '_blank')
}
</script>

<style scoped>
.game-card {
  cursor: default;
  transition: transform 0.2s, box-shadow 0.2s;
}
.game-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.game-card-img-wrapper {
  overflow: hidden;
  cursor: pointer;
  border-radius: 6px 6px 0 0;
  aspect-ratio: 2 / 3;
}
.game-card-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.game-card-img-wrapper:hover .game-card-img {
  transform: scale(1.05);
}
.game-title {
  font-size: 1rem;
  font-weight: 600;
  text-decoration: none;
}
.genre-tag {
  font-size: 0.75rem;
}
</style>
