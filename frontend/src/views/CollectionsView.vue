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
        <Checkbox id="col-smart" v-model="newIsSmart" :binary="true" />
        <label for="col-smart">{{ t('collections.smart') }}</label>
      </div>
      <div v-if="newIsSmart" class="field">
        <label>{{ t('collections.smart_rules') }}</label>
        <SmartRulesForm v-model="newSmartRulesObj" :options="store.filterOptions" />
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
      <CollectionCard
        v-for="c in paginatedCollections"
        :key="c.id"
        :collection="c"
      />
    </div>

    <Paginator
      v-if="totalPages > 1"
      :rows="pageSize"
      :totalRecords="collections.length"
      :first="(page - 1) * pageSize"
      @page="page = $event.page + 1"
      class="mt-3"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import { useI18n } from '../composables/useI18n'
import { collectionsApi } from '../api/collections'
import { useLibraryStore } from '../stores/library'
import { useToast } from 'primevue/usetoast'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import Button from 'primevue/button'
import Paginator from 'primevue/paginator'
import CollectionCard from '../components/CollectionCard.vue'
import SmartRulesForm from '../components/SmartRulesForm.vue'

const { t } = useI18n()
const toast = useToast()
const store = useLibraryStore()

const collections = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const creating = ref(false)
const newName = ref('')
const newDescription = ref('')
const newIsPublic = ref(false)
const newIsSmart = ref(false)
const newSmartRulesObj = ref({})

const page = ref(1)
const pageSize = 12
const paginatedCollections = computed(() => {
  const start = (page.value - 1) * pageSize
  return collections.value.slice(start, start + pageSize)
})
const totalPages = computed(() => Math.ceil(collections.value.length / pageSize))

onMounted(async () => {
  if (!store.filterOptions.genres?.length) {
    await store.fetchFilterOptions()
  }
  load()
})
onActivated(load)

async function load() {
  loading.value = true
  try {
    const res = await collectionsApi.listWithHero()
    collections.value = res.data.data || []
    page.value = 1
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
    const payload = {
      name: newName.value.trim(),
      description: newDescription.value.trim(),
      isPublic: newIsPublic.value,
      isSmart: newIsSmart.value
    }
    if (newIsSmart.value && Object.keys(newSmartRulesObj.value).length) {
      payload.smartRules = JSON.stringify(newSmartRulesObj.value)
    }
    await collectionsApi.create(payload)
    showCreateDialog.value = false
    newName.value = ''
    newDescription.value = ''
    newIsPublic.value = false
    newIsSmart.value = false
    newSmartRulesObj.value = {}
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
  max-width: 1400px;
  margin: 0 auto;
  padding: 1rem;
}
.collections-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.25rem;
}
</style>
