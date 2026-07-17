<template>
  <div v-if="loading" class="flex justify-content-center p-5">
    <ProgressSpinner />
  </div>
  <div v-else-if="game" class="edit-container">
    <div class="flex align-items-center gap-3 mb-3">
      <Button icon="pi pi-arrow-left" text @click="$router.push(`/game/${game.id}`)" />
      <h2 class="m-0">{{ t('game.edit_title') }} {{ game.name }}</h2>
    </div>

    <Message v-if="error" severity="error" :closable="false" class="mb-3">{{ error }}</Message>
    <Message v-if="success" severity="success" :closable="false" class="mb-3">{{ success }}</Message>

    <div class="grid">
      <div class="col-12 md:col-8">
        <Card>
          <template #content>
            <div class="formgrid grid">
              <div class="field col-12 md:col-6">
                <label for="name">{{ t('game.field.name') }}</label>
                <InputText id="name" v-model="form.name" class="w-full" :class="{ 'p-invalid': errors.name }" />
                <small v-if="errors.name" class="p-error">{{ errors.name }}</small>
              </div>
              <div class="field col-12 md:col-6">
                <label for="platform">{{ t('game.field.platform') }}</label>
                <InputText id="platform" v-model="form.platform" class="w-full" :class="{ 'p-invalid': errors.platform }" />
                <small v-if="errors.platform" class="p-error">{{ errors.platform }}</small>
              </div>
              <div class="field col-12 md:col-4">
                <label for="releaseDate">{{ t('game.field.release_date') }}</label>
                <InputText id="releaseDate" v-model="form.releaseDate" class="w-full" />
              </div>
              <div class="field col-12 md:col-4">
                <label for="directoryPath">{{ t('game.field.directory_path') }}</label>
                <InputText id="directoryPath" v-model="form.directoryPath" class="w-full" />
              </div>
              <div class="field col-12 md:col-4">
                <label for="trailerUrl">{{ t('game.field.trailer_url') }}</label>
                <InputText id="trailerUrl" v-model="form.trailerUrl" class="w-full" />
              </div>
              <div class="field col-12">
                <label for="genres">{{ t('game.field.genres') }}</label>
                <MultiSelect
                  id="genres"
                  v-model="form.genres"
                  :options="allGenres"
                  optionLabel="name"
                  optionValue="code"
                  :placeholder="t('filter.genres_placeholder')"
                  display="chip"
                  filter
                  :filterPlaceholder="t('filter.genres_search')"
                  class="w-full"
                  scrollHeight="400px"
                />
              </div>
              <div class="field col-12">
                <label for="tags">{{ t('filter.tags') }}</label>
                <TagInput
                  v-model="form.tags"
                  :allTags="allTags"
                  :placeholder="t('tags.input_placeholder')"
                  :filterPlaceholder="t('tags.filter_placeholder')"
                  :emptyText="t('tags.empty')"
                />
              </div>
              <div class="field col-12">
                <label for="description">{{ t('game.field.description') }}</label>
                <QuillEditor v-model:content="form.description" content-type="html"
                  :options="editorOptions" class="quill-editor"
                  style="height:250px;display:flex;flex-direction:column" />
              </div>
              <div class="field col-12">
                <label for="instruction">{{ t('game.field.instruction') }}</label>
                <QuillEditor v-model:content="form.instruction" content-type="html"
                  :options="editorOptions" class="quill-editor"
                  style="height:250px;display:flex;flex-direction:column" />
              </div>
              <div class="field col-12">
                <label>{{ t('game.field.screenshots') }}</label>
                <div class="screenshots-grid">
                  <div v-for="ss in existingScreenshots" :key="ss.id" class="screenshot-item">
                    <img :src="ss.url" alt="screenshot"
                         class="screenshot-thumb img-fade"
                         :class="{ loaded: existingSsLoaded[ss.id] }"
                         @load="existingSsLoaded[ss.id] = true"
                         @error="existingSsLoaded[ss.id] = true" />
                    <Button icon="pi pi-trash" severity="danger" text rounded size="small"
                      class="screenshot-delete-btn" @click="removeExistingScreenshot(ss.id)" />
                  </div>
                  <div v-for="(preview, i) in newScreenshotPreviews" :key="'new-'+i" class="screenshot-item">
                    <img :src="preview" alt="new screenshot" class="screenshot-thumb img-fade loaded" />
                    <Button icon="pi pi-trash" severity="danger" text rounded size="small"
                      class="screenshot-delete-btn" @click="removeNewScreenshot(i)" />
                  </div>
                  <div class="screenshot-add-box" @click="$refs.screenshotsInput.click()">
                    <i class="pi pi-plus text-2xl"></i>
                    <span class="text-sm">{{ t('game.field.screenshots_add') }}</span>
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
          <template #title>{{ t('game.logo') }}</template>
          <template #content>
            <div class="flex flex-column align-items-center gap-2">
              <img v-if="logoPreview" :src="logoPreview" class="logo-preview img-fade loaded" alt="logo preview" />
              <img v-else
                   :src="game.logo || game.logoUrl || '/game-library/img/default.jpg'"
                   class="logo-preview img-fade"
                   :class="{ loaded: logoLoaded }"
                   alt="current logo"
                   @load="logoLoaded = true"
                   @error="logoLoaded = true" />
              <Button :label="t('game.change_logo')" icon="pi pi-image" severity="secondary" size="small"
                @click="$refs.logoInput.click()" />
              <input ref="logoInput" type="file" accept="image/*" class="hidden-input" @change="handleLogoUpload" />
            </div>
          </template>
        </Card>
        <Card>
          <template #title>{{ t('game.scraper') }}</template>
          <template #content>
            <div class="field">
              <label for="scrapeSource">{{ t('game.scraper.source') }}</label>
              <Select
                id="scrapeSource"
                v-model="scrape.source"
                :options="scrapeSources"
                optionLabel="label"
                optionValue="value"
                class="w-full"
              />
            </div>
            <div class="field">
              <label for="scrapeUrl">{{ t('game.scraper.url') }}</label>
              <InputText id="scrapeUrl" v-model="scrape.url" class="w-full" />
            </div>
            <div class="field flex flex-column gap-2">
              <label>{{ t('game.scraper.fields') }}</label>
              <div v-for="opt in scrapeFields" :key="opt.key" class="flex align-items-center gap-2">
                <Checkbox v-model="scrape[opt.key]" :binary="true" :inputId="'scrape-' + opt.key" />
                <label :for="'scrape-' + opt.key">{{ t(opt.labelKey) }}</label>
              </div>
            </div>
            <Button :label="t('game.scraper.scrape')" icon="pi pi-cloud-download" @click="handleScrape" :loading="scraping" class="w-full" />
          </template>
        </Card>
      </div>
    </div>

    <div class="flex justify-content-end mt-3 gap-2">
      <Button :label="t('common.cancel')" severity="secondary" @click="$router.push(`/game/${game.id}`)" />
      <Button :label="t('common.save')" icon="pi pi-check" @click="handleSave" :loading="saving" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '../composables/useI18n'
