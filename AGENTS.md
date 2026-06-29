# GameLibrary — Agent Instructions

## Build & Run

```bash
mvn clean package -DskipTests              # builds target/game-library.jar
cd frontend && npm install && npm run build  # builds dist/ with Vue SPA
make all                                     # or docker-compose up --build -d
mvn spring-boot:run                          # run backend locally (needs PostgreSQL)
cd frontend && npm run dev                   # run frontend dev server (:5173, proxies to :8080)
```

No Maven wrapper — `mvn` must be on PATH. Java 11 target, `eclipse-temurin:11-jre-alpine` runtime. No tests, no lint/format/typecheck config.

## Docker

`docker-compose.yml` starts four services: `frontend` (Nginx + Vue SPA, `:80`), `backend` (Spring Boot REST, `:8080`), `transmission` (torrent seeder, `:9091` RPC, `:51413`), `postgresdb` (`:5432`).  
DB lifecycle scripts in `postgresdb/`. Schema in `ddl/*.sql`, copied into `/docker-entrypoint-initdb.d/`.

## Architecture

- Spring Boot 2.7.1 → `jar` packaging, embedded Tomcat.
- Context path `/game-library` (`application.yml:server.servlet.context-path`).
- Frontend: separate Vue 3 + Vite + PrimeVue 4 project in `frontend/`, served via Nginx.
- API: JSON REST (`/game-library/api/**`) with JWT auth; Swagger UI at `/game-library/swagger-ui.html`.
- Legacy Thymeleaf views still work alongside REST (dual auth: form login + JWT).
- PostgreSQL 16 schema `library`, managed via Commons DBCP 1.4 + JPA (Hibernate 5.6.15).
- Auth: Spring Security, form login + JWT, BCrypt, `ROLE_ADMIN`/`ROLE_USER`.
- Torrent: embedded HTTP tracker (`/api/tracker/announce`) + Transmission 4.1.2 RPC for seeding.
- Images: DB bytea with optional filesystem override at `images.directory/games/{id}/logo.jpg` (or `screenshots/`, `avatars/`).
- Scrapers (6 active): Playground (CSS selectors + search API), Igromania (JSON paths), WorldArt (CSS selectors), Steam (Storefront API, no key required), IGDB (Twitch OAuth), TheGamesDB (API key).
- State: Pinia stores for auth, library, locale.
- Rich text: VueQuill + Quill 2 for game description editing.
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
- Либо отредактировать `scrapers/scrapers-config.json` напрямую — но тогда токен будет в plaintext, т.к. шифрование происходит только при записи через API.

## TheGamesDB Scraper Setup

TheGamesDB требует API-ключ. Чтобы получить его:

1. Зарегистрируйся на https://thegamesdb.net/register.php
2. Подтверди email
3. Войди на сайт
4. Перейди на https://api.thegamesdb.net/key.php — ключ будет отображён
5. Скопируй ключ

Куда прописать:
- В админке (`GET /api/admin/scraper-config/thegamesdb`) → поле `encryptedApiKey`
- Либо отредактировать `scrapers/scrapers-config.json` напрямую (будет в plaintext)

Лимит: 1000 запросов в месяц. Один поиск игры может использовать 2-3 запроса (поиск + жанры + скриншоты).

## Gotchas

- **Plaintext DB passwords** in `application.yml` and `postgresdb/ddl/1_init.sql`.
- **Torrent download limit is 5 GB** (not 1 GB). Code at `LibraryOperationService.java:225` uses `5L * 1024 * 1024 * 1024` as the boundary for ZIP vs .torrent download.
- **TTORRENT_HASHING_THREADS** env var controls hashing parallelism for torrent creation. Default `2`. Set lower on low-CPU NAS to avoid I/O saturation.
- `Game.java` is a decompiled `.class` → `@Entity` uses annotated getters, not fields. Keep this pattern.
- **Frontend must be built separately** before Docker (`npm run build` or `make build-frontend`). Dev server (`npm run dev`) proxies `/game-library/*` to `:8080`.
- **OkHttp 3.x API order**: `RequestBody.create(MediaType, String)` — MediaType first.
- **Transmission must be running** for torrent seeding. In Docker it starts automatically; for local dev run `docker run lscr.io/linuxserver/transmission`.
- **Multi-stage Docker build** requires Docker 19.03+ and BuildKit.
- **Tracker announce URL** must be reachable from user torrent clients. Set `TRACKER_ANNOUNCE_URL` to your NAS IP/hostname in `docker-compose.yml`.
- **uTP must be enabled** in Transmission for P2P connections with uTorrent on Windows. Without it, even though the tracker correctly returns the seeder, data transfer fails because uTorrent cannot connect over pure TCP.
  - The modern key is `preferred_transports`, set **both** in `gameLibraryConfigs/tracker/config/settings.json`:
    ```json
    "preferred_transports": ["utp", "tcp"],
    "utp-enabled": true
    ```
  - The container init script (`init-transmission-config/run`) does NOT process `TRANSMISSION_*` env vars (only `USER`, `PASS`, `WHITELIST`, etc.), so `TRANSMISSION_UTP_ENABLED=true` in docker-compose.yml has **no effect**. The fix must be done directly in the host `settings.json` — it survives restarts because the init script doesn't touch these keys.
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
