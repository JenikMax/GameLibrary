# План исправлений: 3 проблемы

## 1. Пагинация (быстрый фикс)

**Проблема:** В `LibraryView.vue` в шаблоне используется `store.pageSize.value`, но Pinia автоматически разматывает refs в шаблонах — `.value` даёт `undefined`, пагинатор не рендерится.

**Файл:** `frontend/src/views/LibraryView.vue`

**Изменения:** Заменить все `store.pageSize.value` → `store.pageSize` в `<template>`:
- Строка 111: `v-for="i in store.pageSize.value"` → `v-for="i in store.pageSize"`
- Строка 115: `v-if="store.totalItems > store.pageSize.value"` → `v-if="store.totalItems > store.pageSize"`
- Строка 117: `:first="(store.currentPage - 1) * store.pageSize.value"` → `:first="(store.currentPage - 1) * store.pageSize"`
- Строка 118: `:rows="store.pageSize.value"` → `:rows="store.pageSize"`
- Строка 129: `v-if="store.totalItems > store.pageSize.value"` → `v-if="store.totalItems > store.pageSize"`
- Строка 131: `:first="(store.currentPage - 1) * store.pageSize.value"` → `:first="(store.currentPage - 1) * store.pageSize"`
- Строка 132: `:rows="store.pageSize.value"` → `:rows="store.pageSize"`

---

## 2. Жанры не обновляются при смене языка

**Проблема:** `fetchFilterOptions()` вызывается один раз в `onMounted`. При смене locale `filterOptions` не перезагружается → `genreMap` остаётся на старом языке.

### 2a. Library store — watch на locale

**Файл:** `frontend/src/stores/library.js`

Добавить импорт `watch` и `useLocaleStore`, добавить watcher:
```js
import { useLocaleStore } from './locale'

// внутри setup():
const localeStore = useLocaleStore()
watch(() => localeStore.locale, () => {
  fetchFilterOptions()
})
```

Это автоматически обновит `genreMap` (computed), и жанры перерисуются во всех компонентах, использующих `libraryStore.genreMap` (GameCard, GameListRow, GameDetailView).

### 2b. GameFilter — авто-обновление genres в MultiSelect

**Файл:** `frontend/src/components/GameFilter.vue`

GameFilter получает `options` через props. Когда `filterOptions` обновляется (после refetch), props обновляются автоматически → MultiSelect с жанрами тоже обновится. Проверить, что `selectedGenres` хранит `code`, а не `name` (уже так).

### 2c. GameEditView — watch на locale для allGenres/allTags

**Файл:** `frontend/src/views/GameEditView.vue`

`allGenres` и `allTags` загружаются в `onMounted` через `gamesApi.getFilterOptions()`. Добавить watch:
```js
import { useLocaleStore } from '../stores/locale'

const localeStore = useLocaleStore()
watch(() => localeStore.locale, async () => {
  try {
    const res = await gamesApi.getFilterOptions()
    allGenres.value = res.data.data.genres || []
    allTags.value = res.data.data.tags || []
  } catch { /* ignore */ }
})
```

---

## 3. Умные коллекции — UI builder

**Проблема:** Пользователь вынужден править JSON руками. Нужно заменить textarea на form builder с dropdown'ами.

### 3a. Создать компонент `SmartRulesForm.vue`

**Файл:** `frontend/src/components/SmartRulesForm.vue` (новый)

Компонент принимает:
- `modelValue` — объект правил (reactive) или null
- `options` — filterOptions (platforms, genres, tags, years)

Эмитит `update:modelValue` при изменениях.

Поля:
| Поле | Компонент | Источник данных |
|------|-----------|-----------------|
| Платформы | MultiSelect | `options.platforms` (Chip list) |
| Жанры | MultiSelect | `options.genres` (code + translated name) |
| Год от / до | InputNumber × 2 | свободный ввод |
| Мин. рейтинг | InputNumber (1-10) | свободный ввод |
| Теги | MultiSelect | `options.tags` |
| Текст в названии | InputText | свободный ввод |

