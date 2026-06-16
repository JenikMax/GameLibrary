<template>
  <Card class="game-card">
    <template #header>
      <div class="game-card-img-wrapper" @click="goToGame">
        <Image
          :src="game.logoUrl || '/game-library/img/default.jpg'"
          :alt="game.name"
          class="game-card-img"
          image-class="game-card-img"
          preview
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
          :value="genre"
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
          label="Details"
          severity="help"
          size="small"
          @click="goToGame"
        />
        <Button
          icon="pi pi-download"
          severity="success"
          size="small"
          @click="downloadGame"
        />
      </div>
    </template>
  </Card>
</template>

<script setup>
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Image from 'primevue/image'
import { gamesApi } from '../api/games'

const props = defineProps({
  game: { type: Object, required: true }
})

const router = useRouter()

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
}
.game-card-img {
  width: 100%;
  height: 200px;
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
