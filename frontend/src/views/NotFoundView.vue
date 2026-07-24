<template>
  <div class="not-found">
    <i class="pi pi-exclamation-triangle not-found-icon"></i>
    <h1 class="not-found-code">
      <span v-if="currentThemeId === 'retro-terminal'">&gt; 404_Not_Found<span class="t-cursor" /></span>
      <span v-else>404</span>
    </h1>
    <p class="not-found-message">{{ t('not_found.message') }}</p>
    <router-link to="/">
      <Button :label="t('not_found.back')" icon="pi pi-arrow-left" />
    </router-link>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import { useI18n } from '../composables/useI18n'
import { useTheme } from '../composables/useTheme'
const { t } = useI18n()
const { currentThemeId } = useTheme()
</script>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 1rem;
  text-align: center;
  padding: 2rem;
}
.not-found-icon {
  font-size: 4rem;
  color: var(--p-yellow-500);
}
.not-found-code {
  font-size: 5rem;
  font-weight: 800;
  margin: 0;
  line-height: 1;
  color: var(--p-text-muted-color);
}
.not-found-message {
  font-size: 1.1rem;
  color: var(--p-text-secondary-color);
  margin: 0;
}
</style>
