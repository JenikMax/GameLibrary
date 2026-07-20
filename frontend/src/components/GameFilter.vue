<template>
  <Accordion :activeIndex="0">
    <AccordionTab :header="t('filter.search')">
      <div class="flex flex-column gap-3 p-2">
        <div class="field">
          <label for="search">{{ t('filter.search') }}</label>
          <IconField>
            <InputIcon>
              <i class="pi pi-search" />
            </InputIcon>
            <InputText
              id="search"
              v-model="searchText"
              :placeholder="t('filter.search_placeholder')"
              @keyup.enter="applyFilters"
              class="w-full"
            />
          </IconField>
        </div>

        <div class="field" v-if="options.semanticAvailable">
          <div class="flex align-items-center gap-2">
            <ToggleSwitch v-model="semanticSearch" @change="applyFilters" />
            <label>{{ t('filter.semantic') }}</label>
          </div>
          <small class="text-color-secondary ml-1">{{ t('filter.semantic_hint') }}</small>
        </div>

        <div class="field">
          <label>{{ t('filter.platforms') }}</label>
          <div class="flex flex-wrap gap-2">
            <Chip
              v-for="p in options.platforms"
              :key="p"
              :label="p"
              :class="{ 'chip-selected': selectedPlatforms.includes(p) }"
              @click="togglePlatform(p)"
            />
          </div>
        </div>

        <div class="field">
          <label>{{ t('filter.years') }}</label>
          <div class="flex flex-wrap gap-2">
            <Chip
              v-for="y in options.years"
              :key="y"
              :label="y"
              :class="{ 'chip-selected': selectedYears.includes(y) }"
              @click="toggleYear(y)"
            />
          </div>
        </div>

        <div class="field">
          <label>{{ t('filter.genres') }}</label>
          <MultiSelect
            v-model="selectedGenres"
            :options="options.genres"
            optionLabel="name"
            optionValue="code"
            :placeholder="t('filter.genres_placeholder')"
            filter
            :filterPlaceholder="t('filter.genres_search')"
            display="chip"
            class="w-full"
            scrollHeight="400px"
          />
        </div>

        <div class="field" v-if="options.tags && options.tags.length">
          <label>{{ t('filter.tags') }}</label>
          <MultiSelect
            v-model="selectedTags"
            :options="options.tags"
            :placeholder="t('filter.tags_placeholder')"
            filter
            display="chip"
            class="w-full"
            scrollHeight="300px"
          />
        </div>

        <div class="flex gap-2">
          <Button :label="t('filter.reset')" icon="pi pi-times" severity="secondary" @click="resetFilters" class="flex-1" />
        </div>
      </div>
    </AccordionTab>
  </Accordion>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useI18n } from '../composables/useI18n'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import InputText from 'primevue/inputtext'
import InputIcon from 'primevue/inputicon'
import IconField from 'primevue/iconfield'
import Chip from 'primevue/chip'
import MultiSelect from 'primevue/multiselect'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'

const { t } = useI18n()

function debounce(fn, delay) {
  let timer
  return function (...args) {
    clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
}

const props = defineProps({
  options: { type: Object, required: true }
})

const emit = defineEmits(['apply', 'reset'])

const searchText = ref('')
const selectedPlatforms = ref([])
const selectedYears = ref([])
const selectedGenres = ref([])
const selectedTags = ref([])
const semanticSearch = ref(false)
const resetting = ref(false)

function restoreState(state) {
  resetting.value = true
  searchText.value = state.searchText || state.search || ''
  selectedPlatforms.value = state.selectedPlatforms || state.platforms || []
  selectedYears.value = state.selectedYears || state.years || []
  selectedGenres.value = state.selectedGenres || state.genres || []
  selectedTags.value = state.selectedTags || state.tags || []
  semanticSearch.value = state.semanticSearch || state.semantic || false
  setTimeout(() => { resetting.value = false }, 300)
}

defineExpose({ restoreState, semanticSearch })

const debouncedApply = debounce(() => {
  if (!resetting.value) applyFilters()
}, 250)

watch(searchText, debouncedApply)
watch(selectedPlatforms, debouncedApply, { deep: true })
watch(selectedYears, debouncedApply, { deep: true })
watch(selectedGenres, debouncedApply, { deep: true })
watch(selectedTags, debouncedApply, { deep: true })

function togglePlatform(p) {
  const idx = selectedPlatforms.value.indexOf(p)
  if (idx >= 0) selectedPlatforms.value.splice(idx, 1)
  else selectedPlatforms.value.push(p)
}

function toggleYear(y) {
  const idx = selectedYears.value.indexOf(y)
  if (idx >= 0) selectedYears.value.splice(idx, 1)
  else selectedYears.value.push(y)
}

function applyFilters() {
  emit('apply', {
    search: searchText.value,
    platforms: [...selectedPlatforms.value],
    years: [...selectedYears.value],
    genres: [...selectedGenres.value],
    tags: [...selectedTags.value],
    semantic: semanticSearch.value
  })
}

function resetFilters() {
  resetting.value = true
  searchText.value = ''
  selectedPlatforms.value = []
  selectedYears.value = []
  selectedGenres.value = []
  selectedTags.value = []
  semanticSearch.value = false
  emit('reset')
  setTimeout(() => { resetting.value = false }, 300)
}
</script>

<style scoped>
.chip-selected {
  background: var(--p-primary-color) !important;
  color: var(--p-primary-contrast-color) !important;
}
.field {
  margin-bottom: 0.5rem;
}
.field label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
}
:deep(.p-multiselect-label) {
  flex-wrap: wrap;
}
</style>
