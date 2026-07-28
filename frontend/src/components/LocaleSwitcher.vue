<!-- Переключатель языка интерфейса (RU/EN) через SelectButton. Сохраняет выбор через LocaleStore. При смене языка происходит window.location.reload() для перезапуска приложения. -->
<template>
  <SelectButton
    v-model="lang"
    :options="langOptions"
    optionLabel="label"
    optionValue="value"
    class="lang-switcher"
  />
</template>

<script setup>
// Переключатель языка: RU/EN, сохраняет в LocaleStore, перезагружает страницу при смене
import { ref, watch } from 'vue'
import { useLocaleStore } from '../stores/locale'
import SelectButton from 'primevue/selectbutton'

const localeStore = useLocaleStore()

const langOptions = [
  { label: 'RU', value: 'ru' },
  { label: 'EN', value: 'en' }
]

const lang = ref(localeStore.locale)
watch(lang, (val) => {
  localeStore.setLocale(val)
})
</script>

<style scoped>
.lang-switcher {
  height: 2rem;
}
</style>