import { gamesApi } from '../api/games'
import { useLocaleStore } from '../stores/locale'
import { useLibraryStore } from '../stores/library'
import ProgressSpinner from 'primevue/progressspinner'
import Button from 'primevue/button'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import MultiSelect from 'primevue/multiselect'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'
import Message from 'primevue/message'
import TagInput from '../components/TagInput.vue'
import { QuillEditor } from '@vueup/vue-quill'
import 'quill/dist/quill.snow.css'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const localeStore = useLocaleStore()
const libraryStore = useLibraryStore()

const game = ref(null)
const loading = ref(true)
const saving = ref(false)
const scraping = ref(false)
const error = ref('')
const success = ref('')
const logoLoaded = ref(false)
const existingSsLoaded = reactive({})
const errors = ref({})

const form = ref({
  name: '',
  platform: '',
  releaseDate: '',
  directoryPath: '',
  trailerUrl: '',
  genres: [],
  tags: [],
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
const allTags = ref([])
const scrapeSources = ref([])
const scrapeFields = [
  { key: 'title', labelKey: 'game.scraper.field.title' },
  { key: 'poster', labelKey: 'game.scraper.field.poster' },
  { key: 'description', labelKey: 'game.scraper.field.description' },
  { key: 'year', labelKey: 'game.scraper.field.year' },
  { key: 'genres', labelKey: 'game.scraper.field.genres' },
  { key: 'screens', labelKey: 'game.scraper.field.screenshots' },
  { key: 'instruction', labelKey: 'game.scraper.field.instruction' }
]
const scrape = ref({
  source: '',
  url: '',
  title: true,
  poster: true,
  description: true,
  year: true,
  genres: true,
  screens: true,
  instruction: true
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
  placeholder: t('game.editor_placeholder')
}

onMounted(async () => {
  try {
    const [gameRes, filterRes, scraperRes] = await Promise.all([
      gamesApi.getGame(route.params.id),
      gamesApi.getFilterOptions(),
      gamesApi.getScrapers()
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
      tags: g.tags || [],
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
    allTags.value = filterRes.data.data.tags || []
    const sources = (scraperRes.data.data || []).map(s => ({ label: s.displayName || s.type, value: s.type }))
    scrapeSources.value = sources
    if (sources.length > 0 && !scrape.value.source) {
      scrape.value.source = sources[0].value
    }
  } finally {
    loading.value = false
  }
})

watch(() => localeStore.locale, async () => {
  try {
    const res = await gamesApi.getFilterOptions()
    allGenres.value = res.data.data.genres || []
    allTags.value = res.data.data.tags || []
  } catch { /* ignore */ }
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
  errors.value = {}
  if (!form.value.name?.trim()) {
    errors.value.name = t('game.field_required')
  }
  if (!form.value.platform?.trim()) {
    errors.value.platform = t('game.field_required')
  }
  if (Object.keys(errors.value).length) return

  saving.value = true
  error.value = ''
  try {
    await gamesApi.editGame(route.params.id, form.value)
    await libraryStore.fetchFilterOptions()
    await router.replace(`/game/${route.params.id}`)
  } catch (e) {
    if (e.response?.status === 413) {
      error.value = t('game.file_size_error')
    } else {
      error.value = e.response?.data?.message || t('game.save_failed')
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
      tags: g.tags || form.value.tags || [],
      description: g.description || form.value.description,
      instruction: g.instruction || form.value.instruction,
      logo: g.logo || form.value.logo || '',
      screenshots: [...(new Set([...(form.value.screenshots || []), ...(g.screenshots || [])]))],
      deleteScreenshotIds: form.value.deleteScreenshotIds || []
    }
    if (g.logo) {
      logoPreview.value = g.logo
    }
    if (g.screenshots && g.screenshots.length > 0) {
      newScreenshotPreviews.value = [...g.screenshots]
    }
    success.value = t('game.scrape_success')
  } catch (e) {
    error.value = e.response?.data?.message || t('game.scrape_failed')
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
  border-radius: 6px 0 0 0;
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
:deep(.p-multiselect-option) {
  cursor: pointer;
}
:deep(.p-multiselect-option *) {
  pointer-events: auto !important;
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
