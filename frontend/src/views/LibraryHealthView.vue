<template>
  <div class="health-dashboard">
    <h2 class="mb-3">
      <span v-if="isTerminalTheme">&gt; {{ t('health.title').replace(/ /g, '_') }}<span class="t-cursor" /></span>
      <span v-else>{{ t('health.title') }}</span>
    </h2>

    <div v-if="loading" class="flex justify-content-center p-5">
      <ProgressBar mode="indeterminate" class="w-full" />
    </div>

    <template v-else-if="report">
      <div class="health-summary mb-3">
        <div class="flex align-items-center gap-3 mb-2">
          <span class="text-lg font-bold">{{ t('health.score') }}:</span>
          <span class="text-2xl font-bold" :class="scoreColorClass">{{ report.healthScore }}%</span>
          <span class="text-sm text-muted">{{ t('health.total_games') }}: {{ report.totalGames }}</span>
        </div>
        <ProgressBar :value="report.healthScore" :showValue="false" class="w-full" />
      </div>

      <div class="health-grid">
        <Card v-for="(info, code) in report.issueCounts" :key="code"
          :class="{ 'health-card-ok': info.count === 0, 'health-card-warn': info.count > 0 }">
          <template #content>
            <div class="flex flex-column align-items-center text-center gap-2">
              <i :class="issueIcon(code)" class="text-4xl" :style="{ color: info.count > 0 ? 'var(--p-orange-500)' : 'var(--p-green-500)' }" />
              <span class="font-bold">{{ issueLabel(code) }}</span>
              <span class="text-2xl font-bold" :style="{ color: info.count > 0 ? 'var(--p-orange-500)' : 'var(--p-green-500)' }">
                {{ info.count }}
              </span>
              <div class="flex flex-column gap-1">
                <Button v-if="info.count > 0"
                  :label="t('health.view_list')"
                  icon="pi pi-list"
                  severity="secondary"
                  size="small"
                  variant="outlined"
                  @click="showIssueList(code)"
                />
                <span v-if="info.count > 0 && !info.fixable" class="text-xs text-muted">
                  {{ t('health.needs_scraper') }}
                </span>
              </div>
            </div>
          </template>
        </Card>
      </div>

      <Dialog v-model:visible="issueDialogVisible" :header="issueDialogTitle" :modal="true" :style="{ width: '700px' }" class="issue-dialog">
        <div v-if="issueGames.length > 0">
          <DataTable :value="issueGames" paginator :rows="10" :rowsPerPageOptions="[5, 10, 20]" class="w-full">
            <Column field="gameId" :header="'ID'" style="width: 60px" />
            <Column field="gameName" :header="navItems.name || 'Name'">
              <template #body="{ data }">
                <router-link :to="`/game/${data.gameId}`" class="game-link">{{ data.gameName }}</router-link>
              </template>
            </Column>
            <Column field="platform" :header="navItems.platform || 'Platform'" style="width: 120px" />
          </DataTable>
        </div>
        <div v-else-if="dialogLoading" class="flex justify-content-center p-5">
          <ProgressBar mode="indeterminate" class="w-full" />
        </div>
        <div v-else class="text-center p-5 text-muted">
          {{ t('health.no_issues') }}
        </div>
      </Dialog>
    </template>

    <div v-else class="text-center p-5 text-muted">
      {{ t('health.no_issues') }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from '../composables/useI18n'
import { healthApi } from '../api/health'
import { useTheme } from '../composables/useTheme'
import Card from 'primevue/card'
import Button from 'primevue/button'
import ProgressBar from 'primevue/progressbar'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const { isTerminalTheme } = useTheme()
const toast = useToast()

const report = ref(null)
const loading = ref(true)
const issueDialogVisible = ref(false)
const issueDialogTitle = ref('')
const issueGames = ref([])
const currentIssueType = ref(null)
const dialogLoading = ref(false)

const navItems = computed(() => ({
  name: t('game.name') || 'Name',
  platform: t('game.platform') || 'Platform'
}))

const scoreColorClass = computed(() => {
  if (!report.value) return ''
  if (report.value.healthScore >= 90) return 'text-green-500'
  if (report.value.healthScore >= 70) return 'text-orange-500'
  return 'text-red-500'
})

const issueIcon = (code) => {
  const iconMap = {
    NO_GENRES: 'pi pi-tags',
    NO_DESCRIPTION: 'pi pi-file-edit',
    NO_TAGS: 'pi pi-tag',
    NO_SCREENSHOTS: 'pi pi-images',
    NO_YEAR: 'pi pi-calendar',
    NO_EMBEDDING: 'pi pi-bolt',
    NO_TRANSLATION: 'pi pi-language',
    PLACEHOLDER_DESC: 'pi pi-exclamation-triangle'
  }
  return iconMap[code] || 'pi pi-exclamation-circle'
}

const issueLabel = (code) => {
  const labelMap = {
    NO_GENRES: 'health.no_genres',
    NO_DESCRIPTION: 'health.no_description',
    NO_TAGS: 'health.no_tags',
    NO_SCREENSHOTS: 'health.no_screenshots',
    NO_YEAR: 'health.no_year',
    NO_EMBEDDING: 'health.no_embedding',
    NO_TRANSLATION: 'health.no_translation',
    PLACEHOLDER_DESC: 'health.placeholder_desc'
  }
  return t(labelMap[code] || code)
}

onMounted(async () => {
  await loadReport()
})

async function loadReport() {
  loading.value = true
  try {
    const res = await healthApi.getReport()
    report.value = res.data.data
  } catch {
    toast.add({ severity: 'error', summary: 'Failed to load health report', life: 3000 })
  } finally {
    loading.value = false
  }
}

async function showIssueList(code) {
  currentIssueType.value = code
  issueDialogTitle.value = issueLabel(code)
  issueDialogVisible.value = true
  dialogLoading.value = true
  try {
    const res = await healthApi.getIssues(code)
    issueGames.value = res.data.data || []
  } catch {
    issueGames.value = []
  } finally {
    dialogLoading.value = false
  }
}
</script>

<style scoped>
.health-dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}

.health-summary {
  padding: 1rem;
  border-radius: 0.5rem;
  background: var(--p-surface-100);
}

[data-color-scheme="dark"] .health-summary {
  background: var(--p-surface-800);
}

.health-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.health-card-ok {
  border-top: 3px solid var(--p-green-500);
}

.health-card-warn {
  border-top: 3px solid var(--p-orange-500);
}

.health-grid .p-card {
  transition: transform 0.2s;
}

.health-grid .p-card:hover {
  transform: translateY(-2px);
}

.game-link {
  color: var(--p-primary-color);
  text-decoration: none;
}

.game-link:hover {
  text-decoration: underline;
}

.text-green-500 { color: var(--p-green-500); }
.text-orange-500 { color: var(--p-orange-500); }
.text-red-500 { color: var(--p-red-500); }
.text-muted { color: var(--p-text-muted-color); }
</style>
