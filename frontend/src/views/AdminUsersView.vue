<template>
  <div class="admin-users-container">
    <h2>{{ t('admin.users.title') }}</h2>

    <DataTable :value="users" stripedRows paginator :rows="10" sortField="name" :sortOrder="1">
      <Column field="id" :header="t('admin.users.id')" sortable style="width:80px" />
      <Column field="name" :header="t('login.username')" sortable />
      <Column field="avatarUrl" :header="t('admin.users.avatar')" style="width:80px">
        <template #body="slotProps">
          <Avatar :image="slotProps.data.avatarUrl" shape="circle" size="small" />
        </template>
      </Column>
      <Column field="admin" :header="t('admin.users.admin')" sortable style="width:100px">
        <template #body="slotProps">
          <ToggleSwitch
            :modelValue="slotProps.data.admin"
            @update:modelValue="(val) => toggleAdmin(slotProps.data.id, val)"
          />
        </template>
      </Column>
      <Column field="active" :header="t('admin.users.active')" sortable style="width:100px">
        <template #body="slotProps">
          <ToggleSwitch
            :modelValue="slotProps.data.active"
            @update:modelValue="(val) => toggleActive(slotProps.data.id, val)"
          />
        </template>
      </Column>
      <Column :header="t('admin.users.actions')" style="width:150px">
        <template #body="slotProps">
          <Button
            icon="pi pi-refresh"
            severity="warn"
            text
            v-tooltip.left="t('admin.users.reset_password')"
            @click="resetPassword(slotProps.data.id)"
          />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../api/admin'
import { useI18n } from '../composables/useI18n'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Avatar from 'primevue/avatar'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()

const users = ref([])
const toast = useToast()

onMounted(async () => {
  try {
    const res = await adminApi.getUsers()
    users.value = res.data.data || []
  } catch {
    toast.add({ severity: 'error', summary: t('admin.users.load_failed'), life: 3000 })
  }
})

async function toggleAdmin(id, value) {
  try {
    await adminApi.toggleAdmin(id, value)
    toast.add({ severity: 'success', summary: t('admin.users.update_success'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: t('admin.users.update_failed'), life: 3000 })
  }
}

async function toggleActive(id, value) {
  try {
    await adminApi.toggleActive(id, value)
    toast.add({ severity: 'success', summary: t('admin.users.update_success'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: t('admin.users.update_failed'), life: 3000 })
  }
}

async function resetPassword(id) {
  try {
    await adminApi.resetPassword(id)
    toast.add({ severity: 'success', summary: t('admin.users.password_reset_success'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: t('admin.users.password_reset_failed'), life: 3000 })
  }
}
</script>

<style scoped>
.admin-users-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 1rem;
}
</style>
