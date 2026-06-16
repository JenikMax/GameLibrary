<template>
  <Accordion :activeIndex="0">
    <AccordionTab header="Filters">
      <div class="flex flex-column gap-3 p-2">
        <div class="field">
          <label for="search">Search</label>
          <IconField>
            <InputIcon>
              <i class="pi pi-search" />
            </InputIcon>
            <InputText
              id="search"
              v-model="searchText"
              placeholder="Search games..."
              @keyup.enter="applyFilters"
              class="w-full"
            />
          </IconField>
        </div>

        <div class="field">
          <label>Platforms</label>
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
          <label>Years</label>
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
          <label>Genres</label>
          <MultiSelect
            v-model="selectedGenres"
            :options="options.genres"
            optionLabel="name"
            optionValue="code"
            placeholder="Select genres"
            class="w-full"
            :maxSelectedLabels="3"
          />
        </div>

        <div class="field">
          <label>Sort by</label>
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
          <Button label="Apply" icon="pi pi-filter" @click="applyFilters" class="flex-1" />
          <Button label="Reset" icon="pi pi-times" severity="secondary" @click="resetFilters" />
        </div>
      </div>
    </AccordionTab>
  </Accordion>
</template>

<script setup>
import { ref, watch } from 'vue'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import InputText from 'primevue/inputtext'
import InputIcon from 'primevue/inputicon'
import IconField from 'primevue/iconfield'
import Chip from 'primevue/chip'
import MultiSelect from 'primevue/multiselect'
import SelectButton from 'primevue/selectbutton'
import Button from 'primevue/button'

const props = defineProps({
  options: { type: Object, required: true },
  modelValue: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['apply', 'reset'])

const searchText = ref(props.modelValue.searchText || '')
const selectedPlatforms = ref(props.modelValue.selectedPlatforms || [])
const selectedYears = ref(props.modelValue.selectedYears || [])
const selectedGenres = ref(props.modelValue.selectedGenres || [])
const sortField = ref(props.modelValue.sortField || '')
const sortType = ref(props.modelValue.sortType || '')

const sortOptions = [
  { label: 'Name', value: 'name' },
  { label: 'Year', value: 'year' },
  { label: 'Date added', value: 'create' }
]
const sortTypeOptions = [
  { label: 'Asc', value: 'asc' },
  { label: 'Desc', value: 'desc' }
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
</style>
