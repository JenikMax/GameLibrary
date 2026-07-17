<template>
  <div v-if="readonly" class="smart-rules-display">
    <div v-if="isEmpty" class="text-color-secondary text-sm">{{ t('collections.smart_rules_empty') }}</div>
    <div v-else class="flex flex-column gap-1">
      <div v-if="rules.platforms?.length">
        <strong>{{ t('collections.smart_rules_platforms') }}:</strong> {{ rules.platforms.join(', ') }}
      </div>
      <div v-if="rules.genres?.length">
        <strong>{{ t('collections.smart_rules_genres') }}:</strong> {{ genreNames }}
      </div>
      <div v-if="rules.yearFrom || rules.yearTo">
        <strong>{{ t('collections.smart_rules_year_from') }}:</strong>
        {{ rules.yearFrom || '—' }} — {{ rules.yearTo || '—' }}
      </div>
      <div v-if="rules.minRating">
        <strong>{{ t('collections.smart_rules_min_rating') }}:</strong> {{ rules.minRating }}
      </div>
      <div v-if="rules.tags?.length">
        <strong>{{ t('collections.smart_rules_tags') }}:</strong> {{ tagNames }}
      </div>
      <div v-if="rules.nameContains">
        <strong>{{ t('collections.smart_rules_name') }}:</strong> {{ rules.nameContains }}
      </div>
    </div>
  </div>
  <div v-else class="smart-rules-form flex flex-column gap-3">
    <div class="field">
      <label>{{ t('collections.smart_rules_platforms') }}</label>
      <MultiSelect
        v-model="localRules.platforms"
        :options="options.platforms || []"
        display="chip"
        class="w-full"
        @update:modelValue="emitUpdate"
      />
    </div>
    <div class="field">
      <label>{{ t('collections.smart_rules_genres') }}</label>
      <MultiSelect
        v-model="localRules.genres"
        :options="genreOptions"
        optionLabel="name"
        optionValue="code"
        display="chip"
        class="w-full"
        @update:modelValue="emitUpdate"
      />
    </div>
    <div class="flex gap-3">
      <div class="field flex-1">
        <label>{{ t('collections.smart_rules_year_from') }}</label>
        <InputNumber v-model="localRules.yearFrom" class="w-full" :min="1900" :max="2100" @update:modelValue="emitUpdate" />
      </div>
      <div class="field flex-1">
        <label>{{ t('collections.smart_rules_year_to') }}</label>
        <InputNumber v-model="localRules.yearTo" class="w-full" :min="1900" :max="2100" @update:modelValue="emitUpdate" />
      </div>
    </div>
    <div class="field">
      <label>{{ t('collections.smart_rules_min_rating') }}</label>
      <InputNumber v-model="localRules.minRating" class="w-full" :min="1" :max="10" @update:modelValue="emitUpdate" />
    </div>
    <div class="field">
      <label>{{ t('collections.smart_rules_tags') }}</label>
      <MultiSelect
        v-model="localRules.tags"
        :options="tagOptions"
        display="chip"
        class="w-full"
        @update:modelValue="emitUpdate"
      />
    </div>
    <div class="field">
      <label>{{ t('collections.smart_rules_name') }}</label>
      <InputText v-model="localRules.nameContains" class="w-full" @update:modelValue="emitUpdate" />
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'
import { useI18n } from '../composables/useI18n'
import MultiSelect from 'primevue/multiselect'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'

const { t } = useI18n()

const props = defineProps({
  modelValue: { type: [Object, String], default: () => ({}) },
  options: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const localRules = reactive(parseRules(props.modelValue))

watch(() => props.modelValue, (val) => {
  const parsed = parseRules(val)
  Object.assign(localRules, parsed)
}, { deep: true })

function parseRules(val) {
  if (!val) return { platforms: [], genres: [], yearFrom: null, yearTo: null, minRating: null, tags: [], nameContains: '' }
  if (typeof val === 'string') {
    try { return { platforms: [], genres: [], yearFrom: null, yearTo: null, minRating: null, tags: [], nameContains: '', ...JSON.parse(val) } }
    catch { return { platforms: [], genres: [], yearFrom: null, yearTo: null, minRating: null, tags: [], nameContains: '' } }
  }
  return { platforms: [], genres: [], yearFrom: null, yearTo: null, minRating: null, tags: [], nameContains: '', ...val }
}

function emitUpdate() {
  const result = {}
  if (localRules.platforms?.length) result.platforms = localRules.platforms
  if (localRules.genres?.length) result.genres = localRules.genres
  if (localRules.yearFrom) result.yearFrom = localRules.yearFrom
  if (localRules.yearTo) result.yearTo = localRules.yearTo
  if (localRules.minRating) result.minRating = localRules.minRating
  if (localRules.tags?.length) result.tags = localRules.tags
  if (localRules.nameContains?.trim()) result.nameContains = localRules.nameContains.trim()
  emit('update:modelValue', result)
}

const genreOptions = computed(() => props.options.genres || [])
const tagOptions = computed(() => props.options.tags || [])

const genreMap = computed(() => {
  const map = {}
  for (const g of genreOptions.value) {
    map[g.code] = g.name
  }
  return map
})

const genreNames = computed(() => (localRules.genres || []).map(c => genreMap.value[c] || c).join(', '))
const tagNames = computed(() => (localRules.tags || []).join(', '))

const isEmpty = computed(() => {
  return !localRules.platforms?.length
    && !localRules.genres?.length
    && !localRules.yearFrom
    && !localRules.yearTo
    && !localRules.minRating
    && !localRules.tags?.length
    && !localRules.nameContains?.trim()
})
</script>

<style scoped>
.smart-rules-form .field label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.25rem;
  font-size: 0.875rem;
}
.smart-rules-display strong {
  font-size: 0.875rem;
}
</style>