JSON-формат (совместимый с текущим):
```json
{
  "platforms": ["PC", "PS5"],
  "genres": ["rpg", "action"],
  "yearFrom": 2000,
  "yearTo": 2020,
  "minRating": 7,
  "tags": ["multiplayer"],
  "nameContains": "dragon"
}
```

Компонент также включает функцию `parseRules(jsonString)` → объект и `serializeRules(rulesObj)` → JSON string для обратной совместимости с БД.

### 3b. Коллекции — i18n ключи

**Файл:** `frontend/src/composables/useI18n.js`

Добавить новые ключи (RU и EN):
```
'collections.smart_rules_platforms': 'Платформы' / 'Platforms'
'collections.smart_rules_genres': 'Жанры' / 'Genres'
'collections.smart_rules_year_from': 'Год от' / 'Year from'
'collections.smart_rules_year_to': 'Год до' / 'Year to'
'collections.smart_rules_min_rating': 'Мин. рейтинг' / 'Min rating'
'collections.smart_rules_tags': 'Теги' / 'Tags'
'collections.smart_rules_name': 'Текст в названии' / 'Name contains'
'collections.smart_rules_empty': 'Правила не заданы' / 'No rules specified'
```

Обновить существующий ключ:
- `collections.smart_rules`: `'Правила (JSON)'` → `'Правила'` / `'Rules (JSON)'` → `'Rules'`
- Удалить `collections.smart_rules_hint` (не нужен)

### 3c. CollectionsView.vue — интеграция SmartRulesForm

**Файл:** `frontend/src/views/CollectionsView.vue`

Изменения:
- Импортировать `SmartRulesForm`, `useLibraryStore`
- Заменить `newSmartRules` (string) на `newSmartRulesObj` (reactive object)
- В dialog: заменить `<Textarea>` на `<SmartRulesForm>`
- `newIsSmart` toggle → инициализировать `newSmartRulesObj = {}`
- При сохранении: `JSON.stringify(newSmartRulesObj)` → `payload.smartRules`
- Передать `store.filterOptions` как prop в SmartRulesForm
- Вызвать `store.fetchFilterOptions()` если не загружены

### 3d. CollectionDetailView.vue — интеграция SmartRulesForm

**Файл:** `frontend/src/views/CollectionDetailView.vue`

Изменения:
- Импортировать `SmartRulesForm`, `useLibraryStore`
- В отображении: заменить `<code>{{ collection.smartRules }}</code>` на human-readable список правил
- В edit dialog: заменить `<Textarea>` на `<SmartRulesForm>`
- Загрузить filterOptions из library store для SmartRulesForm
- При загрузке: `JSON.parse(collection.smartRules)` → объект
- При сохранении: `JSON.stringify(editSmartRulesObj)` → payload

### 3e. SmartRulesForm — human-readable отображение

Для отображения правил в CollectionDetailView (без edit mode) — использовать SmartRulesForm в readonly-режиме или создать вспомогательную функцию `formatRules(rules, genreMap)` → массив строк:
```
Платформы: PC, PS5
Жанры: RPG, Action
Год: 2000—2020
Мин. рейтинг: 7
```

---

## Файлы для изменения (сводка)

| # | Файл | Тип |
|---|------|-----|
| 1 | `frontend/src/views/LibraryView.vue` | edit (7 замен `.value`) |
| 2 | `frontend/src/stores/library.js` | edit (добавить watch) |
| 3 | `frontend/src/views/GameEditView.vue` | edit (добавить watch) |
| 4 | `frontend/src/composables/useI18n.js` | edit (7 новых ключей × 2 языка) |
| 5 | `frontend/src/components/SmartRulesForm.vue` | **create** |
| 6 | `frontend/src/views/CollectionsView.vue` | edit (интеграция SmartRulesForm) |
| 7 | `frontend/src/views/CollectionDetailView.vue` | edit (интеграция SmartRulesForm + human-readable) |

---

## Порядок выполнения

1. **Пагинация** — LibraryView.vue (7 замен)
2. **Locale + жанры** — library.js (watch), GameEditView.vue (watch)
3. **i18n ключи** — useI18n.js
4. **SmartRulesForm.vue** — новый компонент
5. **CollectionsView.vue** — интеграция
6. **CollectionDetailView.vue** — интеграция + human-readable display
