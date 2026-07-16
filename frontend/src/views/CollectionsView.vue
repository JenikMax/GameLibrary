<template>
  <div class="collections-page">
    <div class="flex align-items-center justify-content-between mb-3">
      <h2 class="m-0">{{ t('collections.title') }}</h2>
      <Button :label="t('collections.create')" icon="pi pi-plus" severity="success" @click="showCreateDialog = true" />
    </div>

    <Dialog v-model:visible="showCreateDialog" :header="t('collections.create')" :modal="true" :closable="true">
      <div class="field">
        <label for="col-name">{{ t('collections.name') }}</label>
        <InputText id="col-name" v-model="newName" class="w-full" :placeholder="t('collections.name_placeholder')" />
      </div>
      <div class="field">
        <label for="col-desc">{{ t('collections.description') }}</label>
        <Textarea id="col-desc" v-model="newDescription" class="w-full" rows="3" />
      </div>
      <div class="field-checkbox">
        <Checkbox id="col-public" v-model="newIsPublic" :binary="true" />
        <label for="col-public">{{ t('collections.public') }}</label>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showCreateDialog = false" />
        <Button :label="t('common.save')" @click="handleCreate" :loading="creating" />
      </template>
    </Dialog>

    <div v-if="loading" class="flex justify-content-center p-5">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem" />
    </div>

    <div v-else-if="collections.length === 0" class="text-center text-color-secondary p-5">
      {{ t('collections.empty') }}
    </div>

    <div v-else class="collections-grid">
      <div
        v-for="c in collections"
        :key="c.id"
        class="collection-card p-3 border-1 border-round surface-card cursor-pointer hover:shadow-2 transition-shadow"
        @click="router.push(`/collections/${c.id}`)"
      >
        <div class="flex align-items-center justify-content-between mb-2">
          <i class="pi pi-folder" style="font-size: 1.5rem; color: var(--p-primary-color)" />
          <Tag v-if="c.isPublic" :value="t('collections.public')" severity="info" size="small" />
        </div>
        <h3 class="m-0 mb-1 text-base">{{ c.name }}</h3>
        <p v-if="c.description" class="m-0 text-sm text-color-secondary line-clamp-2">{{ c.description }}</p>
        <div class="flex align-items-center gap-2 mt-2 text-xs text-color-secondary">
          <span>{{ c.gameCount }} {{ t('collections.games') }}</span>
          <span>{{ c.username }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRouter, onBeforeRouteUpdate } from 'vue-router'
import { useI18n } from '../composables/useI18n'
import { collectionsApi } from '../api/collections'
import { useToast } from 'primevue/usetoast'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import Tag from 'primevue/tag'
import Button from 'primevue/button'

const { t } = useI18n()
const router = useRouter()
const toast = useToast()

const collections = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const creating = ref(false)
const newName = ref('')
const newDescription = ref('')
const newIsPublic = ref(false)

onMounted(load)
onActivated(load)

async function load() {
  loading.value = true
  try {
    const res = await collectionsApi.list()
    collections.value = res.data.data || []
  } catch {
    toast.add({ severity: 'error', summary: t('collections.load_failed'), life: 3000 })
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!newName.value.trim()) return
  creating.value = true
  try {
    await collectionsApi.create({
      name: newName.value.trim(),
      description: newDescription.value.trim(),
      isPublic: newIsPublic.value
    })
    showCreateDialog.value = false
    newName.value = ''
    newDescription.value = ''
    newIsPublic.value = false
    await load()
    toast.add({ severity: 'success', summary: t('collections.created'), life: 2000 })
  } catch {
    toast.add({ severity: 'error', summary: t('collections.create_failed'), life: 3000 })
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.collections-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 1rem;
}
.collections-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}
.collection-card {
  transition: box-shadow 0.2s;
}
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
