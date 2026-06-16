<template>
  <div v-if="loading" class="flex justify-content-center p-5">
    <ProgressSpinner />
  </div>
  <div v-else-if="game" class="edit-container">
    <div class="flex align-items-center gap-3 mb-3">
      <Button icon="pi pi-arrow-left" text @click="$router.push(`/game/${game.id}`)" />
      <h2 class="m-0">Edit: {{ game.name }}</h2>
    </div>

    <Message v-if="error" severity="error" :closable="false" class="mb-3">{{ error }}</Message>
    <Message v-if="success" severity="success" :closable="false" class="mb-3">{{ success }}</Message>

    <div class="grid">
      <div class="col-12 md:col-8">
        <Card>
          <template #content>
            <div class="formgrid grid">
              <div class="field col-12 md:col-6">
                <label for="name">Name</label>
                <InputText id="name" v-model="form.name" class="w-full" />
              </div>
              <div class="field col-12 md:col-6">
                <label for="platform">Platform</label>
                <InputText id="platform" v-model="form.platform" class="w-full" />
              </div>
              <div class="field col-12 md:col-4">
                <label for="releaseDate">Release Date</label>
                <InputText id="releaseDate" v-model="form.releaseDate" class="w-full" />
              </div>
              <div class="field col-12 md:col-4">
                <label for="directoryPath">Directory Path</label>
                <InputText id="directoryPath" v-model="form.directoryPath" class="w-full" />
              </div>
              <div class="field col-12 md:col-4">
                <label for="trailerUrl">Trailer URL</label>
                <InputText id="trailerUrl" v-model="form.trailerUrl" class="w-full" />
              </div>
              <div class="field col-12">
                <label for="genres">Genres</label>
                <MultiSelect
                  id="genres"
                  v-model="form.genres"
                  :options="allGenres"
                  optionLabel="name"
                  optionValue="code"
                  placeholder="Select genres"
                  class="w-full"
                />
              </div>
              <div class="field col-12">
                <label for="description">Description</label>
                <Textarea id="description" v-model="form.description" rows="6" class="w-full" />
              </div>
              <div class="field col-12">
                <label for="instruction">Instruction</label>
                <Textarea id="instruction" v-model="form.instruction" rows="4" class="w-full" />
              </div>
            </div>
          </template>
        </Card>
      </div>

      <div class="col-12 md:col-4">
        <Card>
          <template #title>Scraper</template>
          <template #content>
            <div class="field">
              <label for="scrapeSource">Source</label>
              <Select
                id="scrapeSource"
                v-model="scrape.source"
                :options="scrapeSources"
                class="w-full"
              />
            </div>
            <div class="field">
              <label for="scrapeUrl">URL</label>
              <InputText id="scrapeUrl" v-model="scrape.url" class="w-full" />
            </div>
            <div class="field flex flex-column gap-2">
              <label>Fields to scrape</label>
              <div v-for="opt in scrapeFields" :key="opt.key" class="flex align-items-center gap-2">
                <Checkbox v-model="scrape[opt.key]" :binary="true" :inputId="opt.key" />
                <label :for="opt.key">{{ opt.label }}</label>
              </div>
            </div>
            <Button label="Scrape" icon="pi pi-cloud-download" @click="handleScrape" :loading="scraping" class="w-full" />
          </template>
        </Card>
      </div>
    </div>

    <div class="flex justify-content-end mt-3 gap-2">
      <Button label="Cancel" severity="secondary" @click="$router.push(`/game/${game.id}`)" />
      <Button label="Save" icon="pi pi-check" @click="handleSave" :loading="saving" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gamesApi } from '../api/games'
import ProgressSpinner from 'primevue/progressspinner'
import Button from 'primevue/button'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import MultiSelect from 'primevue/multiselect'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'
import Message from 'primevue/message'

const route = useRoute()
const router = useRouter()

const game = ref(null)
const loading = ref(true)
const saving = ref(false)
const scraping = ref(false)
const error = ref('')
const success = ref('')

const form = ref({
  name: '',
  platform: '',
  releaseDate: '',
  directoryPath: '',
  trailerUrl: '',
  genres: [],
  description: '',
  instruction: ''
})

const allGenres = ref([])
const scrapeSources = ['Playground', 'Igromania']
const scrapeFields = [
  { key: 'title', label: 'Title' },
  { key: 'poster', label: 'Poster' },
  { key: 'description', label: 'Description' },
  { key: 'year', label: 'Year' },
  { key: 'genres', label: 'Genres' },
  { key: 'screens', label: 'Screenshots' }
]
const scrape = ref({
  source: 'Playground',
  url: '',
  title: false,
  poster: false,
  description: false,
  year: false,
  genres: false,
  screens: false
})

onMounted(async () => {
  try {
    const [gameRes, filterRes] = await Promise.all([
      gamesApi.getGame(route.params.id),
      gamesApi.getFilterOptions()
    ])
    const g = gameRes.data.data
    game.value = g
    form.value = {
      name: g.name || '',
      platform: g.platform || '',
      releaseDate: g.releaseDate || '',
      directoryPath: g.directoryPath || '',
      trailerUrl: g.trailerUrl || '',
      genres: g.genres || [],
      description: g.description || '',
      instruction: g.instruction || ''
    }
    allGenres.value = filterRes.data.data.genres || []
  } finally {
    loading.value = false
  }
})

async function handleSave() {
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const response = await gamesApi.editGame(route.params.id, form.value)
    game.value = response.data.data
    success.value = 'Game saved successfully'
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to save game'
  } finally {
    saving.value = false
  }
}

async function handleScrape() {
  scraping.value = true
  error.value = ''
  try {
    const response = await gamesApi.grabGame(route.params.id, scrape.value)
    const g = response.data.data
    form.value = {
      name: g.name || form.value.name,
      platform: g.platform || form.value.platform,
      releaseDate: g.releaseDate || form.value.releaseDate,
      directoryPath: g.directoryPath || form.value.directoryPath,
      trailerUrl: g.trailerUrl || form.value.trailerUrl,
      genres: g.genres || form.value.genres,
      description: g.description || form.value.description,
      instruction: g.instruction || form.value.instruction
    }
    success.value = 'Data scraped successfully'
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to scrape data'
  } finally {
    scraping.value = false
  }
}
</script>

<style scoped>
.edit-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}
.field {
  margin-bottom: 1rem;
}
.field label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.25rem;
}
</style>
