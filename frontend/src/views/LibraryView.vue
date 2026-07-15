<template>
  <div class="library-layout">
    <aside class="filter-sidebar">
      <GameFilter
        ref="filterRef"
        :options="store.filterOptions"
        @apply="handleApplyFilters"
        @reset="handleResetFilters"
      />
    </aside>
    <main class="library-main">
      <div class="flex align-items-center justify-content-between mb-3">
        <h2 class="m-0">{{ t('library.title') }}</h2>
        <div class="flex gap-2 align-items-center">
          <Button
            :label="t('library.random')"
            icon="pi pi-shuffle"
            severity="info"
            size="small"
            @click="handleRandom"
          />
          <Button
            v-if="authStore.isAdmin"
            :label="t('library.scan')"
            icon="pi pi-refresh"
            severity="warning"
            size="small"
            @click="handleScan"
            :loading="scanning"
          />
        </div>
      </div>

      <div v-if="history.length > 0" class="mb-3">
        <h4 class="m-0 mb-2">{{ t('history.title') }}</h4>
        <div class="history-strip">
          <div
            v-for="g in history.slice(0, 10)"
            :key="g.id"
            class="history-item"
            @click="router.push(`/game/${g.id}`)"
          >
            <img
              :src="g.logoUrl || '/game-library/api/images/games/' + g.id + '/logo'"
              :alt="g.name"
              class="history-img"
              @error="$event.target.src = '/game-library/img/default.jpg'"
            />
            <span class="history-name">{{ g.name }}</span>
          </div>
        </div>
      </div>

      <div class="sort-bar flex align-items-center gap-2 mb-3">
        <SelectButton
          v-model="store.sortField"
          :options="sortOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          @change="onSortChange"
        />
        <SelectButton
          v-if="store.sortField"
          v-model="store.sortType"
          :options="sortTypeOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          @change="onSortChange"
        />
        <Badge :value="store.totalItems" severity="info" />
      </div>

      <ProgressBar v-if="store.loading" mode="indeterminate" class="mb-3" />

      <div v-if="!store.loading && store.games.length === 0" class="text-center p-5">
        <i class="pi pi-info-circle text-6xl text-gray-400"></i>
        <p class="text-xl mt-3">{{ t('library.no_games') }}</p>
      </div>

      <div v-else-if="store.loading">
        <div class="game-grid">
          <GameCardSkeleton v-for="i in store.pageSize" :key="i" />
        </div>
      </div>
      <div v-else>
        <div v-if="store.totalItems > store.pageSize" class="flex justify-content-center mb-3">
          <Paginator
            :first="(store.currentPage - 1) * store.pageSize"
            :rows="store.pageSize"
            :totalRecords="store.totalItems"
            @page="onPageChange"
          />
        </div>
        <TransitionGroup name="card-list" tag="div" class="game-grid">
          <GameCard v-for="game in store.games" :key="game.id" :game="game" />
        </TransitionGroup>
        <div v-if="store.totalItems > store.pageSize" class="flex justify-content-center mt-4">
          <Paginator
            :first="(store.currentPage - 1) * store.pageSize"
            :rows="store.pageSize"
            :totalRecords="store.totalItems"
            @page="onPageChange"
          />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLibraryStore } from '../stores/library'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import { useViewHistory } from '../composables/useViewHistory'
import { adminApi } from '../api/admin'
import { gamesApi } from '../api/games'
import { useToast } from 'primevue/usetoast'
import GameCard from '../components/GameCard.vue'
import GameCardSkeleton from '../components/GameCardSkeleton.vue'
import GameFilter from '../components/GameFilter.vue'
import Paginator from 'primevue/paginator'
import ProgressBar from 'primevue/progressbar'
import Badge from 'primevue/badge'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'

const LIBRARY_STATE_KEY = 'libraryState'

function saveStateToSession() {
  sessionStorage.setItem(LIBRARY_STATE_KEY, JSON.stringify({
    currentPage: store.currentPage,
    searchText: store.searchText,
    platforms: store.selectedPlatforms,
    years: store.selectedYears,
    genres: store.selectedGenres,
    sortField: store.sortField,
    sortType: store.sortType,
    favoritesOnly: store.favoritesOnly
  }))
}

