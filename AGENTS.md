# GameLibrary — Agent Instructions

## Build & Run

```bash
mvn clean package -DskipTests              # builds target/game-library.jar
cd frontend && npm install && npm run build  # builds dist/ with Vue SPA
make all                                     # or docker-compose up --build -d
mvn spring-boot:run                          # run backend locally (needs PostgreSQL)
cd frontend && npm run dev                   # run frontend dev server (:5173, proxies to :8080)
```

No Maven wrapper — `mvn` must be on PATH. Java 25 target, `eclipse-temurin:25-jre-alpine` runtime. No tests, no lint/format/typecheck config.

## Docker

`docker-compose.yml` starts four services: `frontend` (Nginx + Vue SPA, `:80`), `backend` (Spring Boot REST, `:8080`), `transmission` (torrent seeder, `:9091` RPC, `:51413`), `postgresdb` (`:5432`).  
DB lifecycle scripts in `postgresdb/`. Schema in `ddl/*.sql`, copied into `/docker-entrypoint-initdb.d/`.

## Architecture

- Spring Boot 4.0.7 → `jar` packaging, embedded Tomcat (max 10 threads).
- Context path `/game-library` (`application.yml:server.servlet.context-path`).
- Frontend: separate Vue 3 + Vite 5 + PrimeVue 4 project in `frontend/`, served via Nginx.
- API: JSON REST (`/game-library/api/**`) with JWT auth; Swagger UI at `/game-library/swagger-ui.html`.
- Legacy Thymeleaf views still work alongside REST (dual auth: form login + JWT).
- PostgreSQL 16 schema `library`, JPA (Hibernate managed by Boot 4.x) + HikariCP pool.
- Auth: Spring Security, form login + JWT, BCrypt, `ROLE_ADMIN`/`ROLE_USER`.
- Torrent: embedded HTTP tracker (`/api/tracker/announce`) + Transmission 4.1.2 RPC for seeding.
- Images: DB bytea with optional filesystem override at `images.directory/games/{id}/logo.jpg` (or `screenshots/`, `avatars/`).
- Scrapers (7 active): Playground (CSS selectors + search API), Igromania (JSON paths), WorldArt (CSS selectors), Steam (Storefront API, no key required), IGDB (Twitch OAuth), TheGamesDB (API key), PsxDataCenter (JSoup, PS1/PS2, no key).
- State: Pinia stores for auth, library, locale.
- Rich text: VueQuill + Quill 2 for game description editing.
- **Phase 3 features**: rating 1-10 (`GameRating.java` + `RatingController`), favorites (`FavoriteController` + heart toggle, filter via `?favorites=1` URL param + store), comments (`CommentController` with ownership check), notifications (`NotificationService` + bell icon with 15s polling + click-outside close), view history (composable `useViewHistory.js`, localStorage, max 20 items, error handling for quota exceeded), related games (`RelatedGamesController`, 2 SQL queries: same genre, similar name with first-word fallback), statistics (`StatisticsController` + `StatisticsView.vue`, Chart.js dashboard with platform/genre/year charts + top lists), collections (`CollectionController` + `CollectionsView.vue`, user-created game playlists, public/private, reorder).
- Package root: `com.jenikmax.game.library`.

## IGDB Scraper Setup

IGDB требует OAuth 2.0 через Twitch. Чтобы получить **Client-ID** и **Access Token**:

1. Зайди на https://dev.twitch.tv/console/apps/create
2. Создай приложение (Name — любое, OAuth Redirect URLs — `http://localhost`, Category — любой)
3. После создания скопируй **Client-ID** (показывается в списке приложений)
4. Там же нажми **New Secret** → скопируй **Client Secret**
5. Обменяй на access token:
   ```bash
   curl -X POST "https://id.twitch.tv/oauth2/token?client_id=ВАШ_CLIENT_ID&client_secret=ВАШ_SECRET&grant_type=client_credentials"
   ```
6. Из JSON-ответа скопируй значение поля `access_token`.

