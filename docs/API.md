# GameLibrary API Reference

Префикс: `/game-library/api/`. Аутентификация: JWT Bearer token (заголовок `Authorization: Bearer <token>`).

Документация Swagger UI доступна по адресу `/game-library/swagger-ui.html` (группы: `public` и `admin`).

---

## Auth

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `POST` | `/auth/login` | все | Вход → JWT access + refresh tokens, профиль |
| `POST` | `/auth/register` | все | Регистрация нового пользователя |
| `GET` | `/auth/me` | USER, ADMIN | Информация о текущем пользователе |

---

## Games

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/games` | USER, ADMIN | Список игр (фильтры, сортировка, пагинация, `favoritesOnly`) |
| `GET` | `/games/filter-options` | USER, ADMIN | Годы, платформы, жанры для UI фильтров |
| `GET` | `/games/scrapers` | USER, ADMIN | Список включённых скраперов |
| `GET` | `/games/{id}` | USER, ADMIN | Детали игры |
| `POST` | `/games/{id}/edit` | ADMIN | Сохранить изменения игры |
| `POST` | `/games/{id}/grab` | ADMIN | Собрать метаданные выбранным скрапером |
| `GET` | `/games/{id}/download` | USER, ADMIN | Скачать ZIP (<5 ГБ) или .torrent (≥5 ГБ) |
| `GET` | `/games/{id}/download-info` | USER, ADMIN | Информация о скачивании (размер, статус кэша) |
| `POST` | `/games/{id}/seed` | USER, ADMIN | Начать раздачу через Transmission |
| `POST` | `/games/{id}/prepare-download` | USER, ADMIN | Асинхронная подготовка .torrent (для ≥5 ГБ) |

### Rating & Favorites

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/games/{id}/rating` | USER, ADMIN | Рейтинг игры |
| `POST` | `/games/{id}/rating` | USER, ADMIN | Оценить игру (1-10) |
| `POST` | `/games/{id}/favorite` | USER, ADMIN | Добавить/удалить из избранного |
| `GET` | `/games/{id}/favorite` | USER, ADMIN | Проверить, в избранном ли |
| `GET` | `/games/favorites` | USER, ADMIN | Список ID избранных игр |
| `GET` | `/games/random` | USER, ADMIN | Случайная игра |

### Comments

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/games/{id}/comments` | USER, ADMIN | Комментарии к игре |
| `POST` | `/games/{id}/comments` | USER, ADMIN | Добавить комментарий |
| `DELETE` | `/games/{id}/comments/{commentId}` | USER, ADMIN | Удалить свой комментарий |

### Related & Reviews

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/games/{id}/related` | USER, ADMIN | Связанные игры (жанр или похожее название) |
| `GET` | `/games/{id}/reviews` | USER, ADMIN | Рецензии с агрегированными оценками |
| `POST` | `/games/{id}/reviews` | USER, ADMIN | Добавить/обновить рецензию (4 категории 1-10, плюсы/минусы) |
| `DELETE` | `/games/{id}/reviews/{reviewId}` | USER, ADMIN | Удалить свою рецензию |

### AI Features

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `POST` | `/games/{id}/suggest-tags` | USER, ADMIN | Авто-теги: предложить теги и жанры из описания |
| `POST` | `/games/{id}/translate` | USER, ADMIN | Перевести описание игры ru↔en (кешируется) |
| `POST` | `/games/translate-text` | USER, ADMIN | Перевести произвольный текст ru↔en |
| `POST` | `/games/auto-tag-preview` | USER, ADMIN | Предпросмотр авто-тегов для произвольного текста |

---

## AI — Embeddings

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `POST` | `/embeddings/generate` | ADMIN | Асинхронная генерация embedding'ов → `202 { taskId }` |
| `GET` | `/embeddings/status/{taskId}` | USER, ADMIN | Статус задачи генерации embedding'ов |

---

## Collections

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/collections` | USER, ADMIN | Свои + публичные коллекции |
| `GET` | `/collections/with-hero` | USER, ADMIN | Коллекции с hero/preview данными для карточек |
| `GET` | `/collections/{id}` | USER, ADMIN | Получить коллекцию |
| `POST` | `/collections` | USER, ADMIN | Создать коллекцию (`name`, `description?`, `isPublic?`, `isSmart?`, `smartRules?`) |
| `PUT` | `/collections/{id}` | USER, ADMIN | Обновить коллекцию |
| `DELETE` | `/collections/{id}` | USER, ADMIN | Удалить коллекцию |
| `GET` | `/collections/{id}/games` | USER, ADMIN | Игры в коллекции |
| `POST` | `/collections/{id}/games` | USER, ADMIN | Добавить игру |
| `DELETE` | `/collections/{id}/games/{gameId}` | USER, ADMIN | Удалить игру |
| `PUT` | `/collections/{id}/games/reorder` | USER, ADMIN | Изменить порядок игр (полный список `gameId`) |

**Smart collections** — правила (`platforms`, `genres`, `yearFrom`, `yearTo`, `minRating`, `tags`, `nameContains`) оцениваются на сервере, игры подбираются автоматически.

---

## Statistics

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/statistics` | USER, ADMIN | Метрики библиотеки (количество, диаграммы, топы) |
| `POST` | `/statistics/refresh-sizes` | ADMIN | Сбросить кэш размеров — пересчёт при следующем GET |

