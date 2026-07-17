<template>
  <div class="statistics-page">
    <h2 class="mb-3">{{ t('statistics.title') }}</h2>

    <div class="flex justify-content-end mb-3">
      <Button v-if="authStore.isAdmin" :label="t('statistics.refresh_sizes')" icon="pi pi-refresh" severity="secondary" size="small"
        :loading="refreshing" @click="handleRefreshSizes" />
    </div>

    <div v-if="loading" class="flex justify-content-center p-5">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem" />
    </div>

    <template v-else-if="stats">
      <div class="stats-cards grid mb-3">
        <div class="col-12 md:col-3">
          <div class="stat-card surface-card border-1 border-round p-3 text-center">
            <i class="pi pi-shopping-bag text-3xl mb-2" style="color: var(--p-primary-color)" />
            <div class="text-2xl font-bold">{{ stats.totalGames }}</div>
            <div class="text-sm text-color-secondary">{{ t('statistics.total_games') }}</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="stat-card surface-card border-1 border-round p-3 text-center">
            <i class="pi pi-database text-3xl mb-2" style="color: var(--p-orange-500)" />
            <div class="text-2xl font-bold">{{ formatSize(stats.totalSizeBytes) }}</div>
            <div class="text-sm text-color-secondary">{{ t('statistics.total_size') }}</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="stat-card surface-card border-1 border-round p-3 text-center">
            <i class="pi pi-calendar-plus text-3xl mb-2" style="color: var(--p-green-500)" />
            <div class="text-2xl font-bold">{{ stats.recentAdditions }}</div>
            <div class="text-sm text-color-secondary">{{ t('statistics.recent_additions') }}</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="stat-card surface-card border-1 border-round p-3 text-center">
            <i class="pi pi-star text-3xl mb-2" style="color: var(--p-yellow-500)" />
            <div class="text-2xl font-bold">{{ stats.averageRating }}</div>
            <div class="text-sm text-color-secondary">{{ t('statistics.avg_rating') }}</div>
          </div>
        </div>
      </div>

      <div class="grid mb-3">
        <div class="col-12 md:col-6">
          <div class="surface-card border-1 border-round p-3">
            <h3 class="m-0 mb-2 text-base">{{ t('statistics.by_platform') }}</h3>
            <Bar v-if="platformChartData" :data="platformChartData" :options="chartOptions" />
            <div v-else class="text-center text-color-secondary p-3">{{ t('statistics.no_data') }}</div>
          </div>
        </div>
        <div class="col-12 md:col-6">
          <div class="surface-card border-1 border-round p-3">
            <h3 class="m-0 mb-2 text-base">{{ t('statistics.by_genre') }}</h3>
            <Pie v-if="genreChartData" :data="genreChartData" :options="pieOptions" />
            <div v-else class="text-center text-color-secondary p-3">{{ t('statistics.no_data') }}</div>
          </div>
        </div>
      </div>

      <div class="grid mb-3">
        <div class="col-12 md:col-6">
          <div class="surface-card border-1 border-round p-3">
            <h3 class="m-0 mb-2 text-base">{{ t('statistics.by_year') }}</h3>
            <Bar v-if="yearChartData" :data="yearChartData" :options="chartOptions" />
            <div v-else class="text-center text-color-secondary p-3">{{ t('statistics.no_data') }}</div>
          </div>
        </div>
        <div class="col-12 md:col-6">
          <div class="surface-card border-1 border-round p-3">
            <h3 class="m-0 mb-2 text-base">{{ t('statistics.top_rated') }}</h3>
            <div v-if="stats.topRated && stats.topRated.length">
              <div v-for="g in stats.topRated.slice(0, 5)" :key="g.id" class="flex align-items-center gap-2 py-1 border-bottom-1 surface-border cursor-pointer" @click="router.push(`/game/${g.id}`)">
                <span class="font-semibold flex-1 text-sm">{{ g.name }}</span>
                <span class="text-yellow-500 font-bold text-sm">{{ g.avgRating }}</span>
                <span class="text-xs text-color-secondary">({{ g.ratingCount }})</span>
              </div>
            </div>
            <div v-else class="text-center text-color-secondary p-3">{{ t('statistics.no_ratings') }}</div>
          </div>
        </div>
      </div>

      <div class="grid mb-3">
        <div class="col-12 md:col-6">
          <div class="surface-card border-1 border-round p-3">
            <h3 class="m-0 mb-2 text-base">{{ t('statistics.most_rated') }}</h3>
            <div v-if="stats.mostRated && stats.mostRated.length">
              <div v-for="g in stats.mostRated.slice(0, 5)" :key="g.id" class="flex align-items-center gap-2 py-1 border-bottom-1 surface-border cursor-pointer" @click="router.push(`/game/${g.id}`)">
                <span class="font-semibold flex-1 text-sm">{{ g.name }}</span>
                <span class="text-sm">{{ g.ratingCount }} {{ t('statistics.votes') }}</span>
                <span class="text-xs text-color-secondary">{{ g.avgRating }}</span>
              </div>
            </div>
            <div v-else class="text-center text-color-secondary p-3">{{ t('statistics.no_ratings') }}</div>
          </div>
        </div>
        <div class="col-12 md:col-6">
          <div class="surface-card border-1 border-round p-3">
            <h3 class="m-0 mb-2 text-base">{{ t('statistics.most_favorited') }}</h3>
            <div v-if="stats.mostFavorited && stats.mostFavorited.length">
              <div v-for="g in stats.mostFavorited.slice(0, 5)" :key="g.id" class="flex align-items-center gap-2 py-1 border-bottom-1 surface-border cursor-pointer" @click="router.push(`/game/${g.id}`)">
                <i class="pi pi-heart-fill text-pink-500" />
                <span class="font-semibold flex-1 text-sm">{{ g.name }}</span>
                <span class="text-sm text-color-secondary">{{ g.favoriteCount }}</span>
              </div>
            </div>
            <div v-else class="text-center text-color-secondary p-3">{{ t('statistics.no_favorites') }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import { statisticsApi } from '../api/statistics'
import Button from 'primevue/button'
import { Bar, Pie } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Title, Tooltip, Legend)

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const stats = ref(null)
const loading = ref(false)
const refreshing = ref(false)

