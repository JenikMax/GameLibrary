<!-- Управление пользователями (ADMIN). Таблица с полями ID, имя, аватар, статус админа, активность. Возможность переключить роль, деактивировать, сбросить пароль (генерируется случайно и показывается в диалоге). -->
<template>
  <div class="admin-users-container">
    <h2>
      <span v-if="isTerminalTheme">&gt; {{ t('admin.users.title').replace(/ /g, '_') }}<span class="t-cursor" /></span>
      <span v-else>{{ t('admin.users.title') }}</span>
    </h2>

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
          <span v-if="isTerminalTheme">
            <Button
              :label="slotProps.data.admin ? 'ADMIN' : 'USER'"
              :class="slotProps.data.admin ? 'rt-admin-btn rt-admin' : 'rt-admin-btn rt-user'"
              size="small"
              @click="toggleAdmin(slotProps.data.id, !slotProps.data.admin)"
            />
          </span>
          <ToggleSwitch v-else
            :modelValue="slotProps.data.admin"
            @update:modelValue="(val) => toggleAdmin(slotProps.data.id, val)"
          />
        </template>
      </Column>
      <Column field="active" :header="t('admin.users.active')" sortable style="width:100px">
        <template #body="slotProps">
          <span v-if="isTerminalTheme">
            <Button
              :label="slotProps.data.active ? 'ACTIVE' : 'INACTIVE'"
              :class="slotProps.data.active ? 'rt-admin-btn rt-active' : 'rt-admin-btn rt-inactive'"
              size="small"
              @click="toggleActive(slotProps.data.id, !slotProps.data.active)"
            />
          </span>
          <ToggleSwitch v-else
            :modelValue="slotProps.data.active"
            @update:modelValue="(val) => toggleActive(slotProps.data.id, val)"
          />
        </template>
      </Column>
      <Column :header="t('admin.users.actions')" style="width:150px">
        <template #body="slotProps">
          <span v-if="isTerminalTheme">
            <Button
              label="RESET"
              class="rt-admin-btn rt-reset"
              size="small"
              @click="resetPassword(slotProps.data.id, slotProps.data.name)"
            />
          </span>
          <Button v-else
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
// Управление пользователями: загрузка списка, переключение admin/active, сброс пароля с копированием в буфер обмена
import { ref, onMounted } from 'vue'
import { adminApi } from '../api/admin'
import { useI18n } from '../composables/useI18n'
import { useTheme } from '../composables/useTheme'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Avatar from 'primevue/avatar'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const { isTerminalTheme } = useTheme()

const users = ref([])
const toast = useToast()

// Состояние диалога сброса пароля
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

// Переключение роли admin/user
async function toggleAdmin(id, value) {
  try {
    await adminApi.toggleAdmin(id, value)
    const user = users.value.find(u => u.id === id)
    if (user) user.admin = value
    toast.add({ severity: 'success', summary: t('admin.users.update_success'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: t('admin.users.update_failed'), life: 3000 })
  }
}

// Переключение активности пользователя
async function toggleActive(id, value) {
  try {
    await adminApi.toggleActive(id, value)
    const user = users.value.find(u => u.id === id)
    if (user) user.active = value
    toast.add({ severity: 'success', summary: t('admin.users.update_success'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: t('admin.users.update_failed'), life: 3000 })
  }
}

// Сброс пароля: генерируется случайный, показывается в диалоге
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

// Копирование сгенерированного пароля в буфер обмена
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
