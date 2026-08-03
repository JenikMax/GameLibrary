<!-- Форма правил смарт-коллекции (readonly и редактируемая). Поля: платформы, жанры, годы (от/до), минимальный рейтинг, теги, часть названия. Через v-model общается с родителем, очищая пустые поля. -->
<template>
  <!-- Режим только для чтения (отображение правил) -->
  <div v-if="readonly" class="smart-rules-display">
    <div v-if="isEmpty" class="text-color-secondary text-sm">{{ t('collections.smart_rules_empty') }}</div>
    <div v-else class="flex flex-column gap-1">
      <div v-if="localRules.platforms?.length">
        <strong>{{ t('collections.smart_rules_platforms') }}:</strong> {{ localRules.platforms.join(', ') }}
      </div>
      <div v-if="localRules.genres?.length">
        <strong>{{ t('collections.smart_rules_genres') }}:</strong> {{ genreNames }}
      </div>
      <div v-if="localRules.yearFrom || localRules.yearTo">
        <strong>{{ t('collections.smart_rules_year_from') }}:</strong>
        {{ localRules.yearFrom || '—' }} — {{ localRules.yearTo || '—' }}
      </div>
      <div v-if="localRules.minRating">
        <strong>{{ t('collections.smart_rules_min_rating') }}:</strong> {{ localRules.minRating }}
      </div>
      <div v-if="localRules.tags?.length">
        <strong>{{ t('collections.smart_rules_tags') }}:</strong> {{ tagNames }}
      </div>
      <div v-if="localRules.nameContains">
        <strong>{{ t('collections.smart_rules_name') }}:</strong> {{ localRules.nameContains }}
      </div>
    </div>
  </div>
  <!-- Режим редактирования -->
  <div v-else class="smart-rules-form flex flex-column gap-3">
    <!-- Платформы -->
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
    <!-- Жанры (с optionLabel/optionValue для отображения названий) -->
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
    <!-- Диапазон годов -->
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
    <!-- Минимальный рейтинг -->
    <div class="field">
      <label>{{ t('collections.smart_rules_min_rating') }}</label>
      <InputNumber v-model="localRules.minRating" class="w-full" :min="1" :max="10" @update:modelValue="emitUpdate" />
    </div>
    <!-- Теги -->
    <div class="field">
      <label>{{ t('collections.smart_rules_tags') }}</label>
      <MultiSelect
        v-model="localRules.tags"
        :options="tagOptions"
        optionLabel="name"
        optionValue="code"
        display="chip"
        class="w-full"
        @update:modelValue="emitUpdate"
      />
    </div>
    <!-- Часть названия -->
    <div class="field">
      <label>{{ t('collections.smart_rules_name') }}</label>
      <InputText v-model="localRules.nameContains" class="w-full" @update:modelValue="emitUpdate" />
    </div>
  </div>
</template>

<script setup>
// Правила смарт-коллекции: v-model поддерживает как объект, так и JSON-строку. В режиме readonly отображает правила текстом, в режиме edit — форму с MultiSelect/InputNumber/InputText.
import { reactive, computed, watch } from 'vue'
import { useI18n } from '../composables/useI18n'
import { useLibraryStore } from '../stores/library'
import MultiSelect from 'primevue/multiselect'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'

const { t } = useI18n()
const libraryStore = useLibraryStore()

const props = defineProps({
  modelValue: { type: [Object, String], default: () => ({}) },
  options: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

// Локальная реактивная копия правил
const localRules = reactive(parseRules(props.modelValue))

// Синхронизация при изменении modelValue извне
watch(() => props.modelValue, (val) => {
  const parsed = parseRules(val)
  Object.assign(localRules, parsed)
}, { deep: true })

// Парсинг modelValue: строка → JSON, объект — как есть, с дефолтными полями
function parseRules(val) {
  if (!val) return { platforms: [], genres: [], yearFrom: null, yearTo: null, minRating: null, tags: [], nameContains: '' }
  if (typeof val === 'string') {
    try { return { platforms: [], genres: [], yearFrom: null, yearTo: null, minRating: null, tags: [], nameContains: '', ...JSON.parse(val) } }
    catch { return { platforms: [], genres: [], yearFrom: null, yearTo: null, minRating: null, tags: [], nameContains: '' } }
  }
  return { platforms: [], genres: [], yearFrom: null, yearTo: null, minRating: null, tags: [], nameContains: '', ...val }
}

// Эмит изменений, исключая пустые поля
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
const tagOptions = computed(() => props.options.tagItems || [])

// Маппинг кода жанра → локализованное название
const genreMap = computed(() => {
  const map = {}
  for (const g of genreOptions.value) {
    map[g.code] = g.name
  }
  return map
})

const genreNames = computed(() => (localRules.genres || []).map(c => genreMap.value[c] || c).join(', '))
const tagNames = computed(() => (localRules.tags || []).map(c => libraryStore.tagMap[c] || c).join(', '))

// Проверка: пустые ли все правила
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