Куда прописать:
- **Client-ID** → в админке (`GET /api/admin/scraper-config/igdb`) → поле `headers.Client-ID`
- **access_token** → в админке → поле `encryptedApiKey` (админка зашифрует сама)
- Либо отредактировать файл конфигурации скрапера (`${SCRAPER_CONFIG_DIR}/scrapers-config.json`, по умолчанию `/gameLibrary/gameLibraryConfigs/scrapers/scrapers-config.json`) напрямую — но тогда токен будет в plaintext, т.к. шифрование происходит только при записи через API.

## TheGamesDB Scraper Setup

TheGamesDB требует API-ключ. Чтобы получить его:

1. Зарегистрируйся на https://thegamesdb.net/register.php
2. Подтверди email
3. Войди на сайт
4. Перейди на https://api.thegamesdb.net/key.php — ключ будет отображён
5. Скопируй ключ

Куда прописать:
- В админке (`GET /api/admin/scraper-config/thegamesdb`) → поле `encryptedApiKey`
- Либо отредактировать файл конфигурации скрапера (`${SCRAPER_CONFIG_DIR}/scrapers-config.json`, по умолчанию `/gameLibrary/gameLibraryConfigs/scrapers/scrapers-config.json`) напрямую (будет в plaintext)

Лимит: 1000 запросов в месяц. Один поиск игры может использовать 2-3 запроса (поиск + жанры + скриншоты).

## PsxDataCenter Scraper

PsxDataCenter (psxdatacenter.com) — скрапер для PS1 и PS2 без ключа. Использует JSoup для парсинга фреймовой структуры сайта.

- **Поиск по названию**: скрапит 6 list-страниц (3 региона × 2 платформы PS1/PS2), первое совпадение.
- **Две версии разметки** карточки игры:
  - **Version A** (старая): метки в `<b>` — `Game Name:`, `Genre:`, `Release Date:`
  - **Version B** (н овая): текст метки напрямую в `<td>` — `Common Title`, `Genre / Style`, `Date Released`
  - Какая версия отдаётся, зависит от User-Agent / Referer. `jsoupHelper` (OkHttp + Mozilla UA) → Version B.
- **PS2**: метки **ВЕРХНИМ РЕГИСТРОМ** (`GENRE / STYLE`, `COMMON TITLE`, `DATE RELEASED`) — сравнение через `equalsIgnoreCase`.
- **Кодировка**: windows-1252.
- **Описание**: `table#table16 td[style*="#333333"]`, fallback на самый длинный `<td>`.
- **Instruction**: `table#table25 td.bluecell:matchesOwn((?i)emulator)`.
- **PSP исключён**: на PSP list-страницах все INFO-кнопки пустые.
- **Genre mappings** (~40 записей) в `ScraperConfigService.buildPsxDataCenterGenreMappings()`.
- **Screenshots**: URL извлекается из `td[onclick*="Select("]` через regex.
- **NBSP-очистка**: `cleanText()` заменяет `\u00A0` и типографские кавычки.

## Gotchas

- **Plaintext DB passwords** in `application.yml` and `postgresdb/ddl/1_init.sql`.
- **Torrent download limit is 5 GB** (not 1 GB). Code at `LibraryOperationService.java:225` uses `5L * 1024 * 1024 * 1024` as the boundary for ZIP vs .torrent download.
- **TTORRENT_HASHING_THREADS** env var defined in `docker-compose.yml:51` with default `2`. **Not currently read by any Java code** — exists as placeholder for future torrent hashing parallelism control. Set lower on low-CPU NAS to avoid I/O saturation if wired up.
- `Game.java` is a decompiled `.class` → `@Entity` uses annotated getters, not fields. Keep this pattern.
- **Frontend must be built separately** before Docker (`npm run build` or `make build-frontend`). Dev server (`npm run dev`) proxies `/game-library/*` to `:8080`.
- **OkHttp 4.x API order**: `RequestBody.create(MediaType, String)` — MediaType first.
- **Transmission must be running** for torrent seeding. In Docker it starts automatically; for local dev run `docker run lscr.io/linuxserver/transmission`.
- **Multi-stage Docker build** requires Docker 19.03+ and BuildKit.
- **Tracker announce URL** must be reachable from user torrent clients. Set `TRACKER_ANNOUNCE_URL` to your NAS IP/hostname in `docker-compose.yml`.
- **uTP must be enabled** in Transmission for P2P connections with uTorrent on Windows. Without it, even though the tracker correctly returns the seeder, data transfer fails because uTorrent cannot connect over pure TCP.
  - The modern key is `preferred_transports`, set **both** in `gameLibraryConfigs/tracker/config/settings.json` (монтируется в `/config` контейнера transmission):
    ```json
    "preferred_transports": ["utp", "tcp"],
    "utp-enabled": true
    ```
  - The container does NOT process `TRANSMISSION_*` env vars (only `USER`, `PASS`, `WHITELIST`, etc.), so `TRANSMISSION_UTP_ENABLED=true` in docker-compose.yml has **no effect**. The fix must be done directly in the host `settings.json`.
  - After editing, `docker-compose restart transmission`.