---

## Downloads (Transmission)

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/download/prepare-status/{taskId}` | USER, ADMIN | Статус подготовки торрента |
| `GET` | `/seed/status/{taskId}` | USER, ADMIN | Статус задачи раздачи |
| `GET` | `/downloads/active` | USER, ADMIN | Активные торренты |
| `GET` | `/downloads/waiting` | USER, ADMIN | Ожидающие торренты |
| `GET` | `/downloads/stopped` | USER, ADMIN | Остановленные торренты |
| `GET` | `/downloads/{gid}/status` | USER, ADMIN | Статус одного торрента |
| `POST` | `/downloads/{gid}/remove` | USER, ADMIN | Удалить торрент (файлы остаются) |
| `POST` | `/downloads/{gid}/pause` | USER, ADMIN | Пауза |
| `POST` | `/downloads/{gid}/unpause` | USER, ADMIN | Возобновить |
| `GET` | `/downloads/global-stat` | USER, ADMIN | Статистика сессии Transmission |
| `GET` | `/downloads/aria2-version` | USER, ADMIN | Проверка связи с Transmission |

---

## Notifications

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/notifications` | USER, ADMIN | Последние 20 уведомлений + счётчик непрочитанных |
| `PUT` | `/notifications/{id}/read` | USER, ADMIN | Отметить прочитанным |
| `PUT` | `/notifications/read-all` | USER, ADMIN | Отметить все прочитанными |

---

## Images

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/images/games/{gameId}/logo` | все | Логотип игры (ФС → БД fallback) |
| `GET` | `/images/games/{gameId}/screenshots/{screenshotId}` | все | Скриншот (ФС → БД fallback) |
| `GET` | `/images/avatars/{userId}` | все | Аватар пользователя (ФС → БД fallback) |

Поддержка ETag + `Cache-Control: public, max-age=86400` (24ч), `If-None-Match` → `304 Not Modified`.

---

## Profile

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/profile` | USER, ADMIN | Данные профиля + статистика |
| `PUT` | `/profile` | USER, ADMIN | Обновить профиль (аватар, имя) |
| `POST` | `/profile/pass` | USER, ADMIN | Сменить пароль |

---

## Admin — Scan

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `POST` | `/scan` | ADMIN | Запуск асинхронного сканирования ФС → `202 { taskId }` |
| `GET` | `/scan/status/{taskId}` | USER, ADMIN | Прогресс сканирования (`status`, `progress`, `currentGame`, `phase`) |

Фазы: `SCANNING_DIRS` → `STORING_METADATA` → `LOADING_IMAGES` → `REFRESHING_SIZES` → `COMPLETED`.  
Фронтенд polling'ит каждые 500ms.

---

## Admin — Users

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/admin/users` | ADMIN | Список всех пользователей |
| `POST` | `/admin/users/{id}/toggle-admin` | ADMIN | Переключить роль admin/user |
| `POST` | `/admin/users/{id}/toggle-active` | ADMIN | Блокировать / разблокировать |
| `POST` | `/admin/users/{id}/reset-pass` | ADMIN | Сбросить пароль (авто-генерация или `RESET_PASSWORD_DEFAULT`) |

---

## Admin — Scrapers

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/admin/scraper-config` | ADMIN | Список конфигов всех скраперов |
| `GET` | `/admin/scraper-config/{type}` | ADMIN | Конфиг одного скрапера |
| `PUT` | `/admin/scraper-config/{type}` | ADMIN | Обновить конфиг скрапера |
| `POST` | `/admin/scraper-config/reload` | ADMIN | Перезагрузить конфиг из файла |

---

## Tracker

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| `GET` | `/tracker/announce` | все | BitTorrent HTTP tracker announce |
| `GET` | `/tracker/scrape` | все | BitTorrent HTTP tracker scrape |

---

## Rate Limiting

- Login: 5 запросов/мин на IP+User-Agent
- API: 100 запросов/мин на IP
- При превышении: HTTP 429 Too Many Requests

---

## Auth Model

- JWT access token (15 мин) + refresh token (7 дн)
- Refresh — silently через Axios interceptor (`frontend/src/api/axios.js`)
- In-memory blacklist для отозванных токенов
- BCrypt для паролей
- Роли: `ROLE_ADMIN`, `ROLE_USER`
