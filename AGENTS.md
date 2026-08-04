# AGENTS.md

## Project overview

Three distinct services in one repo — not a monorepo toolchain, not a monolith:
- **Backend:** Java 25 + Spring Boot 4.0.7 (Maven), serving at `/game-library/`
- **Frontend:** Vue 3 + Vite 5 (plain JS, no TypeScript), served at `/game-library/`
- **AI service:** Python 3.12 + FastAPI, HuggingFace models (CPU)
- **DB:** PostgreSQL 16 + pgvector, schema `library`

All five services run via `docker compose` (adds transmission + nginx).

## Working directories

```
Command                           Run from
-------                           -------
mvn *                             repo root
npm *                             frontend/
pip / python / uvicorn            ai-service/
docker compose *                  repo root
make *                            repo root
```

## Commands

### Build & run

```
make all                 # build-backend → build-frontend → docker compose up -d
make build-backend       # mvn clean package -DskipTests
make build-frontend      # npm install && npm run build (in frontend/)
make up / make down      # docker compose up/down
make dev-backend         # mvn spring-boot:run (with Java dev tools)
make dev-frontend        # npm run dev (Vite on :5173, proxies API to :8080)
make clean               # docker compose down -v + mvn clean + rm -rf frontend/dist
```

### Run backend alone (no Docker)

```
mvn spring-boot:run -Dspring.profiles.active=alone
```

Uses `application-alone.yml` — connects to localhost DB, expects Windows paths for torrents.

### Lint & format (frontend only)

```
npm run lint             # ESLint + vue3-recommended + prettier, auto-fix
```

No semicolons, single quotes, trailing commas, 120-char width, 2-space indent (see `frontend/.prettierrc`).

### Tests

```
mvn test                           # all backend tests
mvn test -Dtest=ClassName          # single test class
npm run test                       # all frontend tests (Vitest + happy-dom)
npx vitest run src/file.test.js    # single frontend test
```

**Currently there are zero test files in the repo** — JUnit and Vitest are configured but unused.

## Environment & secrets

Copy `.env.example` → `.env` before first build. Four required secrets:
- `GAME_LIBRARY_JWT_SECRET` — JWT signing key
- `GAME_LIBRARY_ADMIN_PASSWORD` — initial admin password
- `IGDB_CLIENT_ID` / `IGDB_CLIENT_SECRET` — optional, for IGDB scraper
- `SCRAPER_ENCRYPTION_KEY` — AES-256 key for encrypting scraper API keys in DB

## Database migrations

**No Flyway, no Liquibase.** DDL scripts in `postgresdb/ddl/` execute alphabetically via PostgreSQL's `docker-entrypoint-initdb.d` at first container start. For bare-metal: run them manually in order with `psql`.

Never create migration files — add new scripts to `postgresdb/ddl/` with the next numeric prefix.

## Architecture notes

- **Base path:** Everything is under `/game-library/` (Spring `context-path`, Vite `base`, Vue Router `createWebHistory`, nginx `location`). Never add routes without this prefix.
- **Auth:** JWT access token (15min) + refresh token (7d) with in-memory blacklist. Refresh is handled silently by the Axios interceptor (`frontend/src/api/axios.js`).
- **Rate limiting:** In-memory bucket4j — 5 req/min for login per IP+User-Agent, 100 req/min for general API. Returns 429.
- **Virtual threads:** Enabled via `spring.threads.virtual.enabled: true`. All servlet requests run on virtual threads.
- **API docs:** Swagger UI at `/game-library/swagger-ui.html`, OpenAPI groups: `public` and `admin`.
- **Images:** Stored as `bytea` in DB with optional filesystem override. Support ETag + Cache-Control (24h).
- **ZIP downloads:** Custom `StreamingZipWriter` using STORED method (no compression) for files < 5 GB.
- **i18n:** Backend uses `messages_{en,ru}.properties`. Frontend uses `useI18n.js` composable + Pinia locale store. Two languages: Russian (default) and English.
- **Scrapers:** 7 sources (Playground, Igromania, Steam, IGDB, TheGamesDB, World-Art, PsxDataCenter). API keys encrypted with AES-256 in DB via `ConfigEncryptionService`.
- **BitTorrent tracker:** Built-in HTTP tracker at `/api/tracker/announce`. Transmission handles P2P seeding. `.torrent` files generated via `ttorrent` library (version 1.5).

## Style conventions

- **No TypeScript** — write plain JavaScript for all frontend code
- **Vue component names:** `vue/multi-word-component-names` is turned off — single-word component names are fine
- **`v-html`:** Allowed but emits a warning; must sanitize with DOMPurify first (use `useSanitizeHtml` composable)
- **Backend package:** `com.jenikmax.game.library` — all new code goes here or in sub-packages
- **No code generation** — no OpenAPI codegen, protobuf, or GraphQL schemas

## Прочее
- ** Приложение расчитано для работы на NAS серверах в локальной сети на слабых процессорах
- ** Минимальные системные требования TODO
- ** Бэкенд приложения не собирать
- ** Отвечать в переписке на русском 