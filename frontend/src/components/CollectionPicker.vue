<template>
  <Dialog :visible="visible" @update:visible="$emit('close')" :header="t('collections.add_to')" :modal="true" :closable="true" class="w-[700px]">
    <div v-if="loading" class="flex justify-content-center p-3">
      <ProgressSpinner style="width: 50px; height: 50px" />
    </div>

    <div v-else-if="collections.length === 0" class="text-center p-3 text-color-secondary">
      {{ t('collections.no_collections') }}
    </div>

    <div v-else class="flex flex-column" style="max-height: 55vh; overflow: hidden">
      <InputText
        v-model="search"
        :placeholder="t('filter.search_placeholder')"
        class="w-full mb-2"
        size="small"
      />
      <div class="picker-list flex-1 overflow-y-auto" style="min-height: 0">
        <div
          v-for="collection in filteredCollections"
          :key="collection.id"
          class="collection-item flex align-items-center justify-content-between p-2 border-round cursor-pointer"
          @click="toggleGame(collection)"
        >
          <div class="flex align-items-center gap-2">
            <i class="pi pi-folder" />
            <span>{{ collection.name }}</span>
            <Tag :value="String(collection.gameCount)" severity="info" size="small" />
          </div>
          <i v-if="isInCollection(collection)" class="pi pi-check-circle text-green-500" />
          <i v-else class="pi pi-plus-circle text-color-secondary" />
        </div>
        <div v-if="filteredCollections.length === 0 && search" class="text-center text-color-secondary p-2 text-sm">
          {{ t('library.no_games') }}
        </div>
      </div>
    </div>

    <template #footer>
      <Button :label="t('common.close')" @click="$emit('close')" />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from '../composables/useI18n'
import { collectionsApi } from '../api/collections'
import { useToast } from 'primevue/usetoast'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import ProgressSpinner from 'primevue/progressspinner'
import Tag from 'primevue/tag'
import Button from 'primevue/button'

const props = defineProps({
  visible: Boolean,
  gameId: [Number, String]
})
const emit = defineEmits(['close'])

const { t } = useI18n()
const toast = useToast()

const collections = ref([])
const loading = ref(false)
const memberMap = ref({})
const search = ref('')

const filteredCollections = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return collections.value
  return collections.value.filter(c => c.name.toLowerCase().includes(q))
})

watch(() => props.visible, async (val) => {
  if (val) {
    search.value = ''
    await load()
  }
})

onMounted(async () => {
  if (props.visible) await load()
})

async function load() {
  loading.value = true
  try {
    const res = await collectionsApi.list()
    collections.value = res.data.data || []

    memberMap.value = {}
    for (const c of collections.value) {
      try {
        const gRes = await collectionsApi.getGames(c.id)
        const gameIds = (gRes.data.data || []).map(e => e.gameId)
        memberMap.value[c.id] = gameIds
      } catch {
        memberMap.value[c.id] = []
      }
    }
  } catch {
    toast.add({ severity: 'error', summary: t('collections.load_failed'), life: 2000 })
  } finally {
    loading.value = false
  }
}

function isInCollection(c) {
  return (memberMap.value[c.id] || []).includes(Number(props.gameId))
}

async function toggleGame(c) {
  const gid = Number(props.gameId)
  try {
    if (isInCollection(c)) {
      await collectionsApi.removeGame(c.id, gid)
      memberMap.value[c.id] = (memberMap.value[c.id] || []).filter(id => id !== gid)
    } else {
      await collectionsApi.addGame(c.id, gid)
      memberMap.value[c.id] = [...(memberMap.value[c.id] || []), gid]
    }
  } catch {
    toast.add({ severity: 'error', summary: t('collections.operation_failed'), life: 2000 })
  }
}
</script>

<style scoped>
.picker-list {
  scrollbar-width: thin;
  scrollbar-gutter: stable;
  margin-bottom: 12px;
  padding-right: 16px;
}
.collection-item {
  transition: background-color 0.15s ease;
}
.collection-item:hover {
  background-color: var(--p-surface-100);
}
.app-dark .collection-item:hover {
  background-color: var(--p-surface-700);
}
</style>