- **WorldArt screenshot bucket formula**: `((id + 9999) / 10000) * 10000` in `WorldArtScraper.java:220`. World-art.ru stores images in `img/{bucket}/{id}/{num}.jpg` where bucket is a rounded `10000 * ceil(id/10000)`. The optimize_b path format is `img/converted_images_{bucket}/optimize_b/{id}-{num}-optimize_b.jpg`.
- **Images**: served from DB bytea. Optionally, files at `images.directory/games/{id}/logo.jpg` (or `screenshots/`, `avatars/`) override DB — useful for manual replacement.
- **Local dev profile** in `application-alone.yml` overrides games/images/gameLibraryConfigs paths for local development. Activate with `--spring.profiles.active=alone`.
- **SCRAPER_ENCRYPTION_KEY** must be set in `docker-compose.yml` (backend → environment). API-ключи скраперов шифруются этим ключом; при его смене нужно **пересохранить все API-ключи** в админке. Сгенерировать новый: `openssl rand -base64 32`.
- **Playground screenshot URL normalization** (`PlaygroundScraper.java:212-221`). На странице игры Playground каждая скриншота = `<a href="//i/screenshot/{id}/img.jpg">` (фулсайз) + `<img src="/i/screenshot/{id}/img.jpg?1200x675">` (превью с query-параметром). `extractScreenshotsFromDocument` собирает URL из обоих аттрибутов в `LinkedHashSet`, но из-за разницы в `//` и `?query` они не дедуплицируются. Метод `normalizeScreenshotUrl()` убирает `//`, `.webp?`-мусор и всё после `?` **до** добавления в Set, что превращает оба URL в идентичную строку и устраняет дубликаты.
- **GameConverter genre fallback** (`GameConverter.java:123-127`). `dtoToGameGenreEntityConverter` обёрнут в try-catch(`IllegalArgumentException`) и возвращает `null` для неизвестных жанров. Вызывающие методы фильтруют `null` перед добавлением в список жанров игры.
- **Playground Cyrillic genre mappings** (`ScraperConfigService.java:219-221`). Playground дополнительно к 7 английским slug-маппингам получает все русские маппинги из `buildWorldArtGenreMappings()` (~220 записей) + специфичные `"рогалик"→roguelike`, `"глобальная_стратегия"→_4X`. JSON-LD жанры в PlaygroundScraper валидируются через `Genre.valueOf()`, неизвестные тихо пропускаются.
- **Playground search-by-name** (`PlaygroundScraper.java:44-55, 93-103`). Если поле URL в панели скрапера оставить пустым или ввести название игры (не ссылку), PlaygroundScraper делает GET-запрос к `https://www.playground.ru/api/game.search?query={name}&include_addons=1`, получает `slug` первого результата и строит URL `https://www.playground.ru/{slug}` для полного скрапа. Если поиск ничего не вернул — выбрасывается `RuntimeException("Game not found on Playground: ...")`, которое не заворачивается в `catch (Exception)`, а пробрасывается как есть.
- **Scrape checkbox inputId prefix** (`GameEditView.vue:126-127`). Чекбоксы в панели скрапера имеют `:inputId="'scrape-' + opt.key"`, чтобы избежать конфликта с `id="genres"` на компоненте `MultiSelect`. `<label :for="'scrape-' + opt.key">` соответственно.
- **Screenshot save = replacement** (`LibraryController.java:150-155`). `editGame()` использует `gameEdit.getScreenshots()` как полную замену существующих скриншотов (а не merge). Если `null` — сохраняет старые.
- **Frontend scrape screenshot handling** (`GameEditView.vue:339,346`). `handleScrape()` заменяет `newScreenshotPreviews` (не push) и дедуплицирует `form.screenshots` через `new Set()`.
- **Save redirects to game card** (`GameEditView.vue:299-301`). `handleSave()` при успехе делает `router.replace(\`/game/\${route.params.id}\`)` — уходит на карточку, а не остаётся на странице редактирования. `replace` вместо `push`, чтобы кнопка «назад» вела в Library, не возвращаясь на редактор.
- **Back-to-library arrow** (`GameDetailView.vue:19`). На карточке игры есть кнопка `←` (`router.push('/')`).
- **Library state сохраняется в sessionStorage** (`LibraryView.vue`). Перед уходом со страницы Library (`onBeforeUnmount`) состояние (страница, поиск, платформы, годы, жанры, сортировка) пишется в `sessionStorage.libraryState`. При монтировании Library проверяет наличие сохранённого состояния и восстанавливает его. При сбросе фильтров сохранённое состояние удаляется. Это позволяет вернуться на ту же страницу с теми же фильтрами после просмотра/редактирования игры, даже после F5.
- **Сортировка вынесена из GameFilter в LibraryView** (`LibraryView.vue:54-72`). `SelectButton` для сортировки (название, год, дата добавления, рейтинг) и направления отображается над сеткой игр, а не в боковой панели фильтра. Применяется сразу по `@change` без debounce.
- **Virtual threads enabled** (`application.yml:spring.threads.virtual.enabled: true`). Все сервлетные запросы и асинхронные задачи используют виртуальные треды Java 25 (Project Loom). Это даёт высокую конкурентность при малом пуле потоков Tomcat (max 10).
- **Streaming ZIP без сжатия** (`StreamingZipWriter.java`). ZIP-архив пишется методом STORED (без compression), с CRC32 data descriptor. `ZipManifest` предвычисляет размер на лету для заголовка Content-Length. Это позволяет стримить архивы >5 ГБ без буферизации.
- **Download preview (prepare-download)**. Для игр ≥5 ГБ запускается асинхронная задача `TorrentTaskService`, которая готовит .torrent-файл в фоне. Статус проверяется через `GET /api/download/prepare-status/{taskId}`. После готовности клиенту отдаётся .torrent для скачивания через Transmission.
- **Admin password reset — случайная генерация** (`UserDataService.java:102-110`). При сбросе пароля админом генерируется новый пароль (8 байт, base64) через `SecureRandom`. Новый пароль возвращается в API-ответе и отображается в диалоговом окне на фронтенде. Можно переопределить через переменную окружения `RESET_PASSWORD_DEFAULT`.
- **`GET /downloads/aria2-version` проверяет Transmission, не Aria2**. Эндпоинт назван legacy, но на самом деле проверяет connectivity с Transmission RPC. Возвращает `"Transmission is connected"` / `"Transmission is not available"`.
- **Route transition — cross-fade** (`App.vue:9`). Переходы между страницами используют `<Transition name="route-fade" mode="out-in">`. Старый контент затухает, затем появляется новый — без белых вспышек. `.main-container` имеет `background-color: var(--p-surface-50)` (light) / `var(--p-surface-950)` (dark).
- **AppHeader — full-width background + centered content + подменю** (`AppHeader.vue`). Меню фиксировано сверху, фон растянут на всю ширину через `app-header-wrapper`. Внутренний `Menubar` центрирован с `max-width: 1400px`. Пункт «Библиотека» содержит подменю: «Список» (`/`) и «Избранное» (`/?favorites=1`). Панель уведомлений закрывается по клику вне области.
- **primeflex.css — кастомный utility framework** (`frontend/src/assets/styles/primeflex.css`, ~20k строк). Самописный CSS-фреймворк с классами для grid, flex, spacing, colors, surfaces. Не является npm-пакетом.
- **i18n — кастомный composable без библиотеки** (`useI18n.js`). Встроенный словарь на 179 ключей (RU/EN) без vue-i18n. При смене языка — `window.location.reload()` для перезапуска приложения.
- **Debounced filter (250ms)** (`GameFilter.vue`). Все поля фильтра (поиск, платформы, годы, жанры) имеют `watch` с debounce 250ms и авто-применением. Отдельной кнопки Apply нет. Сортировка и избранное вынесены из фильтра — ими управляет `LibraryView` через URL query (`?favorites=1`) и SelectButton.
- **Сканирование ФС в две фазы** (`GameScanerService.java`). Phase 1: запись метаданных в БД (без bytea). Phase 2: добавление изображений с ручным `EntityManager` (через `@PersistenceUnit EntityManagerFactory`) после каждой игры — предотвращает OOM при больших библиотеках. Запускается асинхронно через `POST /api/scan` → progress bar с polling'ом каждые 500ms (`ScanTaskService.java`).
- **OkHttpClient — единый инстанс** (`AppConfig.java`). Настроен в `@Bean OkHttpClient` с 30s timeout, Mozilla User-Agent. Используется всеми скраперами через `JsoupHelper`.
- **Notification `isRead` — JavaBean boolean naming** (`Notification.java:51`). Геттер `isRead()` → Hibernate выводит имя свойства `read`, а не `isRead`. JPQL-запросы и derived query methods обязаны использовать `read` (например `n.read`, `countByUserIdAndReadFalse`). Поле БД `is_read` задаётся через `@Column(name = "is_read")`.
- **Dark mode CSS: `.app-dark`, не `.dark`** (`main.js:23`). PrimeVue настроен с `darkModeSelector: '.app-dark'`. Все кастомные CSS-правила для тёмной темы должны использовать `.app-dark` (не `.dark`), иначе они не сработают.
- **View history — localStorage + composable** (`useViewHistory.js`). Хранит до 20 ID игр в `localStorage.viewHistory`. Композабл подмешивается в `GameDetailView.vue` (добавление) и `LibraryView.vue` (стрип). Очистка только вручную или через localStorage API. При превышении квоты localStorage поэтапно урезает историю до 5, затем до 0.
- **Related games — 2 SQL запроса** (`RelatedGamesController.java`). Возвращает игры: 1) с тем же жанром, 2) с похожим названием через `regexp_replace` и fallback на первое слово при пустом результате. Каждый запрос лимитирован, результаты объединяются в `LinkedHashSet` для дедупликации. «Та же платформа» удалена из-за шума.
- **Rating 1-10** (`GameRating.java` + `RatingController`). Сущность `GameRating` использует `@Id` + `@GeneratedValue` с `@ManyToOne` связями на `Game` и `User`.
- **Comment ownership check** (`CommentController.java:46`). DELETE проверяет `comment.getUser().getId().equals(currentUserId)`, флаг `canDelete` вычисляется на бэкенде и отдаётся в DTO. Админ может удалить любой комментарий.
- **Favorites list — два SQL-запроса** (`LibraryController.java:91-117`). При `favoritesOnly=true` сначала выполняется `getGameIdList()` для получения всех ID (с фильтрами текста/жанров/платформ), затем `retainAll()` выделяет только ID из избранного. После этого `getGameShortListByIds()` загружает полноценные DTO **только для этих ID**, с последующей сортировкой в порядке `gameIdList` для сохранения пагинации. Ранее использовался `getGameList()` с offset/limit, который возвращал игры из общего списка, игнорируя фильтр избранного.
- **CORS: `CORS_ALLOWED_ORIGINS` нужно явно добавить в docker-compose.yml** (`docker-compose.yml:49`). Переменная `CORS_ALLOWED_ORIGINS` не прокинута в контейнер по умолчанию — её нет в `environment` секции `backend`. Если вы обращаетесь к API напрямую (не через Nginx на порту 80), а не через reverse-proxy, браузер шлёт `Origin` и Spring CORS блокирует запрос. Исправление: `.env` → `CORS_ALLOWED_ORIGINS=http://ваш-хост:порт`, и `docker-compose.yml` → `backend.environment` → `- CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}`.
- **CRLF line endings: `.gitattributes`** (`postgresdb/ddl/1_init.sh`). Shell-скрипты с CRLF-окончаниями (`\r\n`) не выполняются в контейнере — шебанг `#!/bin/bash\r` не находит интерпретатор. В корне проекта добавлен `.gitattributes` с `* text=auto eol=lf` для автоматической нормализации. Если файлы продолжают появляться с CRLF, проверить `git config core.autocrlf` — должно быть `false` или `input`.
- **Collection ownership check** (`CollectionController.java`). `canModify()` проверяет `isOwner` (userId коллекции === текущий пользователь) **или** `ROLE_ADMIN`. Админы могут изменять любую коллекцию.
- **Collections API возвращает свои + публичные чужие** (`CollectionController.java`). `GET /api/collections` возвращает собственные коллекции пользователя (сортировка по `updatedAt` desc) И все публичные коллекции других пользователей (дедуплицированы). `CollectionPicker` также показывает все доступные коллекции.
- **CollectionPicker загружает все membership'ы сразу** (`CollectionPicker.vue`). При открытии для каждой коллекции параллельно вызывается `getGames()`, что может быть медленно при большом числе коллекций.
- **Reorder коллекций — полный список** (`CollectionController.java`). `PUT /{id}/games/reorder` требует полный упорядоченный список `gameId`. Каждая запись сохраняется индивидуально, а не bulk update.
- **Статистика использует JdbcTemplate напрямую** (`StatisticsController.java`). Все агрегации — через raw SQL, минуя Hibernate и `GameRepository` (который грузил bytea-логотипы всех игр → OOM). `GameRepository` полностью исключён из контроллера статистики.
- **`totalSizeBytes` — кэширование в БД с lazy-вычислением** (`StatisticsController.java`). В таблицу `game_data` добавлена колонка `total_size_bytes BIGINT`. При запросе статистики: `SELECT SUM(total_size_bytes) WHERE total_size_bytes IS NOT NULL` → если есть NULL-строки, для каждой вычисляется размер через `walkFileTree(maxDepth=3)` с сохранением в БД. После первого вычисления — мгновенный ответ. Размер пересчитывается: (1) при сканировании ФС — для новых игр в фазе `LOADING_IMAGES` и для существующих в фазе `REFRESHING_SIZES`, (2) по кнопке «Обновить размер» на странице статистики → `POST /api/statistics/refresh-sizes` (сбрасывает все `total_size_bytes` в NULL).
- **Statistics genre chart — top 12** (`StatisticsView.vue`). Круговая диаграмма жанров обрезается до первых 12 элементов с `count > 0`.
- **Statistics — top list показывают top 5** (`StatisticsView.vue`), хотя сервер возвращает до 10.
- **Chart.js dependency** (`StatisticsView.vue`). Импортирует `vue-chartjs` + Chart.js. Если их нет в `package.json`, вью не отрендерится.
- **Average rating округляется до 1 знака** на бэкенде (`Math.round(avgRating * 10.0) / 10.0`).
- **Scan progress bar** (`ScanTaskService.java` + `ScanTask.java`). Сканирование ФС переведено на асинхронную модель с progress bar: `POST /api/scan` → `202 Accepted` + `{ taskId }`, фронтенд polling'ит `GET /api/scan/status/{taskId}` каждые 500ms, отображая `ProgressBar` с детерминированным прогрессом (фазы: `SCANNING_DIRS` → `STORING_METADATA` → `LOADING_IMAGES` → `REFRESHING_SIZES` → `COMPLETED`). Выполняется в daemon-потоке `library-scanner` через `singleThreadExecutor`.
  - **`@PersistenceUnit EntityManagerFactory`, не `@PersistenceContext`** — `EntityManagerFactory.createEntityManager()` используется для ручного управления EM в фазе загрузки изображений, потому что `@PersistenceContext` привязан к потоку HTTP-запроса и не работает в daemon-потоке. Каждая игра обрабатывается в отдельной транзакции (`em.getTransaction().begin/commit/close`).
  - **Phase 1 metadata**: `gameService.storeGameMetadata()` — каждый вызов создаёт свою транзакцию через `@Transactional` на `GameDataService`.
  - **Phase 2 images**: ручной EM → `find()` → модификации → commit → close → затем `gameService.updateGameImages()` с `@Transactional` делает merge+save.
  - **Не использовать `TransactionTemplate` + `@PersistenceContext`** — эта комбинация не обеспечивает корректную привязку EntityManager к daemon-потоку. Работает только `EntityManagerFactory` с ручным управлением.
