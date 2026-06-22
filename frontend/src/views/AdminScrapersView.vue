<template>
  <div class="admin-scrapers-container">
    <div class="flex align-items-center justify-content-between mb-3">
      <h2>{{ t('admin.scrapers.title') }}</h2>
      <Button :label="t('admin.scrapers.reload')" icon="pi pi-refresh" severity="help" @click="handleReload" :loading="reloading" />
    </div>

    <DataTable :value="scrapers" stripedRows responsiveLayout="scroll" class="p-datatable-sm">
      <Column field="type" :header="t('admin.scrapers.type')" sortable />
      <Column field="displayName" :header="t('admin.scrapers.display_name')" sortable />
      <Column field="enabled" :header="t('admin.scrapers.enabled')" style="width:100px">
        <template #body="slotProps">
          <ToggleSwitch
            :modelValue="slotProps.data.enabled"
            @update:modelValue="(val) => toggleEnabled(slotProps.data, val)"
          />
        </template>
      </Column>
      <Column field="baseUrl" :header="t('admin.scrapers.base_url')" />
      <Column field="apiUrl" :header="t('admin.scrapers.api_url')" />
      <Column :header="t('admin.scrapers.edit')" style="width:80px">
        <template #body="slotProps">
          <Button icon="pi pi-pencil" severity="secondary" text rounded @click="openEdit(slotProps.data)" />
        </template>
      </Column>
    </DataTable>

    <Sidebar v-model:visible="editVisible" :header="editTitle" position="right" style="width:42rem">
      <div v-if="editData" class="flex flex-column gap-3">
        <div class="field">
          <label>{{ t('admin.scrapers.display_name') }}</label>
          <InputText v-model="editData.displayName" class="w-full" />
        </div>
        <div class="field">
          <label>{{ t('admin.scrapers.enabled') }}</label>
          <ToggleSwitch v-model="editData.enabled" />
        </div>
        <div class="field">
          <label>{{ t('admin.scrapers.base_url') }}</label>
          <InputText v-model="editData.baseUrl" class="w-full" />
        </div>
        <div class="field">
          <label>{{ t('admin.scrapers.api_url') }}</label>
          <InputText v-model="editData.apiUrl" class="w-full" />
        </div>
        <div class="field">
          <label>{{ t('admin.scrapers.api_key') }}</label>
          <div class="p-inputgroup">
            <InputText :type="apiKeyVisible ? 'text' : 'password'" v-model="editData.apiKey" class="w-full" />
            <Button :icon="apiKeyVisible ? 'pi pi-eye-slash' : 'pi pi-eye'" severity="secondary" @click="apiKeyVisible = !apiKeyVisible" />
          </div>
        </div>
        <div class="flex gap-3">
          <div class="field flex-1">
            <label>{{ t('admin.scrapers.auth_scheme') }}</label>
            <InputText v-model="editData.authScheme" class="w-full" />
          </div>
          <div class="field flex-1">
            <label>{{ t('admin.scrapers.timeout_ms') }}</label>
            <InputNumber v-model="editData.timeoutMs" class="w-full" />
          </div>
          <div class="field flex-1">
            <label>{{ t('admin.scrapers.max_screenshots') }}</label>
            <InputNumber v-model="editData.maxScreenshots" class="w-full" />
          </div>
        </div>
        <div class="flex gap-3">
          <div class="field flex-1">
            <label>{{ t('admin.scrapers.ssl_protocol') }}</label>
            <InputText v-model="editData.sslProtocol" class="w-full" placeholder="TLSv1.2" />
          </div>
          <div class="field flex-1 flex align-items-end pb-2">
            <div class="flex align-items-center gap-2">
              <ToggleSwitch v-model="editData.trustAllCerts" />
              <label>{{ t('admin.scrapers.trust_all_certs') }}</label>
            </div>
          </div>
        </div>

        <Accordion :multiple="true">
          <AccordionTab v-if="editData.cssSelectors" :header="t('admin.scrapers.css_selectors')">
            <div v-for="(val, key) in editData.cssSelectors" :key="key" class="flex gap-2 mb-2">
              <InputText :modelValue="key" class="flex-1" disabled />
              <InputText v-model="editData.cssSelectors[key]" class="flex-3" />
            </div>
          </AccordionTab>
          <AccordionTab v-if="editData.jsonPaths" :header="t('admin.scrapers.json_paths')">
            <div v-for="(val, key) in editData.jsonPaths" :key="key" class="flex gap-2 mb-2">
              <InputText :modelValue="key" class="flex-1" disabled />
              <InputText v-model="editData.jsonPaths[key]" class="flex-3" />
            </div>
          </AccordionTab>
          <AccordionTab v-if="editData.genreMappings" :header="t('admin.scrapers.genre_mappings')">
            <div v-for="(val, key) in editData.genreMappings" :key="key" class="flex gap-2 mb-2">
              <InputText :modelValue="key" class="flex-1" disabled />
              <Chip v-for="g in val" :key="g" :label="g" class="mr-1" />
            </div>
          </AccordionTab>
          <AccordionTab v-if="editData.headers" :header="t('admin.scrapers.headers')">
            <div v-for="(val, key) in editData.headers" :key="key" class="flex gap-2 mb-2">
              <InputText :modelValue="key" class="flex-1" disabled />
              <InputText v-model="editData.headers[key]" class="flex-3" />
            </div>
          </AccordionTab>
        </Accordion>

        <Button :label="t('common.save')" icon="pi pi-check" class="w-full mt-3" @click="handleSave" :loading="saving" />
      </div>
    </Sidebar>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useI18n } from '../composables/useI18n'
