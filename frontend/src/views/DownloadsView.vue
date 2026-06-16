<template>
  <div class="downloads-container">
    <div class="flex align-items-center justify-content-between mb-3">
      <div class="flex align-items-center gap-2">
        <i class="pi pi-download text-2xl"></i>
        <h2 class="m-0">Downloads</h2>
      </div>
      <div class="flex gap-2 align-items-center">
        <Tag v-if="globalStat" :value="'⬆ ' + formatSpeed(globalStat.uploadSpeed)" severity="info" />
        <Tag v-if="globalStat" :value="'⬇ ' + formatSpeed(globalStat.downloadSpeed)" severity="warn" />
        <Button icon="pi pi-refresh" text rounded @click="fetchAll" />
      </div>
    </div>

    <Message v-if="aria2Connected === false" severity="warn" :closable="false" class="mb-3">
      aria2 is not connected. Large game seeding will not work.
      <a :href="ariaNgUrl" target="_blank" class="ml-2">Open AriaNg</a>
    </Message>
    <Message v-else severity="info" :closable="false" class="mb-3">
      <span class="flex align-items-center gap-2">
        <i class="pi pi-check-circle text-green-500"></i> aria2 connected
        <a :href="ariaNgUrl" target="_blank" class="ml-2">
          <i class="pi pi-external-link"></i> AriaNg Web UI
        </a>
      </span>
    </Message>

    <TabView>
      <TabPanel header="Active">
        <DataTable :value="activeDownloads" stripedRows :loading="loading">
          <Column field="name" header="Name">
            <template #body="slotProps">
              <div class="flex align-items-center gap-2">
                <i class="pi pi-file" />
                {{ slotProps.data.name || 'Unknown' }}
              </div>
            </template>
          </Column>
          <Column field="status" header="Status" style="width:100px">
            <template #body="slotProps">
              <Tag :value="slotProps.data.status" :severity="statusSeverity(slotProps.data.status)" />
            </template>
          </Column>
          <Column field="progress" header="Progress" style="width:200px">
            <template #body="slotProps">
              <div class="flex align-items-center gap-2">
                <ProgressBar :value="slotProps.data.progress" style="height:12px;width:120px" />
                <small>{{ slotProps.data.progress }}%</small>
              </div>
            </template>
          </Column>
          <Column field="downloadSpeed" header="DL Speed" style="width:100px">
            <template #body="slotProps">{{ formatSpeed(slotProps.data.downloadSpeed) }}</template>
          </Column>
          <Column field="uploadSpeed" header="UL Speed" style="width:100px">
            <template #body="slotProps">{{ formatSpeed(slotProps.data.uploadSpeed) }}</template>
          </Column>
          <Column header="Actions" style="width:100px">
            <template #body="slotProps">
              <div class="flex gap-1">
                <Button
                  v-if="slotProps.data.status === 'active'"
                  icon="pi pi-pause"
                  severity="warn"
                  text
                  @click="pauseDownload(slotProps.data.gid)"
                  v-tooltip.left="'Pause'"
                />
                <Button
                  v-if="slotProps.data.status === 'paused'"
                  icon="pi pi-play"
                  severity="success"
                  text
                  @click="unpauseDownload(slotProps.data.gid)"
                  v-tooltip.left="'Resume'"
                />
                <Button
                  icon="pi pi-times"
                  severity="danger"
                  text
                  @click="removeDownload(slotProps.data.gid)"
                  v-tooltip.left="'Remove'"
                />
              </div>
            </template>
          </Column>
          <template #empty>
            <div class="text-center p-4 text-gray-500">No active downloads</div>
          </template>
        </DataTable>
      </TabPanel>
      <TabPanel header="Waiting">
        <DataTable :value="waitingDownloads" stripedRows>
          <Column field="gid" header="GID"></Column>
          <Column field="status" header="Status">
            <template #body="slotProps">
              <Tag :value="slotProps.data.status" severity="info" />
            </template>
          </Column>
          <template #empty>
            <div class="text-center p-4 text-gray-500">No waiting downloads</div>
          </template>
        </DataTable>
      </TabPanel>
      <TabPanel header="Completed / Stopped">
        <DataTable :value="stoppedDownloads" stripedRows>
          <Column field="gid" header="GID"></Column>
          <Column field="status" header="Status">
            <template #body="slotProps">
              <Tag :value="slotProps.data.status" :severity="slotProps.data.status === 'complete' ? 'success' : 'secondary'" />
            </template>
          </Column>
          <template #empty>
            <div class="text-center p-4 text-gray-500">No completed downloads</div>
          </template>
        </DataTable>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { downloadsApi } from '../api/downloads'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import ProgressBar from 'primevue/progressbar'
import Message from 'primevue/message'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import { useToast } from 'primevue/usetoast'

const toast = useToast()
const activeDownloads = ref([])
const waitingDownloads = ref([])
const stoppedDownloads = ref([])
const globalStat = ref(null)
const aria2Connected = ref(null)
const loading = ref(false)
const ariaNgUrl = window.location.origin.replace(/:\d+$/, '') + ':6880'

let pollInterval = null

onMounted(async () => {
  await checkAria2()
  if (aria2Connected.value) {
    await fetchAll()
    pollInterval = setInterval(fetchAll, 5000)
  }
})

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
})

async function checkAria2() {
  try {
    await downloadsApi.getAria2Version()
    aria2Connected.value = true
  } catch {
    aria2Connected.value = false
  }
}

async function fetchAll() {
  loading.value = true
  try {
    const [activeRes, waitingRes, stoppedRes, statRes] = await Promise.all([
      downloadsApi.getActive(),
      downloadsApi.getWaiting(0, 50),
      downloadsApi.getStopped(0, 50),
      downloadsApi.getGlobalStat()
    ])
    activeDownloads.value = activeRes.data.data || []
    waitingDownloads.value = waitingRes.data.data || []
    stoppedDownloads.value = stoppedRes.data.data || []
    globalStat.value = statRes.data.data || null
  } catch {
    // aria2 might be down
  } finally {
    loading.value = false
  }
}

async function pauseDownload(gid) {
  try {
    await downloadsApi.pause(gid)
    toast.add({ severity: 'success', summary: 'Download paused', life: 2000 })
    await fetchAll()
  } catch {
    toast.add({ severity: 'error', summary: 'Failed to pause', life: 3000 })
  }
}

async function unpauseDownload(gid) {
  try {
    await downloadsApi.unpause(gid)
    toast.add({ severity: 'success', summary: 'Download resumed', life: 2000 })
    await fetchAll()
  } catch {
    toast.add({ severity: 'error', summary: 'Failed to resume', life: 3000 })
  }
}

async function removeDownload(gid) {
  try {
    await downloadsApi.remove(gid)
    toast.add({ severity: 'info', summary: 'Download removed', life: 2000 })
    await fetchAll()
  } catch {
    toast.add({ severity: 'error', summary: 'Failed to remove', life: 3000 })
  }
}

function formatSpeed(speed) {
  if (!speed) return '0 B/s'
  const s = parseInt(speed)
  if (s === 0) return '0 B/s'
  const units = ['B/s', 'KB/s', 'MB/s', 'GB/s']
  let i = 0
  let v = s
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return v.toFixed(v >= 10 ? 0 : 1) + ' ' + units[i]
}

function statusSeverity(status) {
  switch (status) {
    case 'active': return 'success'
    case 'waiting': return 'info'
    case 'paused': return 'warn'
    case 'error': return 'danger'
    case 'complete': return 'success'
    default: return 'secondary'
  }
}
</script>

<style scoped>
.downloads-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 1rem;
}
</style>
