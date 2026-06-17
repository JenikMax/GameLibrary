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
                  display="chip"
                  filter
                  filterPlaceholder="Search genres..."
                  class="w-full"
                />
              </div>
              <div class="field col-12">
                <label for="description">Description</label>
                <QuillEditor v-model:content="form.description" content-type="html"
                  :options="editorOptions" class="quill-editor"
                  style="height:250px;display:flex;flex-direction:column" />
              </div>
              <div class="field col-12">
                <label for="instruction">Instruction</label>
                <QuillEditor v-model:content="form.instruction" content-type="html"
                  :options="editorOptions" class="quill-editor"
                  style="height:250px;display:flex;flex-direction:column" />
              </div>
              <div class="field col-12">
                <label>Screenshots</label>
                <div class="screenshots-grid">
                  <div v-for="ss in existingScreenshots" :key="ss.id" class="screenshot-item">
                    <img :src="ss.url" alt="screenshot" class="screenshot-thumb" />
                    <Button icon="pi pi-trash" severity="danger" text rounded size="small"
                      class="screenshot-delete-btn" @click="removeExistingScreenshot(ss.id)" />
                  </div>
                  <div v-for="(preview, i) in newScreenshotPreviews" :key="'new-'+i" class="screenshot-item">
                    <img :src="preview" alt="new screenshot" class="screenshot-thumb" />
                    <Button icon="pi pi-trash" severity="danger" text rounded size="small"
                      class="screenshot-delete-btn" @click="removeNewScreenshot(i)" />
                  </div>
                  <div class="screenshot-add-box" @click="$refs.screenshotsInput.click()">
                    <i class="pi pi-plus text-2xl"></i>
                    <span class="text-sm">Add</span>
                  </div>
                </div>
                <input ref="screenshotsInput" type="file" accept="image/*" multiple
                  class="hidden-input" @change="handleScreenshotsUpload" />
              </div>
            </div>
          </template>
        </Card>
      </div>

      <div class="col-12 md:col-4">
        <Card class="mb-3">
          <template #title>Logo</template>
          <template #content>
            <div class="flex flex-column align-items-center gap-2">
              <img v-if="logoPreview" :src="logoPreview" class="logo-preview" alt="logo preview" />
              <img v-else :src="game.logoUrl || '/game-library/img/default.jpg'" class="logo-preview" alt="current logo" />
              <Button label="Change Logo" icon="pi pi-image" severity="secondary" size="small"
                @click="$refs.logoInput.click()" />
              <input ref="logoInput" type="file" accept="image/*" class="hidden-input" @change="handleLogoUpload" />
            </div>
          </template>
        </Card>
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
import MultiSelect from 'primevue/multiselect'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'
import Message from 'primevue/message'
import { QuillEditor } from '@vueup/vue-quill'
import 'quill/dist/quill.snow.css'

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
  instruction: '',
  logo: '',
  screenshots: [],
  deleteScreenshotIds: []
})

const existingScreenshots = ref([])
const newScreenshotPreviews = ref([])
const logoPreview = ref('')
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

const editorOptions = {
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      [{ header: [1, 2, 3, false] }],
      [{ list: 'ordered' }, { list: 'bullet' }],
      [{ indent: '-1' }, { indent: '+1' }],
      ['link', 'clean']
    ]
  },
  placeholder: 'Write here...'
}

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
      instruction: g.instruction || '',
      logo: '',
      screenshots: [],
      deleteScreenshotIds: []
    }
    logoPreview.value = ''
    existingScreenshots.value = (g.screenshotUrls || []).map(url => {
      const parts = url.split('/')
      return { id: Number(parts[parts.length - 1]), url }
    })
    newScreenshotPreviews.value = []
    allGenres.value = filterRes.data.data.genres || []
  } finally {
    loading.value = false
  }
})

function handleLogoUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    form.value.logo = ev.target.result
    logoPreview.value = ev.target.result
  }
  reader.readAsDataURL(file)
}

function handleScreenshotsUpload(e) {
  const files = e.target.files
  for (const file of files) {
    const reader = new FileReader()
    reader.onload = (ev) => {
      const base64 = ev.target.result
      form.value.screenshots.push(base64)
      newScreenshotPreviews.value.push(base64)
    }
    reader.readAsDataURL(file)
  }
}

function removeExistingScreenshot(id) {
  existingScreenshots.value = existingScreenshots.value.filter(s => s.id !== id)
  form.value.deleteScreenshotIds.push(id)
}

function removeNewScreenshot(index) {
  form.value.screenshots.splice(index, 1)
  newScreenshotPreviews.value.splice(index, 1)
}

async function handleSave() {
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const response = await gamesApi.editGame(route.params.id, form.value)
    game.value = response.data.data
    if (game.value.logoUrl) game.value.logoUrl += '?t=' + Date.now()
    existingScreenshots.value = (response.data.data.screenshotUrls || []).map(url => {
      const parts = url.split('/')
      return { id: Number(parts[parts.length - 1]), url }
    })
    newScreenshotPreviews.value = []
    form.value.screenshots = []
    form.value.deleteScreenshotIds = []
    form.value.logo = ''
    logoPreview.value = ''
    success.value = 'Game saved successfully'
  } catch (e) {
    if (e.response?.status === 413) {
      error.value = 'File size limit exceeded (max 50MB). Compress images or upload fewer screenshots.'
    } else {
      error.value = e.response?.data?.message || 'Failed to save game'
    }
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
.quill-editor {
  border: 1px solid var(--p-content-border-color);
  border-radius: 6px;
}
.quill-editor :deep(.ql-toolbar) {
  border: none;
  border-radius: 6px 6px 0 0;
}
.quill-editor :deep(.ql-container) {
  border: none;
  flex: 1;
  overflow: auto;
  min-height: 0;
  border-radius: 0 0 6px 6px;
}
.logo-preview {
  width: 150px;
  height: 150px;
  object-fit: cover;
  border-radius: 6px;
}
.hidden-input {
  display: none;
}
.screenshots-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
.screenshot-item {
  position: relative;
  width: 120px;
  height: 90px;
  border-radius: 6px;
  overflow: hidden;
}
.screenshot-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.screenshot-delete-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}
.screenshot-item:hover .screenshot-delete-btn {
  opacity: 1;
}
:deep(.p-multiselect-label) {
  flex-wrap: wrap;
}
.screenshot-add-box {
  width: 120px;
  height: 90px;
  border: 2px dashed var(--p-content-border-color);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  cursor: pointer;
  color: var(--p-text-muted-color);
  transition: border-color 0.2s, color 0.2s;
}
.screenshot-add-box:hover {
  border-color: var(--p-primary-color);
  color: var(--p-primary-color);
}
</style>
