<template>
  <div
    class="collection-card border-round overflow-hidden surface-card cursor-pointer transition-all"
    @click="router.push(`/collections/${collection.id}`)"
  >
    <div class="hero-section" :style="heroStyle">
      <div class="hero-overlay">
        <div class="hero-content">
          <div class="flex align-items-center gap-2 mb-1">
            <h3 class="hero-title m-0">{{ collection.name }}</h3>
            <Tag v-if="collection.isSmart" :value="t('collections.smart')" severity="info" size="small" rounded />
          </div>
          <p v-if="collection.description" class="hero-desc line-clamp-2">
            {{ collection.description }}
          </p>
          <span class="game-count-badge">
            {{ t('collections.game_count', { n: collection.gameCount }) }}
          </span>
        </div>
      </div>
    </div>

    <div class="preview-strip">
      <div
        v-if="!collection.previewGames || collection.previewGames.length === 0"
        class="preview-empty"
      >
        <i class="pi pi-folder" style="font-size: 1.5rem; opacity: 0.4" />
        <span>{{ t('collections.no_games_yet') }}</span>
      </div>
      <template v-else>
        <div v-for="(pg, index) in collection.previewGames" :key="pg.gameId" class="preview-item"
             :style="{ zIndex: collection.previewGames.length - index }">
          <img
            :src="`/game-library/api/images/games/${pg.gameId}/logo`"
            :alt="pg.name"
            class="preview-thumb"
            @error="onImgError"
          />
        </div>
        <div v-if="collection.overflow > 0" class="preview-overflow">
          <div class="overflow-inner">
            <span class="overflow-text">+{{ collection.overflow }}</span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '../composables/useI18n'
import Tag from 'primevue/tag'

const props = defineProps({
  collection: { type: Object, required: true }
})

const { t } = useI18n()
const router = useRouter()

const heroStyle = computed(() => {
  const img = props.collection.heroGameId
    ? `/game-library/api/images/games/${props.collection.heroGameId}/logo`
    : '/game-library/img/default.jpg'
  return { backgroundImage: `url(${img})` }
})

function onImgError(e) {
  e.target.src = '/game-library/img/default.jpg'
}
</script>

<style scoped>
.collection-card {
  aspect-ratio: 2 / 3;
  transition: transform 0.2s, box-shadow 0.2s;
}
.collection-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.hero-section {
  height: 80%;
  min-height: 200px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(transparent 30%, rgba(0, 0, 0, 0.9) 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 1.25rem;
}

.hero-title {
  margin: 0 0 0.3rem;
  font-size: 1.3rem;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.7);
}

.hero-desc {
  margin: 0 0 0.6rem;
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.85);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.6);
  line-height: 1.3;
}

.game-count-badge {
  display: inline-block;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 0.75rem;
  padding: 3px 12px;
  border-radius: 12px;
  width: fit-content;
}

.preview-strip {
  height: 20%;
  display: flex;
  align-items: flex-end;
  padding: 14px;
  overflow-x: hidden;
  background: var(--p-surface-800);
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  gap: 6px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.85rem;
}

.preview-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  position: relative;
}

.preview-item + .preview-item {
  margin-left: -19px;
}

.preview-thumb {
  width: 55px;
  height: 73px;
  object-fit: cover;
  border-radius: 6px;
  background: var(--p-surface-700);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
  border: 1.5px solid rgba(255, 255, 255, 0.3);
}

.preview-overflow {
  flex-shrink: 0;
  margin-left: -19px;
}

.overflow-inner {
  width: 55px;
  height: 73px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.overflow-text {
  color: #fff;
  font-size: 1rem;
  font-weight: 700;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
