<template>
  <div class="game-list-row" @click="goToGame">
    <div class="row-img-wrap">
      <img
        :src="game.logo || game.logoUrl || '/game-library/img/default.jpg'"
        :alt="game.name"
        class="row-img"
        loading="lazy"
      />
    </div>
    <div class="row-main">
      <router-link :to="`/game/${game.id}`" class="row-title" @click.stop>{{ game.name }}</router-link>
      <div class="row-meta">
        <Tag :value="game.platform" severity="info" size="small" />
        <Tag :value="game.releaseDate" severity="warn" size="small" />
        <span v-if="game.avgRating" class="row-rating">
          <i class="pi pi-star-fill" style="font-size: 0.7rem;"></i>
          {{ game.avgRating }}
        </span>
      </div>
      <div class="row-genres">
        <Tag v-for="g in game.genres?.slice(0, 4)" :key="g" :value="genreName(g)" severity="secondary" size="small" class="genre-chip" />
        <span v-if="game.genres?.length > 4" class="text-color-secondary text-xs">+{{ game.genres.length - 4 }}</span>
      </div>
      <div v-if="game.tags?.length" class="row-tags">
        <Tag v-for="tag in game.tags.slice(0, 3)" :key="tag" :value="tag" severity="info" size="small" rounded />
        <span v-if="game.tags.length > 3" class="text-color-secondary text-xs">+{{ game.tags.length - 3 }}</span>
      </div>
    </div>
    <div class="row-actions" @click.stop>
      <Button
        :icon="game.favorited ? 'pi pi-heart-fill' : 'pi pi-heart'"
        :severity="game.favorited ? 'danger' : 'secondary'"
        rounded
        text
        size="small"
        @click="toggleFav"
        v-tooltip="game.favorited ? t('filter.favorites_on') : t('filter.favorites_off')"
      />
      <a
        :href="getDownloadUrl(game.id)"
        class="p-button p-button-success p-button-sm p-button-rounded p-button-text"
        v-tooltip="t('game.download_tooltip')"
        @click.stop
      >
        <i class="pi pi-download"></i>
      </a>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useLibraryStore } from '../stores/library'
import { useI18n } from '../composables/useI18n'
import { gamesApi } from '../api/games'
import { useToast } from 'primevue/usetoast'
import Tag from 'primevue/tag'
import Button from 'primevue/button'

const props = defineProps({
  game: { type: Object, required: true }
})

const router = useRouter()
const libraryStore = useLibraryStore()
const { t } = useI18n()
const toast = useToast()

function genreName(code) {
  return libraryStore.genreMap[code] || code
}

function goToGame() {
  router.push(`/game/${props.game.id}`)
}

function getDownloadUrl(id) {
  return gamesApi.getDownloadUrl(id)
}

async function toggleFav() {
  try {
    const res = await gamesApi.toggleFavorite(props.game.id)
    props.game.favorited = res.data.data.favorited
  } catch {
    toast.add({ severity: 'error', summary: t('collections.operation_failed'), life: 3000 })
  }
}
</script>

<style scoped>
.game-list-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s, transform 0.15s;
  border: 1px solid var(--p-surface-200, #e5e7eb);
}
.game-list-row:hover {
  background: var(--p-surface-100);
  transform: translateX(2px);
}
.row-img-wrap {
  flex-shrink: 0;
  width: 48px;
  height: 72px;
  border-radius: 4px;
  overflow: hidden;
}
.row-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.row-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.row-title {
  font-weight: 600;
  font-size: 0.95rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: inherit;
}
.row-title:hover {
  text-decoration: underline;
}
.row-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.row-rating {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.8rem;
  font-weight: 600;
}
.row-genres {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-wrap: wrap;
}
.genre-chip {
  font-size: 0.65rem !important;
  padding: 0.1rem 0.4rem !important;
}
.row-tags {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-wrap: wrap;
}
.row-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-shrink: 0;
}
@media (max-width: 768px) {
  .row-genres,
  .row-tags {
    display: none;
  }
  .row-img-wrap {
    width: 40px;
    height: 60px;
  }
}
</style>
