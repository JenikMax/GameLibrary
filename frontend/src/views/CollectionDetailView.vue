<template>
  <div class="collection-detail" v-if="collection">
    <div class="flex align-items-center justify-content-between mb-3">
      <div class="flex align-items-center gap-2">
        <Button icon="pi pi-arrow-left" text rounded @click="router.push('/collections')" />
        <div>
          <div class="flex align-items-center gap-2">
            <h2 class="m-0">{{ collection.name }}</h2>
            <Tag v-if="collection.isSmart" :value="t('collections.smart')" severity="info" size="small" rounded />
          </div>
          <p v-if="collection.description" class="m-0 text-sm text-color-secondary">{{ collection.description }}</p>
          <div v-if="collection.isSmart && collection.smartRules" class="mt-1">
            <small class="text-color-secondary">{{ t('collections.smart_rules') }}: </small>
            <SmartRulesForm :modelValue="parsedRules" :options="libraryStore.filterOptions" readonly />
          </div>
        </div>
      </div>
      <div class="flex gap-2">
        <Button
          v-if="isOwner"
          :label="t('collections.edit')"
          icon="pi pi-pencil"
          severity="info"
          size="small"
          @click="showEditDialog = true"
        />
        <Button
          v-if="isOwner"
          :label="t('collections.delete')"
          icon="pi pi-trash"
          severity="danger"
          size="small"
          @click="handleDelete"
        />
      </div>
    </div>

    <Dialog v-model:visible="showEditDialog" :header="t('collections.edit')" :modal="true">
      <div class="field">
        <label for="edit-name">{{ t('collections.name') }}</label>
        <InputText id="edit-name" v-model="editName" class="w-full" />
      </div>
      <div class="field">
        <label for="edit-desc">{{ t('collections.description') }}</label>
        <Textarea id="edit-desc" v-model="editDescription" class="w-full" rows="3" />
      </div>
      <div class="field-checkbox">
        <Checkbox id="edit-smart" v-model="editIsSmart" :binary="true" />
        <label for="edit-smart">{{ t('collections.smart') }}</label>
      </div>
      <div v-if="editIsSmart" class="field">
        <label>{{ t('collections.smart_rules') }}</label>
        <SmartRulesForm v-model="editSmartRulesObj" :options="libraryStore.filterOptions" />
      </div>
      <div class="field-checkbox">
        <Checkbox id="edit-public" v-model="editIsPublic" :binary="true" />
        <label for="edit-public">{{ t('collections.public') }}</label>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showEditDialog = false" />
        <Button :label="t('common.save')" @click="handleUpdate" :loading="updating" />
      </template>
    </Dialog>

    <div v-if="loading" class="flex justify-content-center p-5">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem" />
    </div>

    <div v-else-if="games.length === 0" class="text-center text-color-secondary p-5">
      {{ t('collections.no_games') }}
    </div>

    <div v-else class="games-grid">
      <div
        v-for="(entry, i) in games"
        :key="entry.id || entry.gameId"
        class="game-card-wrapper"
      >
        <GameCard :game="entry.gameData" />
        <Button
          v-if="isOwner && !collection.isSmart"
          icon="pi pi-times"
          rounded
          text
          severity="danger"
          class="remove-btn"
          @click.stop="handleRemove(entry.gameId)"
        />
      </div>
    </div>
  </div>

  <div v-else-if="!loading" class="text-center p-5 text-color-secondary">
    {{ t('collections.not_found') }}
  </div>
</template>

