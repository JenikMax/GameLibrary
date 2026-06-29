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

        <div class="field">
          <label>{{ t('filter.sort_by') }}</label>
          <SelectButton
            v-model="sortField"
            :options="sortOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>
        <div v-if="sortField" class="field">
          <SelectButton
            v-model="sortType"
            :options="sortTypeOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>

        <div class="flex gap-2">
          <Button :label="t('filter.apply')" icon="pi pi-filter" @click="applyFilters" class="flex-1" />
          <Button :label="t('filter.reset')" icon="pi pi-times" severity="secondary" @click="resetFilters" />
        </div>
      </div>
    </AccordionTab>
  </Accordion>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from '../composables/useI18n'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import InputText from 'primevue/inputtext'
import InputIcon from 'primevue/inputicon'
import IconField from 'primevue/iconfield'
import Chip from 'primevue/chip'
import MultiSelect from 'primevue/multiselect'
import SelectButton from 'primevue/selectbutton'
import Button from 'primevue/button'

const { t } = useI18n()

const props = defineProps({
  options: { type: Object, required: true }
})

const emit = defineEmits(['apply', 'reset'])

const searchText = ref('')
const selectedPlatforms = ref([])
const selectedYears = ref([])
const selectedGenres = ref([])
const sortField = ref('')
const sortType = ref('')

function restoreState(state) {
  searchText.value = state.searchText || ''
  selectedPlatforms.value = state.selectedPlatforms || []
  selectedYears.value = state.selectedYears || []
  selectedGenres.value = state.selectedGenres || []
  sortField.value = state.sortField || ''
  sortType.value = state.sortType || ''
}

defineExpose({ restoreState })

const sortOptions = [
  { label: t('filter.sort_name'), value: 'name' },
  { label: t('filter.sort_year'), value: 'year' },
  { label: t('filter.sort_date'), value: 'create' }
]
const sortTypeOptions = [
  { label: t('filter.asc'), value: 'asc' },
  { label: t('filter.desc'), value: 'desc' }
]

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
    searchText: searchText.value,
    platforms: [...selectedPlatforms.value],
    years: [...selectedYears.value],
    genres: [...selectedGenres.value],
    sortField: sortField.value,
    sortType: sortType.value
  })
}

function resetFilters() {
  searchText.value = ''
  selectedPlatforms.value = []
  selectedYears.value = []
  selectedGenres.value = []
  sortField.value = ''
  sortType.value = ''
  emit('reset')
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
