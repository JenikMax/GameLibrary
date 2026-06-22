<template>
  <div class="library-layout">
    <aside class="filter-sidebar">
      <GameFilter
        :options="store.filterOptions"
        @apply="handleApplyFilters"
        @reset="handleResetFilters"
      />
    </aside>
    <main class="library-main">
      <div class="flex align-items-center justify-content-between mb-3">
        <h2 class="m-0">{{ t('library.title') }}</h2>
        <div class="flex gap-2 align-items-center">
          <Badge :value="store.totalItems" severity="info" />
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

      <ProgressBar v-if="store.loading" mode="indeterminate" class="mb-3" />

      <div v-if="!store.loading && store.games.length === 0" class="text-center p-5">
        <i class="pi pi-info-circle text-6xl text-gray-400"></i>
        <p class="text-xl mt-3">{{ t('library.no_games') }}</p>
      </div>

      <div v-else class="game-grid">
        <GameCard v-for="game in store.games" :key="game.id" :game="game" />
      </div>

      <div class="flex justify-content-center mt-4">
        <Paginator
          :first="(store.currentPage - 1) * store.pageSize"
          :rows="store.pageSize"
          :totalRecords="store.totalItems"
          @page="onPageChange"
        />
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLibraryStore } from '../stores/library'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import { adminApi } from '../api/admin'
import { useToast } from 'primevue/usetoast'
import GameCard from '../components/GameCard.vue'
import GameFilter from '../components/GameFilter.vue'
import Paginator from 'primevue/paginator'
import ProgressBar from 'primevue/progressbar'
import Badge from 'primevue/badge'
import Button from 'primevue/button'

const store = useLibraryStore()
const authStore = useAuthStore()
const { t } = useI18n()
const router = useRouter()
const toast = useToast()
const scanning = ref(false)

onMounted(async () => {
  await store.fetchFilterOptions()
  await store.fetchGames()
})

function onPageChange(event) {
  store.fetchGames(event.page + 1)
}

function handleApplyFilters(filters) {
  store.setSearch(filters.searchText)
  store.setFilters({
    platforms: filters.platforms,
    years: filters.years,
    genres: filters.genres
  })
  store.setSort(filters.sortField, filters.sortType)
  store.fetchGames(1)
}

function handleResetFilters() {
  store.resetFilters()
  store.fetchGames(1)
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
