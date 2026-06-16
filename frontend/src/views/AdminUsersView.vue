<template>
  <div class="admin-users-container">
    <h2>User Management</h2>

    <DataTable :value="users" stripedRows paginator :rows="10" sortField="name" :sortOrder="1">
      <Column field="id" header="ID" sortable style="width:80px" />
      <Column field="name" header="Username" sortable />
      <Column field="avatarUrl" header="Avatar" style="width:80px">
        <template #body="slotProps">
          <Avatar :image="slotProps.data.avatarUrl" shape="circle" size="small" />
        </template>
      </Column>
      <Column field="admin" header="Admin" sortable style="width:100px">
        <template #body="slotProps">
          <ToggleSwitch
            :modelValue="slotProps.data.admin"
            @update:modelValue="(val) => toggleAdmin(slotProps.data.id, val)"
          />
        </template>
      </Column>
      <Column field="active" header="Active" sortable style="width:100px">
        <template #body="slotProps">
          <ToggleSwitch
            :modelValue="slotProps.data.active"
            @update:modelValue="(val) => toggleActive(slotProps.data.id, val)"
          />
        </template>
      </Column>
      <Column header="Actions" style="width:150px">
        <template #body="slotProps">
          <Button
            icon="pi pi-refresh"
            severity="warn"
            text
            v-tooltip.left="'Reset password'"
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
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Avatar from 'primevue/avatar'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import { useToast } from 'primevue/usetoast'

const users = ref([])
const toast = useToast()

onMounted(async () => {
  try {
    const res = await adminApi.getUsers()
    users.value = res.data.data || []
  } catch {
    toast.add({ severity: 'error', summary: 'Failed to load users', life: 3000 })
  }
})

async function toggleAdmin(id, value) {
  try {
    await adminApi.toggleAdmin(id, value)
    toast.add({ severity: 'success', summary: 'User updated', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Update failed', life: 3000 })
  }
}

async function toggleActive(id, value) {
  try {
    await adminApi.toggleActive(id, value)
    toast.add({ severity: 'success', summary: 'User updated', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Update failed', life: 3000 })
  }
}

async function resetPassword(id) {
  try {
    await adminApi.resetPassword(id)
    toast.add({ severity: 'success', summary: 'Password reset', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Reset failed', life: 3000 })
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
