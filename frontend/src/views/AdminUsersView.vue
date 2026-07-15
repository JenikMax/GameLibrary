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
            @click="resetPassword(slotProps.data.id, slotProps.data.name)"
          />
        </template>
      </Column>
    </DataTable>

    <Dialog v-model:visible="resetDialogVisible" :header="t('admin.users.password_reset_dialog_title')" :modal="true" :closable="true">
      <p>{{ t('admin.users.password_reset_dialog_message', { user: resetDialogUser }) }}</p>
      <div class="password-display">
        <InputText :modelValue="resetDialogPassword" readonly fluid />
        <Button icon="pi pi-copy" severity="info" @click="copyPassword" />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" @click="resetDialogVisible = false" />
      </template>
    </Dialog>
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
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()

const users = ref([])
const toast = useToast()

const resetDialogVisible = ref(false)
const resetDialogUser = ref('')
const resetDialogPassword = ref('')

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

async function resetPassword(id, userName) {
  try {
    const res = await adminApi.resetPassword(id)
    resetDialogUser.value = userName
    resetDialogPassword.value = res.data.data
    resetDialogVisible.value = true
  } catch {
    toast.add({ severity: 'error', summary: t('admin.users.password_reset_failed'), life: 3000 })
  }
}

function copyPassword() {
  const ta = document.createElement('textarea')
  ta.value = resetDialogPassword.value
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.select()
  document.execCommand('copy')
  document.body.removeChild(ta)
  toast.add({ severity: 'info', summary: t('admin.users.password_copied'), life: 2000 })
}
</script>

<style scoped>
.admin-users-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 1rem;
}
.password-display {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}
</style>
