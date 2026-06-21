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
- Images: filesystem (`/gameLibrary/images/`) with DB bytea fallback.
- Scrapers (6 active): Playground (CSS selectors), Igromania (JSON paths), WorldArt (CSS selectors), Steam (Storefront API, no key required), IGDB (Twitch OAuth), TheGamesDB (API key).
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
- **Images on filesystem**: after DB→FS migration (`POST /api/admin/migrate-images` or `scripts/migrate-images.sh`), images are served from disk. If the file is missing, falls back to DB bytea.
- **Local dev profile** in `application-alone.yml` overrides games/images/gameLibraryConfigs paths for local development. Activate with `--spring.profiles.active=alone`.
- **SCRAPER_ENCRYPTION_KEY** must be set in `docker-compose.yml` (backend → environment). API-ключи скраперов шифруются этим ключом; при его смене нужно **пересохранить все API-ключи** в админке. Сгенерировать новый: `openssl rand -base64 32`.