async function handleRefreshSizes() {
  if (!window.confirm(t('statistics.refresh_sizes_confirm'))) return
  refreshing.value = true
  try {
    await statisticsApi.refreshSizes()
    stats.value = null
    loading.value = true
    const res = await statisticsApi.get()
    stats.value = res.data.data
  } finally {
    refreshing.value = false
    loading.value = false
  }
}

const chartOptions = {
  responsive: true,
  plugins: {
    legend: { display: false }
  },
  scales: {
    y: {
      beginAtZero: true,
      ticks: { stepSize: 1 }
    }
  }
}

const pieOptions = {
  responsive: true,
  plugins: {
    legend: { position: 'right', labels: { boxWidth: 12, padding: 8, font: { size: 11 } } }
  }
}

const platformChartData = computed(() => {
  if (!stats.value?.gamesByPlatform?.length) return null
  return {
    labels: stats.value.gamesByPlatform.map(i => i.label),
    datasets: [{
      label: t('statistics.games'),
      data: stats.value.gamesByPlatform.map(i => i.count),
      backgroundColor: '#3B82F6'
    }]
  }
})

const genreChartData = computed(() => {
  if (!stats.value?.gamesByGenre?.length) return null
  const top = stats.value.gamesByGenre.filter(i => i.count > 0).slice(0, 12)
  const colors = [
    '#3B82F6', '#EF4444', '#10B981', '#F59E0B', '#8B5CF6',
    '#EC4899', '#14B8A6', '#F97316', '#06B6D4', '#84CC16',
    '#A855F7', '#64748B'
  ]
  return {
    labels: top.map(i => i.name),
    datasets: [{
      data: top.map(i => i.count),
      backgroundColor: colors.slice(0, top.length)
    }]
  }
})

const yearChartData = computed(() => {
  if (!stats.value?.gamesByYear?.length) return null
  const sorted = [...stats.value.gamesByYear].sort((a, b) => a.label.localeCompare(b.label))
  return {
    labels: sorted.map(i => i.label),
    datasets: [{
      label: t('statistics.games'),
      data: sorted.map(i => i.count),
      backgroundColor: '#10B981'
    }]
  }
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await statisticsApi.get()
    stats.value = res.data.data
  } catch {
    // handled by empty state
  } finally {
    loading.value = false
  }
})

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(1)} ${units[i]}`
}
</script>

<style scoped>
.statistics-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}
.stat-card {
  height: 100%;
}
</style>