const store = useLibraryStore()
const authStore = useAuthStore()
const { t } = useI18n()
const { history } = useViewHistory()
const router = useRouter()
const toast = useToast()
const scanning = ref(false)
const filterRef = ref(null)

const sortOptions = [
  { label: t('filter.sort_name'), value: 'name' },
  { label: t('filter.sort_year'), value: 'year' },
  { label: t('filter.sort_date'), value: 'create' },
  { label: t('filter.sort_rating'), value: 'rating' }
]
const sortTypeOptions = [
  { label: t('filter.asc'), value: 'asc' },
  { label: t('filter.desc'), value: 'desc' }
]

onMounted(async () => {
  await store.fetchFilterOptions()
  const saved = sessionStorage.getItem(LIBRARY_STATE_KEY)
  if (saved) {
    const state = JSON.parse(saved)
    store.searchText = state.searchText || ''
    store.selectedPlatforms = state.platforms || []
    store.selectedYears = state.years || []
    store.selectedGenres = state.genres || []
    store.sortField = state.sortField || ''
    store.sortType = state.sortType || ''
    store.favoritesOnly = state.favoritesOnly || false
    filterRef.value?.restoreState({
      searchText: state.searchText || '',
      selectedPlatforms: state.platforms || [],
      selectedYears: state.years || [],
      selectedGenres: state.genres || []
    })
    sessionStorage.removeItem(LIBRARY_STATE_KEY)
    await store.fetchGames(state.currentPage || 1)
  } else {
    store.resetFilters()
    if (router.currentRoute.value.query.favorites === '1') {
      store.favoritesOnly = true
    }
    await store.fetchGames()
  }
})

onBeforeUnmount(() => {
  saveStateToSession()
})

function onPageChange(event) {
  store.fetchGames(event.page + 1)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function onSortChange() {
  store.fetchGames(1)
}

function handleApplyFilters(filters) {
  store.setSearch(filters.searchText)
  store.setFilters({
    platforms: filters.platforms,
    years: filters.years,
    genres: filters.genres
  })
  store.fetchGames(1)
}

function handleResetFilters() {
  store.resetFilters()
  if (router.currentRoute.value.query.favorites === '1') {
    store.favoritesOnly = true
  }
  sessionStorage.removeItem(LIBRARY_STATE_KEY)
  store.fetchGames(1)
}

async function handleRandom() {
  try {
    const res = await gamesApi.getRandomGame()
    if (res.data.data) {
      router.push(`/game/${res.data.data.id}`)
    } else {
      toast.add({ severity: 'warn', summary: t('library.random_failed'), life: 3000 })
    }
  } catch {
    toast.add({ severity: 'error', summary: t('library.random_failed'), life: 3000 })
  }
}

async function handleScan() {
  scanning.value = true
  try {
    await adminApi.scanLibrary()
    toast.add({ severity: 'success', summary: t('library.scan_complete'), life: 3000 })
    await store.fetchGames()
  } catch {
    toast.add({ severity: 'error', summary: t('library.scan_failed'), life: 3000 })
  } finally {
    scanning.value = false
  }
}
</script>

<style scoped>
.library-layout {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  max-width: 1400px;
  margin: 0 auto;
}
.filter-sidebar {
  width: 300px;
  flex-shrink: 0;
}
.library-main {
  flex: 1;
  min-width: 0;
}
.game-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
}
.history-strip {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
  padding-bottom: 0.25rem;
}
.history-item {
  flex-shrink: 0;
  width: 80px;
  cursor: pointer;
  text-align: center;
  transition: transform 0.15s;
}
.history-item:hover {
  transform: translateY(-2px);
}
.history-img {
  width: 80px;
  height: 120px;
  object-fit: cover;
  border-radius: 4px;
  display: block;
}
.history-name {
  display: block;
  font-size: 0.65rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 0.15rem;
}
@media (max-width: 768px) {
  .library-layout {
    flex-direction: column;
  }
  .filter-sidebar {
    width: 100%;
  }
  .game-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}
</style>
