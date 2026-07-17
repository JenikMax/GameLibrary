<template>
  <div class="tag-input" ref="containerRef">
    <div class="tag-input-chips flex flex-wrap gap-1 mb-1" v-if="modelValue?.length">
      <span
        v-for="tag in modelValue"
        :key="tag"
        class="tag-chip flex align-items-center gap-1 px-2 py-1 border-round"
      >
        {{ tag }}
        <i class="pi pi-times-circle tag-chip-remove" @click.stop="removeTag(tag)" />
      </span>
    </div>
    <div class="tag-input-row flex align-items-center gap-1">
      <InputText
        v-model="newTag"
        :placeholder="placeholder"
        class="flex-1"
        @keydown.enter.prevent="addNewTag"
        @focus="showDropdown = true"
      />
      <Button
        icon="pi pi-plus"
        size="small"
        text
        rounded
        @click="addNewTag"
        :disabled="!newTag.trim()"
      />
      <Button
        :icon="showDropdown ? 'pi pi-chevron-up' : 'pi pi-chevron-down'"
        size="small"
        text
        rounded
        @click="showDropdown = !showDropdown"
      />
    </div>
    <div v-if="showDropdown" class="tag-dropdown mt-1 border-round">
      <div class="p-1">
        <InputText
          v-model="filterQuery"
          :placeholder="filterPlaceholder"
          class="w-full"
          size="small"
        />
      </div>
      <div class="tag-dropdown-list">
        <div
          v-for="tag in filteredTags"
          :key="tag"
          class="tag-dropdown-item px-2 py-1 cursor-pointer border-round"
          @click="selectTag(tag)"
        >
          {{ tag }}
        </div>
        <div v-if="!filteredTags.length" class="px-2 py-1 text-color-secondary text-sm text-center">
          {{ emptyText }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  allTags: { type: Array, default: () => [] },
  placeholder: { type: String, default: '' },
  filterPlaceholder: { type: String, default: '' },
  emptyText: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const newTag = ref('')
const filterQuery = ref('')
const showDropdown = ref(false)
const containerRef = ref(null)

const filteredTags = computed(() => {
  const selected = new Set(props.modelValue || [])
  const q = filterQuery.value.toLowerCase().trim()
  return props.allTags.filter(tag => {
    if (selected.has(tag)) return false
    if (q && !tag.toLowerCase().includes(q)) return false
    return true
  })
})

function addNewTag() {
  const tag = newTag.value.trim()
  if (!tag) return
  const current = [...(props.modelValue || [])]
  if (current.includes(tag)) {
    newTag.value = ''
    return
  }
  current.push(tag)
  emit('update:modelValue', current)
  newTag.value = ''
  filterQuery.value = ''
}

function selectTag(tag) {
  const current = [...(props.modelValue || [])]
  if (!current.includes(tag)) {
    current.push(tag)
    emit('update:modelValue', current)
  }
  newTag.value = ''
  filterQuery.value = ''
}

function removeTag(tag) {
  const current = (props.modelValue || []).filter(t => t !== tag)
  emit('update:modelValue', current)
}

function onClickOutside(e) {
  if (containerRef.value && !containerRef.value.contains(e.target)) {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onClickOutside, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside, true)
})
</script>

<style scoped>
.tag-input {
  width: 100%;
}
.tag-chip {
  background: var(--p-highlight-background);
  color: var(--p-highlight-color);
  font-size: 0.875rem;
  border: 1px solid var(--p-highlight-background);
}
.tag-chip-remove {
  cursor: pointer;
  font-size: 0.7rem;
  opacity: 0.7;
  transition: opacity 0.2s;
}
.tag-chip-remove:hover {
  opacity: 1;
}
.tag-input-row {
  width: 100%;
}
.tag-dropdown {
  border: 1px solid var(--p-inputborder-color);
  background: var(--p-input-background);
  max-height: 220px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.tag-dropdown-list {
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}
.tag-dropdown-item:hover {
  background: var(--p-list-option-focus-background);
  color: var(--p-list-option-focus-color);
}
</style>