import { adminApi } from '../api/admin'
import { useToast } from 'primevue/usetoast'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import ToggleSwitch from 'primevue/toggleswitch'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Button from 'primevue/button'
import Sidebar from 'primevue/sidebar'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import Chip from 'primevue/chip'

const { t } = useI18n()
const toast = useToast()

const scrapers = ref([])
const loading = ref(false)
const reloading = ref(false)
const saving = ref(false)

const editVisible = ref(false)
const editData = ref(null)
const originalType = ref('')
const apiKeyVisible = ref(false)

const editTitle = computed(() => {
  return editData.value ? `${t('admin.scrapers.edit')}: ${editData.value.type}` : ''
})

onMounted(loadConfigs)

async function loadConfigs() {
  loading.value = true
  try {
    const res = await adminApi.getScraperConfigs()
    scrapers.value = res.data.data || []
  } catch {
    toast.add({ severity: 'error', summary: t('admin.scrapers.load_failed'), life: 3000 })
  } finally {
    loading.value = false
  }
}

async function toggleEnabled(item, newVal) {
  item.enabled = newVal
  try {
    await adminApi.updateScraperConfig(item.type, item)
    toast.add({ severity: 'success', summary: t('admin.scrapers.save_success'), life: 2000 })
  } catch {
    toast.add({ severity: 'error', summary: t('admin.scrapers.save_failed'), life: 3000 })
    await loadConfigs()
  }
}

function openEdit(item) {
  originalType.value = item.type
  editData.value = JSON.parse(JSON.stringify(item))
  if (!editData.value.cssSelectors) editData.value.cssSelectors = {}
  if (!editData.value.jsonPaths) editData.value.jsonPaths = {}
  if (!editData.value.genreMappings) editData.value.genreMappings = {}
  if (!editData.value.headers) editData.value.headers = {}
  apiKeyVisible.value = false
  editVisible.value = true
}

async function handleSave() {
  if (!editData.value) return
  saving.value = true
  try {
    await adminApi.updateScraperConfig(originalType.value, editData.value)
    toast.add({ severity: 'success', summary: t('admin.scrapers.save_success'), life: 3000 })
    editVisible.value = false
    await loadConfigs()
  } catch {
    toast.add({ severity: 'error', summary: t('admin.scrapers.save_failed'), life: 3000 })
  } finally {
    saving.value = false
  }
}

async function handleReload() {
  reloading.value = true
  try {
    await adminApi.reloadScraperConfig()
    toast.add({ severity: 'success', summary: t('admin.scrapers.reload_success'), life: 3000 })
    await loadConfigs()
  } catch {
    toast.add({ severity: 'error', summary: t('admin.scrapers.reload_failed'), life: 3000 })
  } finally {
    reloading.value = false
  }
}
</script>

<style scoped>
.admin-scrapers-container {
  padding: 1.5rem;
}
.field {
  margin-bottom: 0.5rem;
}
.field label {
  display: block;
  margin-bottom: 0.25rem;
  font-weight: 600;
  font-size: 0.875rem;
}
.flex-1 { flex: 1; }
.flex-3 { flex: 3; }
</style>
