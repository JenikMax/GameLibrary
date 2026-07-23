<template>
  <div
    class="collection-card border-round overflow-hidden surface-card cursor-pointer transition-all"
    @click="router.push(`/collections/${collection.id}`)"
  >
    <div class="fade-bg" :style="heroStyle"></div>
    <div class="fade-grad"></div>
    <div class="fade-info">
      <div class="flex align-items-center gap-2 mb-1">
        <h3 class="fade-title m-0">{{ collection.name }}</h3>
        <Tag v-if="collection.isSmart" :value="t('collections.smart')" severity="info" size="small" rounded />
      </div>
      <p v-if="collection.description" class="fade-desc line-clamp-1">{{ collection.description }}</p>
      <span class="fade-count">{{ t('collections.game_count', { n: collection.gameCount }) }}</span>
      <div class="fade-covers">
        <div
          v-for="pg in fadeGames"
          :key="pg.gameId"
          class="fade-thumb"
        >
          <img
            :src="`/game-library/api/images/games/${pg.gameId}/logo`"
            :alt="pg.name"
            class="fade-thumb-img"
            @error="onImgError"
          />
        </div>
        <div v-if="collection.overflow > 0" class="fade-ovfl">
          +{{ collection.overflow }}
        </div>
      </div>
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

const fadeGames = computed(() => {
  if (!props.collection.previewGames) return []
  return props.collection.previewGames.slice(0, 4)
})

function onImgError(e) {
  e.target.src = '/game-library/img/default.jpg'
}
</script>

<style scoped>
/* ===== STRUCTURE ONLY ===== */
.collection-card {
  aspect-ratio: 2 / 3;
  position: relative;
  transition: transform 0.2s, box-shadow 0.2s;
}
.collection-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
}

.fade-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  z-index: 0;
}

.fade-grad {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.fade-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 2;
  padding: 14px;
}

.fade-title {
  font-size: 1rem;
  font-weight: 700;
}

.fade-desc {
  font-size: 0.78rem;
  line-height: 1.3;
}

.fade-count {
  font-size: 0.72rem;
  display: block;
  margin-bottom: 6px;
}

.fade-covers {
  display: flex;
  gap: 4px;
}

.fade-thumb {
  width: 34px;
  height: 45px;
  overflow: hidden;
  flex-shrink: 0;
}

.fade-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.fade-ovfl {
  width: 34px;
  height: 45px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  font-weight: 700;
}

.line-clamp-1 {
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}
</style>

<style>
/* ===== LIGHT / DARK DEFAULT ===== */
.fade-grad {
  background: linear-gradient(transparent 50%, #1e1e22 85%, #1e1e22 100%);
}

.fade-title {
  color: #fff;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}

.fade-desc {
  color: rgba(255, 255, 255, 0.85);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.6);
}

.fade-count {
  color: #aaa;
}

.fade-thumb {
  background: var(--p-surface-700);
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
}

.fade-ovfl {
  background: rgba(255, 255, 255, 0.08);
  color: #888;
  border-radius: 4px;
}
</style>