<script setup>
import { ref, onActivated, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from '../composables/useI18n'
import { useAuthStore } from '../stores/auth'
import { useLibraryStore } from '../stores/library'
import { collectionsApi } from '../api/collections'
import { gamesApi } from '../api/games'
import { useToast } from 'primevue/usetoast'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import GameCard from '../components/GameCard.vue'
import SmartRulesForm from '../components/SmartRulesForm.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const libraryStore = useLibraryStore()
const toast = useToast()

const collection = ref(null)
const games = ref([])
const loading = ref(false)

const showEditDialog = ref(false)
const editName = ref('')
const editDescription = ref('')
const editIsPublic = ref(false)
const editIsSmart = ref(false)
const editSmartRulesObj = ref({})
const updating = ref(false)

const parsedRules = computed(() => {
  if (!collection.value?.smartRules) return {}
  try { return JSON.parse(collection.value.smartRules) }
  catch { return {} }
})

const isOwner = computed(() =>
  collection.value && authStore.userId && collection.value.userId === authStore.userId
)

async function load() {
  loading.value = true
  try {
    if (!libraryStore.filterOptions.genres?.length) {
      await libraryStore.fetchFilterOptions()
    }
    const [colRes, gamesRes] = await Promise.all([
      collectionsApi.get(route.params.id),
      collectionsApi.getGames(route.params.id)
    ])
    collection.value = colRes.data.data

    editName.value = collection.value.name || ''
    editDescription.value = collection.value.description || ''
    editIsPublic.value = collection.value.isPublic || false
    editIsSmart.value = collection.value.isSmart || false
    try {
      editSmartRulesObj.value = collection.value.smartRules ? JSON.parse(collection.value.smartRules) : {}
    } catch {
      editSmartRulesObj.value = {}
    }

    const entries = gamesRes.data.data || []

    if (collection.value.isSmart) {
      games.value = entries.map(e => ({
        gameId: e.gameId,
        gameData: {
          id: e.gameId,
          name: e.name || `#${e.gameId}`,
          platform: e.platform || '',
          releaseDate: e.releaseDate || '',
          genres: e.genres || [],
          avgRating: e.avgRating || null,
          logoUrl: `/game-library/api/images/games/${e.gameId}/logo`
        }
      }))
    } else {
      const ids = entries.map(e => e.gameId)
      const shortGames = await Promise.all(
        ids.map(id =>
          gamesApi.getGame(id).then(r => r.data.data).catch(() => null)
        )
      )
      games.value = entries.map((e, i) => {
        const info = shortGames[i] || {}
        return {
          ...e,
          gameData: {
            id: e.gameId,
            name: info.name || `#${e.gameId}`,
            platform: info.platform || '',
            releaseDate: info.releaseDate || '',
            genres: info.genres || [],
            avgRating: info.avgRating || null,
            logoUrl: `/game-library/api/images/games/${e.gameId}/logo`
          }
        }
      })
    }
    games.value.sort((a, b) => {
      const ra = a.gameData.avgRating ?? 0
      const rb = b.gameData.avgRating ?? 0
      if (rb !== ra) return rb - ra
      return (a.gameData.name || '').localeCompare(b.gameData.name || '')
    })
    } catch {
      toast.add({ severity: 'error', summary: t('collections.load_failed'), life: 3000 })
    } finally {
    loading.value = false
  }
}

onActivated(load)
watch(() => route.params.id, load, { immediate: true })

async function handleUpdate() {
  if (!editName.value.trim()) return
  updating.value = true
  try {
    const payload = {
      name: editName.value.trim(),
      description: editDescription.value.trim(),
      isPublic: editIsPublic.value,
      isSmart: editIsSmart.value
    }
    if (editIsSmart.value) {
      const rulesStr = Object.keys(editSmartRulesObj.value).length ? JSON.stringify(editSmartRulesObj.value) : null
      payload.smartRules = rulesStr
    }
    await collectionsApi.update(route.params.id, payload)
    showEditDialog.value = false
    await load()
    toast.add({ severity: 'success', summary: t('collections.updated'), life: 2000 })
  } catch {
    toast.add({ severity: 'error', summary: t('collections.update_failed'), life: 3000 })
  } finally {
    updating.value = false
  }
}

async function handleDelete() {
  if (!confirm(t('collections.delete_confirm'))) return
  try {
    await collectionsApi.delete(route.params.id)
    toast.add({ severity: 'success', summary: t('collections.deleted'), life: 2000 })
    router.push('/collections')
  } catch {
    toast.add({ severity: 'error', summary: t('collections.delete_failed'), life: 3000 })
  }
}

async function handleRemove(gameId) {
  try {
    await collectionsApi.removeGame(route.params.id, gameId)
    await load()
    toast.add({ severity: 'info', summary: t('collections.game_removed'), life: 2000 })
  } catch {
    toast.add({ severity: 'error', summary: t('collections.remove_failed'), life: 3000 })
  }
}
</script>

<style scoped>
.collection-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}
.games-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
}
.game-card-wrapper {
  position: relative;
}
.remove-btn {
  position: absolute;
  top: 0.25rem;
  right: 0.25rem;
  z-index: 1;
}
</style>
