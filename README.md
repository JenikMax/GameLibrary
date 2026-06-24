# GameLibrary

**EN:** Game catalog manager for NAS. Scans filesystem, fetches metadata from scrapers, and provides a web UI for browsing, searching, editing, and downloading games via P2P torrents.

**RU:** Каталогизатор компьютерных игр для NAS. Сканирует файловую систему, собирает метаданные через скраперы и предоставляет веб-интерфейс для просмотра, поиска, редактирования и скачивания игр через P2P-торренты.

---

<p align="center">
  <a href="#en">🇬🇧 English</a> &nbsp;|&nbsp; <a href="#ru">🇷🇺 Русский</a>
</p>

---

<a name="en"></a>

# English

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Backend | Spring Boot 2.7.1, Java 11 |
| Frontend | Vue 3 + Vite 5, PrimeVue 4, Pinia, VueQuill (Quill 2) |
| Database | PostgreSQL 16 (schema `library`) |
| ORM / JDBC | Hibernate 5.6.15, Spring Data JPA, Commons DBCP 1.4 |
| REST API | Spring MVC `@RestController`, JWT auth |
| API Docs | OpenAPI / Swagger UI |
| Downloads | Transmission (JSON-RPC) + built-in HTTP tracker |
| Networking | OkHttp 3, Jsoup (scraping) |
| Backend build | Maven, JAR packaging |
| Frontend build | npm / Vite |
| Containerization | Docker, docker-compose |

## Architecture

```
┌─────────┐   :80   ┌──────────┐   :8080  ┌────────────────┐
│ Browser │ ──────▶ │  Nginx   │ ──────▶  │   Backend      │
└─────────┘         │ (Vue SPA)│          │  (REST API)    │
                    └──────────┘          │  + Tracker     │
                                          └───┬────────┬───┘
                                              │        ▲
                                              │        │ announce
                                              │  ┌─────┴────────┐
                                              │  │  Transmission│ :9091 RPC
                                              │  │  (seeder)    │ :51413 P2P
                                              │  └──────┬───────┘
                                              │         │
                     ┌──────────────┐         │
                     │  PostgreSQL  │  :5432  │
                     └──────────────┘         │
                     User torrent clients
                     (qBittorrent, Transmission, etc.)
                         │              ▲
                         └───── P2P ────┘
```

### Frontend (Vue 3 + PrimeVue)

`frontend/` — standalone SPA communicating with backend via REST API + JWT.

| URL | Access | Description |
|-----|--------|-------------|
| `/login` | all | Login form |
| `/register` | all | Registration |
| `/` | USER, ADMIN | Library: game grid, filters, pagination |
| `/game/:id` | USER, ADMIN | Game detail page |
| `/game/:id/edit` | ADMIN | Editing + scraping |
| `/profile` | USER | Profile, avatar, password change |
| `/admin/users` | ADMIN | User management |
| `/admin/scrapers` | ADMIN | Scraper config (enable/disable, API keys) |
| `/downloads` | USER, ADMIN | Transmission seeding status |

### Backend (Spring Boot REST API)

`/game-library/api/` — JSON REST API with JWT authentication.

| Endpoint | Access | Description |
|----------|--------|-------------|
| `POST /api/auth/login` | all | JWT login |
| `POST /api/auth/register` | all | Registration |
| `GET /api/auth/me` | USER, ADMIN | Current user |
| `GET /api/games` | USER, ADMIN | Game list (filters, sorting, pagination) |
| `GET /api/games/{id}` | USER, ADMIN | Game details |
| `POST /api/games/{id}/edit` | ADMIN | Edit game |
| `POST /api/games/{id}/grab` | ADMIN | Scrape metadata |
| `GET /api/games/{id}/download` | USER, ADMIN | Download (ZIP/.torrent) |
| `POST /api/games/{id}/seed` | USER, ADMIN | Start seeding via Transmission |
| `GET /api/downloads/active` | USER, ADMIN | Active Transmission seeds |
| `GET /api/tracker/announce` | all | BitTorrent HTTP tracker |
| `GET /api/profile` | USER, ADMIN | User profile |
| `POST /api/scan` | ADMIN | Scan library |
| `GET /api/admin/users` | ADMIN | User list |
| `POST /api/admin/users/{id}/toggle-admin` | ADMIN | Toggle admin role |
| `POST /api/admin/users/{id}/toggle-active` | ADMIN | Block/unblock user |
| `POST /api/admin/users/{id}/reset-password` | ADMIN | Reset password |
| `GET /api/admin/scraper-config` | ADMIN | Scraper configs |
| `POST /api/admin/scraper-config` | ADMIN | Save scraper configs |
| `POST /api/admin/scraper-config/reload` | ADMIN | Reload scrapers from file |
| `POST /api/admin/migrate-images` | ADMIN | Migrate images from DB to FS |

Swagger UI: `/game-library/swagger-ui.html`

## Filesystem Layout

```
<games_directory>/games/
└── <platform>/                       (e.g. PC, PlayStation, Xbox)
    └── <game_name>/
        ├── <game files>...
        └── information/              (created during scan)
            ├── logo.jpg              (poster)
            ├── information.json      (name, year, genres, description, trailer, manual)
            └── img/                  (screenshots .jpg)
```

## Features

### Library Scan (ADMIN)
- Traverses `<games>/<platform>/<game_name>/` structure
- Creates `information/` if missing
- Auto-adds/removes DB records

### Browsing & Filtering
- Game grid with posters
- Filters: platform, genre, year, name search
- Sorting: name, year, added date
- Pagination (12 games per page)

### Game Editing (ADMIN)
- Name, year, platform, description (Quill editor), manual, trailer
- Genres from shared dictionary
- Metadata scraping (6 active scrapers: Playground.ru / Igromania.ru / World-Art / Steam / IGDB / TheGamesDB)

### Downloads
- **Small games (<5 GB)**: streaming ZIP archive
- **Large games (≥5 GB)**: .torrent with built-in HTTP tracker announce URL (`/api/tracker/announce`) + seeding via Transmission
- **Seed via Transmission**: button on game page — creates .torrent and starts seeding on NAS

### Users & Roles
- **ADMIN**: scanning, editing, scraping, user management
- **USER**: browsing, downloading, profile

### Localization
Russian and English languages. Switch via `?lang=ru` / `?lang=en`.

## Running

### Full Stack (Docker)

```bash
# Build and start all services
make all

# Or step by step:
make build-backend    # mvn clean package -DskipTests
make build-frontend   # cd frontend && npm run build
make up               # docker-compose up --build -d
```

Services:

| Port | Service | Purpose |
|------|---------|---------|
| `:80` | frontend (Nginx) | Vue SPA + API proxy |
| `:8080` | backend | Spring Boot REST API + built-in HTTP tracker |
| `:9091` | transmission | RPC management |
| `:51413` | transmission | P2P traffic (TCP/UDP) |
| `:5432` | postgresdb | PostgreSQL |

### Local Development

```bash
# Backend (requires local PostgreSQL + Transmission)
mvn spring-boot:run -Dspring.profiles.active=alone

# Frontend (Vite dev server with proxy to localhost:8080)
cd frontend && npm run dev
```

Profile `alone` (`application-alone.yml`) overrides games, tracker and images paths for local development.

Makefile helper targets:
```bash
make dev-backend    # mvn spring-boot:run
make dev-frontend   # cd frontend && npm run dev
make logs           # docker-compose logs -f
make clean          # docker-compose down -v && mvn clean && rm -rf frontend/dist
```

## Image Migration (DB → FS)

Images (logos, screenshots, avatars) are stored in DB as `bytea`.
To migrate to filesystem:

```bash
# Option 1: bash script
./scripts/migrate-images.sh /gameLibrary/images

# Option 2: via REST API (ADMIN)
curl -X POST http://localhost:8080/game-library/api/admin/migrate-images \
  -H "Authorization: Bearer <token>"
```

After migration, images are served from disk via `/api/images/**`
with automatic DB fallback if the file is not found.

## Database

Tables in `library` schema:

| Table | Purpose |
|-------|---------|
| `game_data` | Games (id, name, platform, release_date, description, instruction, trailer_url, logo, directory_path) |
| `game_genre` | Genre dictionary (code, description, description_ru) — ~70 genres |
| `game_data_genre` | M:N game-genre relationship |
| `game_screenshot` | Screenshots (source bytea) |
| `library_user` | Users (user_name, pass BCrypt, is_admin, is_active, avatar bytea) |

PostgreSQL container: `postgresdb/Dockerfile` (base `postgres:16.14`).

DDL executed in sort order: `postgresdb/ddl/1_init.sql` (user + DB + schema), `2_library.sql` (game tables + ~70 genres), `3_user.sql` (user table + admin/guest seed).

DB management scripts in `postgresdb/`: `01_build.sh`, `02_up.sh`, `03_build-and-up.sh`, `04_kill.sh`, `05_stop.sh`, `06_recreate.sh`.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Backend port |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `GAMES_DIRECTORY` | `/gameLibrary` | Game files root |
| `IMAGES_DIRECTORY` | `/gameLibrary/images` | Images on FS |
| `TRACKER_ANNOUNCE_URL` | `http://localhost:8080/game-library/api/tracker/announce` | Announce URL for .torrent (must be reachable by clients) |
| `TRANSMISSION_RPC_URL` | `http://transmission:9091/transmission/rpc` | Transmission RPC |
| `TRANSMISSION_DOWNLOAD_DIR` | `/downloads` | Game directory (in Transmission container) |
| `JWT_SECRET` | (default key) | JWT secret |
| `JWT_EXPIRATION_MS` | `86400000` | Token TTL (24h) |
| `SCRAPER_CONFIG_DIR` | `/gameLibrary/gameLibraryConfigs/scrapers` | Directory with `scrapers-config.json` |
| `SCRAPER_ENCRYPTION_KEY` | (not set) | AES-256 key (base64) for scraper API key encryption. **Required to persist keys across restarts**. Generate: `openssl rand -base64 32` |
| `TORRENT_DIR_TMP` | `/torrentDirTmp` | Temporary directory for .torrent files |
| `TTORRENT_HASHING_THREADS` | `2` | Threads for torrent hashing (speeds up .torrent creation) |

## Scrapers

Scraper configuration is stored in `scrapers/scrapers-config.json` and managed via `/api/admin/scraper-config`.

| Scraper | Status | Method | Description |
|---------|--------|--------|-------------|
| Playground (playground.ru) | ✅ Active | CSS selectors | Name, description, genres, screenshots |
| Igromania (igromania.ru) | ✅ Active | JSON Path | Scrapes via `props` initialStoreState |
| Steam (store.steampowered.com) | ✅ Active | Storefront API | Steam Storefront, no API key needed |
| IGDB (api.igdb.com) | ✅ Active | REST API + Bearer | Requires Client-ID + Bearer token (Twitch OAuth) |
| TheGamesDB (api.thegamesdb.net) | ✅ Active | REST API | Requires API key (register at thegamesdb.net) |
| World-Art (world-art.ru) | ✅ Active | CSS selectors | Direct card parsing + search |

### IGDB Scraper Setup

IGDB uses Twitch OAuth 2.0. You need a **Client-ID** and **Access Token**:

1. Go to https://dev.twitch.tv/console/apps/create
2. Create an app (Name — any, OAuth Redirect URLs — `http://localhost`, Category — any)
3. Copy the **Client-ID** from the app list
4. Click **New Secret** → copy the **Client Secret**
5. Get an access token:

   ```bash
   curl -X POST "https://id.twitch.tv/oauth2/token?client_id=YOUR_CLIENT_ID&client_secret=YOUR_SECRET&grant_type=client_credentials"
   ```

6. Copy the `access_token` from the JSON response.

Where to set:
- **Client-ID** → admin panel (`/admin/scrapers`) → IGDB → `headers.Client-ID` field
- **access_token** → same place → `encryptedApiKey` field (admin encrypts it automatically)

### TheGamesDB Scraper Setup

TheGamesDB requires an API key:

1. Register at https://thegamesdb.net/register.php
2. Confirm email, log in
3. Go to https://api.thegamesdb.net/key.php — the key will be displayed
4. Copy the key

Where to set:
- **API key** → admin panel (`/admin/scrapers`) → TheGamesDB → `encryptedApiKey` field

**Rate limit:** 1000 requests per month. A single game search may use 2-3 requests (search + genres + screenshots).

For gotchas, see `AGENTS.md`.

## Linux Deployment

### Prerequisites

| Component | Version | Installation |
|-----------|---------|-------------|
| Java | 11 (JDK) | `apt install openjdk-11-jdk` / `sdk install java 11.0.26-tem` |
| Maven | 3.6+ | `apt install maven` |
| Node.js | 18+ | `apt install nodejs npm` / `nvm install 18` |
| Docker | 19.03+ | `apt install docker.io docker-compose-v2` |
| PostgreSQL | 12+ | `apt install postgresql` (local dev only) |

### Configuration

Edit `application.yml` before building:

```yaml
game-library:
  games:
    directory: /mnt/nas/gameLibrary     # path to your game library
  images:
    directory: /mnt/nas/gameLibrary/images
  tracker:
    announce-base-url: http://192.168.1.100:8080/game-library/api/tracker/announce  # your NAS IP

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/game-library?currentSchema=library
    username: library-manager-user
    password: 2wq345tgfiNcbBBwee3
```

### Transmission Directory Structure

When running via Docker, the Transmission container uses bind mounts for game files, configs, and service directories. Create the following structure on the host before starting:

```
nas/gameLibrary/                  # library root
├── games/                        # game files (PC, PS3, ...)
├── images/                       # screenshots and covers
└── gameLibraryConfigs/           # configuration data
    ├── db/                       # PostgreSQL data (Docker volume)
    └── tracker/                  # Transmission-related data
        ├── config/               # settings.json, queue.json (auto-created)
        ├── watch/                # auto-add .torrent files
        ├── complete/             # completed downloads
        ├── incomplete/           # incomplete downloads
        └── torrents/             # .torrent files created by backend
```

**Create directories (Linux):**
```bash
mkdir -p /mnt/nas/gameLibrary/games
mkdir -p /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/{config,watch,complete,incomplete,torrents}
```

**Create directories (Windows):**
```cmd
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\config
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\watch
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\complete
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\incomplete
```

Note: `PUID` / `PGID` in `docker-compose.yml` must match the owner UID/GID of the game files. On WSL/Windows, files are accessible even if `chown` in the container shows a warning.

### Transmission Configuration

`gameLibraryConfigs/tracker/config/settings.json` is auto-created on first container start. Important parameters are set via environment variables in `docker-compose.yml` (`environment` section of the `transmission` service):

| Variable | Value | settings.json param |
|---|---|---|
| `TRANSMISSION_DOWNLOAD_DIR` | `/downloads/complete` | `download-dir` |
| `TRANSMISSION_INCOMPLETE_DIR` | `/downloads/incomplete` | `incomplete-dir` |
| `TRANSMISSION_INCOMPLETE_DIR_ENABLED` | `true` | `incomplete-dir-enabled` |
| `TRANSMISSION_WATCH_DIR` | `/watch` | `watch-dir` |
| `TRANSMISSION_WATCH_DIR_ENABLED` | `true` | `watch-dir-enabled` |

To change other Transmission parameters (RPC port, speed limits, etc.) — edit `settings.json` on the host and restart the container.

### Build & Run (Docker)

```bash
# 1. Clone repository
git clone <url> /opt/game-library
cd /opt/game-library

# 2. Build backend (JAR)
mvn clean package -DskipTests

# 3. Build frontend (Vue SPA)
cd frontend
npm install
npm run build
cd ..

# 4. Create host directory structure for Transmission:
#    mkdir -p /mnt/nas/gameLibrary/games
#    mkdir -p /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/{config,watch,complete,incomplete,torrents}
#
#    See "Transmission Directory Structure" section for details.

# 5. Configure volume paths in docker-compose.yml
#    Edit the volumes section for backend and transmission services:
#
#    backend:
#      volumes:
#        - /mnt/nas/gameLibrary:/gameLibrary
#        - /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/torrents:/torrentDirTmp
#
#    transmission:
#      volumes:
#        - /mnt/nas/gameLibrary/games:/downloads/games
#        - /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/config:/config
#        - /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/watch:/watch
#        - /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/complete:/downloads/complete
#        - /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/incomplete:/downloads/incomplete

# 6. Start all services
docker compose up --build -d

# 7. Verify:
#    - Frontend: http://localhost
#    - Swagger UI: http://localhost/game-library/swagger-ui.html
#    - Transmission Web UI: http://localhost:9091
#    - API: http://localhost:8080/game-library/api/games
```

### Build & Run (without Docker)

```bash
# 1. Database
sudo -u postgres psql -f postgresdb/ddl/1_init.sql
sudo -u postgres psql -f postgresdb/ddl/2_library.sql
sudo -u postgres psql -f postgresdb/ddl/3_user.sql

# 2. Transmission (in a separate terminal)
docker run -d --name transmission \
  -p 9091:9091 -p 51413:51413 -p 51413:51413/udp \
  -v /mnt/nas/gameLibrary/games:/downloads/games \
  -v /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/config:/config \
  -v /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/watch:/watch \
  -v /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/complete:/downloads/complete \
  -v /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/incomplete:/downloads/incomplete \
  -e PUID=$(id -u) -e PGID=$(id -g) \
  lscr.io/linuxserver/transmission

# 3. Backend
mvn spring-boot:run

# 4. Frontend (in a separate terminal)
cd frontend && npm run dev
# Frontend at http://localhost:5173, API proxied to :8080
```

## Windows Deployment

### Prerequisites

| Component | Version | Download |
|-----------|---------|----------|
| Java | 11 (JDK) | [Adoptium Temurin 11](https://adoptium.net/temurin/releases/?version=11) |
| Maven | 3.6+ | [Maven Download](https://maven.apache.org/download.cgi) |
| Node.js | 18+ | [Node.js 18 LTS](https://nodejs.org/) |
| Docker Desktop | latest | [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/) |
| PostgreSQL | 12+ | [PostgreSQL Download](https://www.postgresql.org/download/windows/) |
| Git | any | [Git for Windows](https://git-scm.com/download/win) |

**Make sure Git, Java, Maven, Node.js are in your `PATH`:**

```cmd
java -version
mvn -version
node --version
npm --version
```

### Configuration

Edit `application.yml` before building (use forward slashes):

```yaml
game-library:
  games:
    directory: D:/GameLibrary              # your game library
  images:
    directory: D:/GameLibrary/images
  tracker:
    announce-base-url: http://192.168.1.100:8080/game-library/api/tracker/announce

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/game-library?currentSchema=library
    username: library-manager-user
    password: 2wq345tgfiNcbBBwee3
```

### Build & Run (Docker Desktop)

```powershell
# 1. Clone repository
git clone <url> C:\Projects\GameLibrary
cd C:\Projects\GameLibrary

# 2. Build backend (JAR)
mvn clean package -DskipTests

# 3. Build frontend (Vue SPA)
cd frontend
npm install
npm run build
cd ..

# 4. Create host directory structure:
#    mkdir D:\GameLibrary\gameLibraryConfigs\tracker\config
#    mkdir D:\GameLibrary\gameLibraryConfigs\tracker\watch
#    mkdir D:\GameLibrary\gameLibraryConfigs\tracker\complete
#    mkdir D:\GameLibrary\gameLibraryConfigs\tracker\incomplete

# 5. Configure volume paths in docker-compose.yml
#    Replace Linux paths with Windows paths:
#
#    backend:
#      volumes:
#        - D:/GameLibrary:/gameLibrary
#        - D:/GameLibrary/gameLibraryConfigs/tracker/torrents:/torrentDirTmp
#
#    transmission:
#      volumes:
#        - D:/GameLibrary/games:/downloads/games
#        - D:/GameLibrary/gameLibraryConfigs/tracker/config:/config
#        - D:/GameLibrary/gameLibraryConfigs/tracker/watch:/watch
#        - D:/GameLibrary/gameLibraryConfigs/tracker/complete:/downloads/complete
#        - D:/GameLibrary/gameLibraryConfigs/tracker/incomplete:/downloads/incomplete

# 6. Start all services
docker compose up --build -d
```

### Build & Run (without Docker)

#### 1. PostgreSQL

Install PostgreSQL, create DB via pgAdmin or psql:

```powershell
psql -U postgres -f postgresdb\ddl\1_init.sql
psql -U postgres -f postgresdb\ddl\2_library.sql
psql -U postgres -f postgresdb\ddl\3_user.sql
```

#### 2. Transmission

Install [Transmission for Windows](https://transmissionbt.com/download/) or run via Docker Desktop:

```powershell
docker run -d --name transmission \
  -p 9091:9091 -p 51413:51413 -p 51413:51413/udp \
  -v D:\GameLibrary\games:/downloads/games \
  -v D:\GameLibrary\gameLibraryConfigs\tracker\config:/config \
  -v D:\GameLibrary\gameLibraryConfigs\tracker\watch:/watch \
  -v D:\GameLibrary\gameLibraryConfigs\tracker\complete:/downloads/complete \
  -v D:\GameLibrary\gameLibraryConfigs\tracker\incomplete:/downloads/incomplete \
  lscr.io/linuxserver/transmission
```

#### 3. Backend

```powershell
mvn spring-boot:run
```

API at `http://localhost:8080/game-library/api/`.

#### 4. Frontend (Vite dev server)

```powershell
cd frontend
npm install
npm run dev
```

Frontend at `http://localhost:5173`, API proxied to `:8080`.

### Windows Path Configuration

When using Docker Desktop, volumes can only be mounted from directories shared in Docker Desktop settings:
- Settings → Resources → File Sharing
- Add `D:\` or your games folder (`D:\GameLibrary`)

For local development without Docker, use direct Windows paths in `application.yml`.
`information.json` must use `/` (Unix-style) paths — Java normalizes them automatically on any platform.

## Common Issues

### Frontend not loading, blank page
- Check nginx.conf: location `/game-library` must proxy to backend.
- For local dev: Vite dev server must be on `:5173`, backend on `:8080`.
- Open F12 (Console/Network) — check loading errors.

### Backend cannot connect to PostgreSQL
- Check that PostgreSQL is running (Docker: `docker compose logs postgresdb`).
- Check `DB_HOST` and `DB_PORT`: in Docker use service name `postgresdb`, locally use `localhost`.
- Verify password: `2wq345tgfiNcbBBwee3` for user `library-manager-user`.

### Transmission not responding
- Docker: `docker compose logs transmission` — check logs.
- Check `TRANSMISSION_RPC_URL`: Docker `http://transmission:9091/transmission/rpc`, local `http://localhost:9091/transmission/rpc`.
- Verify Transmission is running: `curl -X GET http://localhost:9091/transmission/rpc` should return `X-Transmission-Session-Id` header.
- Ensure `PUID`/`PGID` in docker-compose.yml match the owner of game files.
- If logs show `Couldn't bind to [::]:9091` — Transmission tries IPv6 but Docker Desktop on Windows/WSL doesn't support it. Edit `gameLibraryConfigs/tracker/config/settings.json`, replace `"rpc-bind-address": "[::]"` with `"rpc-bind-address": "0.0.0.0"` and restart the container.

### Images not displaying
- After migration, images are stored on disk, path must match `IMAGES_DIRECTORY`.
- If file is missing — DB fallback is used (slower).
- Run migration: `POST /api/admin/migrate-images` or `scripts/migrate-images.sh`.

### Transmission seeds but clients can't download (uTorrent etc.)

Tracker works (backend logs show `returning complete=1`), uTorrent sees the seeder but data doesn't transfer.

**Fix:** enable uTP in Transmission settings. Edit `gameLibraryConfigs/tracker/config/settings.json` on the host:

```json
{
    "preferred_transports": ["utp", "tcp"],
    "utp-enabled": true
}
```

Then restart the container:

```bash
docker-compose restart transmission
```

**Important:** In Transmission 4.x the `utp-enabled` key is deprecated. The primary way to control uTP is `preferred_transports`. If it doesn't contain `"utp"`, uTP is disabled even with `utp-enabled: true`. Make sure both keys are present.

The `TRANSMISSION_UTP_ENABLED=true` env var in `docker-compose.yml` **does not work** — the linuxserver/transmission init script only processes `USER`, `PASS`, `WHITELIST`, `HOST_WHITELIST`, `PEERPORT`, `UMASK`. Settings must be edited directly in `settings.json` on the host — they persist across restarts.

### Error "no suitable method found for create"
- OkHttp 3.x: `RequestBody.create(MediaType, String)` — MediaType first, then String.
- If you upgraded to OkHttp 4.x, the API changed. This project uses OkHttp 3.14.9.

---

<a name="ru"></a>

# Русский

## Технологический стек

| Компонент | Технология |
|-----------|-----------|
| Backend | Spring Boot 2.7.1, Java 11 |
| Frontend | Vue 3 + Vite 5, PrimeVue 4, Pinia, VueQuill (Quill 2) |
| База данных | PostgreSQL 16 (схема `library`) |
| ORM / JDBC | Hibernate 5.6.15, Spring Data JPA, Commons DBCP 1.4 |
| REST API | Spring MVC `@RestController`, JWT auth |
| Документация API | OpenAPI / Swagger UI |
| Скачивание | Transmission (JSON-RPC) + встроенный HTTP-трекер |
| Сеть | OkHttp 3, Jsoup (скрапинг) |
| Сборка backend | Maven, JAR packaging |
| Сборка frontend | npm / Vite |
| Контейнеризация | Docker, docker-compose |

## Архитектура

```
┌─────────┐   :80   ┌──────────┐   :8080  ┌────────────────┐
│ Browser │ ──────▶ │  Nginx   │ ──────▶  │   Backend      │
└─────────┘         │ (Vue SPA)│          │  (REST API)    │
                    └──────────┘          │  + Tracker     │
                                          └───┬────────┬───┘
                                              │        ▲
                                              │        │ announce
                                              │  ┌─────┴────────┐
                                              │  │  Transmission│ :9091 RPC
                                              │  │  (seeder)    │ :51413 P2P
                                              │  └──────┬───────┘
                                              │         │
                     ┌──────────────┐         │
                     │  PostgreSQL  │  :5432  │
                     └──────────────┘         │
                     Пользовательские торрент-клиенты
                     (qBittorrent, Transmission, и др.)
                         │              ▲
                         └───── P2P ────┘
```

### Frontend (Vue 3 + PrimeVue)

`frontend/` — отдельное SPA-приложение, общающееся с backend через REST API + JWT.

| URL | Доступ | Описание |
|-----|--------|----------|
| `/login` | все | Форма входа |
| `/register` | все | Регистрация |
| `/` | USER, ADMIN | Библиотека: сетка игр, фильтры, пагинация |
| `/game/:id` | USER, ADMIN | Детальная карточка игры |
| `/game/:id/edit` | ADMIN | Редактирование + scraping |
| `/profile` | USER | Профиль, аватар, смена пароля |
| `/admin/users` | ADMIN | Управление пользователями |
| `/admin/scrapers` | ADMIN | Настройка scraper-ов (вкл/выкл, API-ключи) |
| `/downloads` | USER, ADMIN | Статус раздач Transmission |

### Backend (Spring Boot REST API)

`/game-library/api/` — JSON REST API с JWT-аутентификацией.

| Endpoint | Доступ | Описание |
|----------|--------|----------|
| `POST /api/auth/login` | все | JWT login |
| `POST /api/auth/register` | все | Регистрация |
| `GET /api/auth/me` | USER, ADMIN | Текущий пользователь |
| `GET /api/games` | USER, ADMIN | Список игр (фильтры, сортировка, пагинация) |
| `GET /api/games/{id}` | USER, ADMIN | Детальная информация |
| `POST /api/games/{id}/edit` | ADMIN | Редактирование игры |
| `POST /api/games/{id}/grab` | ADMIN | Скраппинг метаданных |
| `GET /api/games/{id}/download` | USER, ADMIN | Скачивание (ZIP/.torrent) |
| `POST /api/games/{id}/seed` | USER, ADMIN | Запуск раздачи через Transmission |
| `GET /api/downloads/active` | USER, ADMIN | Активные раздачи Transmission |
| `GET /api/tracker/announce` | все | HTTP-трекер BitTorrent |
| `GET /api/profile` | USER, ADMIN | Профиль |
| `POST /api/scan` | ADMIN | Сканирование библиотеки |
| `GET /api/admin/users` | ADMIN | Список пользователей |
| `POST /api/admin/users/{id}/toggle-admin` | ADMIN | Смена роли |
| `POST /api/admin/users/{id}/toggle-active` | ADMIN | Блокировка/разблокировка пользователя |
| `POST /api/admin/users/{id}/reset-password` | ADMIN | Сброс пароля |
| `GET /api/admin/scraper-config` | ADMIN | Конфигурации scraper-ов |
| `POST /api/admin/scraper-config` | ADMIN | Сохранение конфигураций scraper-ов |
| `POST /api/admin/scraper-config/reload` | ADMIN | Перезагрузка scraper-ов из файла |
| `POST /api/admin/migrate-images` | ADMIN | Миграция изображений из БД в ФС |

Swagger UI: `/game-library/swagger-ui.html`

## Структура файлового хранилища

```
<games_directory>/games/
└── <platform>/                       (например: PC, PlayStation, Xbox)
    └── <game_name>/
        ├── <файлы игры>...
        └── information/              (создаётся при сканировании)
            ├── logo.jpg              (постер)
            ├── information.json      (название, год, жанры, описание, трейлер, инструкция)
            └── img/                  (скриншоты .jpg)
```

## Функциональные возможности

### Сканирование библиотеки (ADMIN)
- Обход структуры `<games>/<platform>/<game_name>/`
- Создание `information/` при отсутствии
- Автоматическое добавление/удаление записей в БД

### Просмотр и фильтрация
- Сетка игр с постерами
- Фильтрация: платформа, жанр, год, поиск по названию
- Сортировка: название, год, дата добавления
- Пагинация (12 игр на страницу)

### Редактирование игр (ADMIN)
- Название, год, платформа, описание (Quill-редактор), инструкция, трейлер
- Жанры из общего справочника
- Сбор метаданных со scraper-ов (6 активных: Playground.ru / Igromania.ru / World-Art / Steam / IGDB / TheGamesDB)

### Скачивание игр
- **Малые игры (<5 ГБ)**: потоковая упаковка в ZIP
- **Крупные игры (≥5 ГБ)**: создание .torrent с announce URL встроенного HTTP-трекера (`/api/tracker/announce`) + раздача через Transmission
- **Seed via Transmission**: кнопка на странице игры — создаёт торрент и запускает сидирование на NAS

### Пользователи и роли
- **ADMIN**: сканирование, редактирование, scraper, управление пользователями
- **USER**: просмотр, скачивание, профиль

### Локализация
Русский и английский языки. Переключение через `?lang=ru` / `?lang=en`.

## Запуск

### Полный стек (Docker)

```bash
# Сборка и запуск всех сервисов
make all

# Или пошагово:
make build-backend    # mvn clean package -DskipTests
make build-frontend   # cd frontend && npm run build
make up               # docker-compose up --build -d
```

Сервисы:

| Порт | Сервис | Назначение |
|------|--------|-----------|
| `:80` | frontend (Nginx) | Vue SPA + прокси на API |
| `:8080` | backend | Spring Boot REST API + встроенный HTTP-трекер |
| `:9091` | transmission | RPC-управление |
| `:51413` | transmission | P2P-трафик (TCP/UDP) |
| `:5432` | postgresdb | PostgreSQL |

### Локальная разработка

```bash
# Backend (нужен PostgreSQL + Transmission локально)
mvn spring-boot:run -Dspring.profiles.active=alone

# Frontend (Vite dev server с прокси на localhost:8080)
cd frontend && npm run dev
```

Профиль `alone` (`application-alone.yml`) переопределяет пути к играм, трекеру и изображениям для локальной разработки.

Makefile также содержит вспомогательные цели:
```bash
make dev-backend    # mvn spring-boot:run
make dev-frontend   # cd frontend && npm run dev
make logs           # docker-compose logs -f
make clean          # docker-compose down -v && mvn clean && rm -rf frontend/dist
```

## Миграция изображений из БД в ФС

Изображения (логотипы, скриншоты, аватарки) хранятся в БД как `bytea`.
Для переноса на файловую систему:

```bash
# Вариант 1: bash-скрипт
./scripts/migrate-images.sh /gameLibrary/images

# Вариант 2: через REST API (ADMIN)
curl -X POST http://localhost:8080/game-library/api/admin/migrate-images \
  -H "Authorization: Bearer <token>"
```

После миграции изображения будут отдаваться напрямую с диска через `/api/images/**`
с автоматическим fallback на БД, если файл не найден.

## База данных

Таблицы в схеме `library`:

| Таблица | Назначение |
|---------|-----------|
| `game_data` | Игры (id, name, platform, release_date, description, instruction, trailer_url, logo, directory_path) |
| `game_genre` | Справочник жанров (code, description, description_ru) — ~70 жанров |
| `game_data_genre` | M:N связь игры и жанров |
| `game_screenshot` | Скриншоты (source bytea) |
| `library_user` | Пользователи (user_name, pass BCrypt, is_admin, is_active, avatar bytea) |

Сборка PostgreSQL-контейнера: `postgresdb/Dockerfile` (база `postgres:16.14`).

DDL выполняются в порядке сортировки: `postgresdb/ddl/1_init.sql` (пользователь + БД + схема), `2_library.sql` (таблицы игр + ~70 жанров), `3_user.sql` (таблица пользователей + admin/guest seed).

Управление БД через скрипты в `postgresdb/`: `01_build.sh`, `02_up.sh`, `03_build-and-up.sh`, `04_kill.sh`, `05_stop.sh`, `06_recreate.sh`.

## Окружение

Переменные среды (с значениями по умолчанию):

| Переменная | По умолчанию | Описание |
|-----------|-------------|----------|
| `SERVER_PORT` | `8080` | Порт backend |
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `GAMES_DIRECTORY` | `/gameLibrary` | Корень с играми |
| `IMAGES_DIRECTORY` | `/gameLibrary/images` | Изображения на ФС |
| `TRACKER_ANNOUNCE_URL` | `http://localhost:8080/game-library/api/tracker/announce` | URL announce для .torrent (должен быть доступен клиентам) |
| `TRANSMISSION_RPC_URL` | `http://transmission:9091/transmission/rpc` | RPC Transmission |
| `TRANSMISSION_DOWNLOAD_DIR` | `/downloads` | Директория с играми (в контейнере Transmission) |
| `JWT_SECRET` | (ключ по умолчанию) | Секрет JWT |
| `JWT_EXPIRATION_MS` | `86400000` | Время жизни токена (24ч) |
| `SCRAPER_CONFIG_DIR` | `/gameLibrary/gameLibraryConfigs/scrapers` | Директория с `scrapers-config.json` |
| `SCRAPER_ENCRYPTION_KEY` | (не задан) | Ключ AES-256 (base64) для шифрования API-ключей скраперов. **Обязателен для сохранения ключей после перезапуска**. Сгенерировать: `openssl rand -base64 32` |
| `TORRENT_DIR_TMP` | `/torrentDirTmp` | Временная директория для .torrent файлов |
| `TTORRENT_HASHING_THREADS` | `2` | Количество потоков для хеширования торрент-файлов (ускорение создания .torrent) |

## Scraper-ы

Конфигурация scraper-ов хранится в `scrapers/scrapers-config.json` и управляется через `/api/admin/scraper-config`.

| Scraper | Статус | Метод | Описание |
|---------|--------|-------|----------|
| Playground (playground.ru) | ✅ Активен | CSS-селекторы | Сбор названия, описания, жанров, скриншотов |
| Igromania (igromania.ru) | ✅ Активен | JSON Path | Сбор через props initialStoreState |
| Steam (store.steampowered.com) | ✅ Активен | Storefront API | Steam Storefront, без API-ключа |
| IGDB (api.igdb.com) | ✅ Активен | REST API + Bearer | Требуется Client-ID + Bearer-токен (Twitch OAuth) |
| TheGamesDB (api.thegamesdb.net) | ✅ Активен | REST API | Требуется API-ключ (регистрация на thegamesdb.net) |
| World-Art (world-art.ru) | ✅ Активен | CSS-селекторы | Прямой парсинг карточки + поиск |

### Настройка IGDB scraper

IGDB использует авторизацию Twitch. Для работы потребуются **Client-ID** и **Access Token**:

1. Зайди на https://dev.twitch.tv/console/apps/create
2. Создай приложение (Name — любое, OAuth Redirect URLs — `http://localhost`, Category — любой)
3. После создания скопируй **Client-ID** из списка приложений
4. Там же нажми **New Secret** → скопируй **Client Secret**
5. Получи access token:

   ```bash
   curl -X POST "https://id.twitch.tv/oauth2/token?client_id=ВАШ_CLIENT_ID&client_secret=ВАШ_SECRET&grant_type=client_credentials"
   ```

6. Из JSON-ответа скопируй значение поля `access_token`.

Куда прописать:
- **Client-ID** → в админке (`/admin/scrapers`) → IGDB → поле `headers.Client-ID`
- **access_token** → там же → поле `encryptedApiKey` (админка зашифрует сама)

### Настройка TheGamesDB scraper

TheGamesDB требует API-ключ. Получить его:

1. Зарегистрируйся на https://thegamesdb.net/register.php
2. Подтверди email, войди на сайт
3. Перейди на https://api.thegamesdb.net/key.php — ключ будет отображён
4. Скопируй ключ

Куда прописать:
- **API-ключ** → в админке (`/admin/scrapers`) → TheGamesDB → поле `encryptedApiKey` (админка зашифрует сама)

**Лимит:** 1000 запросов в месяц. Один поиск игры может использовать 2-3 запроса (поиск + жанры + скриншоты).

Подробнее о подводных камнях — см. `AGENTS.md`.

## Развёртывание на Linux

### Требования

| Компонент | Версия | Установка |
|-----------|--------|-----------|
| Java | 11 (JDK) | `apt install openjdk-11-jdk` / `sdk install java 11.0.26-tem` |
| Maven | 3.6+ | `apt install maven` |
| Node.js | 18+ | `apt install nodejs npm` / `nvm install 18` |
| Docker | 19.03+ | `apt install docker.io docker-compose-v2` |
| PostgreSQL | 12+ | `apt install postgresql` (только для локальной разработки без Docker) |

### Конфигурация

Перед сборкой отредактируйте `application.yml`:

```yaml
game-library:
  games:
    directory: /mnt/nas/gameLibrary     # путь к вашей библиотеке игр
  images:
    directory: /mnt/nas/gameLibrary/images
  tracker:
    announce-base-url: http://192.168.1.100:8080/game-library/api/tracker/announce  # IP вашего NAS

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/game-library?currentSchema=library
    username: library-manager-user
    password: 2wq345tgfiNcbBBwee3
```

### Структура директорий для Transmission

При запуске через Docker контейнер Transmission использует bind mounts для доступа к файлам игр, настройкам и служебным каталогам. Перед запуском создайте на хосте следующую структуру:

```
nas/gameLibrary/                  # корень библиотеки
├── games/                        # игровые файлы (PC, PS3, ...)
├── images/                       # скриншоты и обложки
└── gameLibraryConfigs/           # конфигурационные данные
    ├── db/                       # данные PostgreSQL (Docker volume)
    └── tracker/                  # всё, что связано с Transmission
        ├── config/               # settings.json, queue.json (создаётся автоматически)
        ├── watch/                # автодобавление .torrent файлов
        ├── complete/             # завершённые загрузки
        ├── incomplete/           # незавершённые загрузки
        └── torrents/             # .torrent файлы, созданные backend
```

**Создание директорий (Linux):**
```bash
mkdir -p /mnt/nas/gameLibrary/games
mkdir -p /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/{config,watch,complete,incomplete,torrents}
```

**Создание директорий (Windows):**
```cmd
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\config
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\watch
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\complete
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\incomplete
```

Обратите внимание: `PUID` / `PGID` в `docker-compose.yml` должны совпадать с UID/GID владельца файлов игр. На WSL/Windows файлы будут доступны, даже если `chown` в контейнере выдаёт предупреждение.

### Настройка Transmission

Файл `gameLibraryConfigs/tracker/config/settings.json` создаётся автоматически при первом запуске контейнера. Важные параметры задаются через переменные окружения в `docker-compose.yml` (секция `environment` сервиса `transmission`):

| Переменная | Значение | Параметр settings.json |
|---|---|---|
| `TRANSMISSION_DOWNLOAD_DIR` | `/downloads/complete` | `download-dir` |
| `TRANSMISSION_INCOMPLETE_DIR` | `/downloads/incomplete` | `incomplete-dir` |
| `TRANSMISSION_INCOMPLETE_DIR_ENABLED` | `true` | `incomplete-dir-enabled` |
| `TRANSMISSION_WATCH_DIR` | `/watch` | `watch-dir` |
| `TRANSMISSION_WATCH_DIR_ENABLED` | `true` | `watch-dir-enabled` |

Если нужно изменить другие параметры Transmission (RPC-порт, лимиты скорости и т.д.) — отредактируйте `settings.json` на хосте и перезапустите контейнер.

### Сборка и запуск (Docker)

```bash
# 1. Клонировать репозиторий
git clone <url> /opt/game-library
cd /opt/game-library

# 2. Собрать backend (JAR)
mvn clean package -DskipTests

# 3. Собрать frontend (Vue SPA)
cd frontend
npm install
npm run build
cd ..

# 4. Создать на хосте структуру директорий для Transmission:
#    mkdir -p /mnt/nas/gameLibrary/games
#    mkdir -p /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/{config,watch,complete,incomplete,torrents}
#
#    Подробнее — в разделе «Структура директорий для Transmission» ниже.

# 5. Настроить volume paths в docker-compose.yml
#    Отредактируйте раздел volumes для сервисов backend и transmission:
#
#    backend:
#      volumes:
#        - /mnt/nas/gameLibrary:/gameLibrary            # корень с games/, images/ и т.д.
#        - /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/torrents:/torrentDirTmp
#
#    transmission:
#      volumes:
#        - /mnt/nas/gameLibrary/games:/downloads/games         # игровые файлы для сидирования
#        - /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/config:/config         # настройки Transmission
#        - /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/watch:/watch           # автодобавление торрентов
#        - /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/complete:/downloads/complete
#        - /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/incomplete:/downloads/incomplete

# 6. Запустить все сервисы
docker compose up --build -d

# 7. Проверить:
#    - Frontend: http://localhost
#    - Swagger UI: http://localhost/game-library/swagger-ui.html
#    - Transmission Web UI: http://localhost:9091
#    - API: http://localhost:8080/game-library/api/games
```

### Сборка и запуск (без Docker)

```bash
# 1. База данных
sudo -u postgres psql -f postgresdb/ddl/1_init.sql
sudo -u postgres psql -f postgresdb/ddl/2_library.sql
sudo -u postgres psql -f postgresdb/ddl/3_user.sql

# 2. Transmission (в отдельном терминале)
docker run -d --name transmission \
  -p 9091:9091 -p 51413:51413 -p 51413:51413/udp \
  -v /mnt/nas/gameLibrary/games:/downloads/games \
  -v /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/config:/config \
  -v /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/watch:/watch \
  -v /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/complete:/downloads/complete \
  -v /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/incomplete:/downloads/incomplete \
  -e PUID=$(id -u) -e PGID=$(id -g) \
  lscr.io/linuxserver/transmission

# 3. Backend
mvn spring-boot:run

# 4. Frontend (в отдельном терминале)
cd frontend && npm run dev
# Frontend будет на http://localhost:5173, API проксируется на :8080
```

## Развёртывание на Windows

### Требования

| Компонент | Версия | Ссылка |
|-----------|--------|--------|
| Java | 11 (JDK) | [Adoptium Temurin 11](https://adoptium.net/temurin/releases/?version=11) |
| Maven | 3.6+ | [Maven Download](https://maven.apache.org/download.cgi) |
| Node.js | 18+ | [Node.js 18 LTS](https://nodejs.org/) |
| Docker Desktop | последняя | [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/) |
| PostgreSQL | 12+ | [PostgreSQL Download](https://www.postgresql.org/download/windows/) |
| Git | любая | [Git for Windows](https://git-scm.com/download/win) |

**Важно**: Git, Java, Maven, Node.js должны быть прописаны в `PATH`. Проверьте:

```cmd
java -version
mvn -version
node --version
npm --version
```

### Конфигурация

Перед сборкой отредактируйте `application.yml` (Windows-пути с обратным слешем или `/`):

```yaml
game-library:
  games:
    directory: D:/GameLibrary              # ваша библиотека игр
  images:
    directory: D:/GameLibrary/images
  tracker:
    announce-base-url: http://192.168.1.100:8080/game-library/api/tracker/announce

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/game-library?currentSchema=library
    username: library-manager-user
    password: 2wq345tgfiNcbBBwee3
```

### Сборка и запуск (Docker Desktop)

```powershell
# 1. Клонировать репозиторий
git clone <url> C:\Projects\GameLibrary
cd C:\Projects\GameLibrary

# 2. Собрать backend (JAR)
mvn clean package -DskipTests

# 3. Собрать frontend (Vue SPA)
cd frontend
npm install
npm run build
cd ..

# 4. Создать на хосте структуру директорий:
#    mkdir D:\GameLibrary\gameLibraryConfigs\tracker\config
#    mkdir D:\GameLibrary\gameLibraryConfigs\tracker\watch
#    mkdir D:\GameLibrary\gameLibraryConfigs\tracker\complete
#    mkdir D:\GameLibrary\gameLibraryConfigs\tracker\incomplete
#    # games/ и gameLibraryConfigs/tracker/torrents/ уже должны быть

# 5. Настроить volume paths в docker-compose.yml
#    Замените Linux-пути на Windows:
#
#    backend:
#      volumes:
#        - D:/GameLibrary:/gameLibrary
#        - D:/GameLibrary/gameLibraryConfigs/tracker/torrents:/torrentDirTmp
#
#    transmission:
#      volumes:
#        - D:/GameLibrary/games:/downloads/games
#        - D:/GameLibrary/gameLibraryConfigs/tracker/config:/config
#        - D:/GameLibrary/gameLibraryConfigs/tracker/watch:/watch
#        - D:/GameLibrary/gameLibraryConfigs/tracker/complete:/downloads/complete
#        - D:/GameLibrary/gameLibraryConfigs/tracker/incomplete:/downloads/incomplete

# 6. Запустить все сервисы
docker compose up --build -d
```

### Сборка и запуск (без Docker)

#### 1. PostgreSQL

Установите PostgreSQL, создайте БД через pgAdmin или psql:

```powershell
psql -U postgres -f postgresdb\ddl\1_init.sql
psql -U postgres -f postgresdb\ddl\2_library.sql
psql -U postgres -f postgresdb\ddl\3_user.sql
```

#### 2. Transmission

Установите [Transmission для Windows](https://transmissionbt.com/download/) или запустите через Docker Desktop:

```powershell
docker run -d --name transmission \
  -p 9091:9091 -p 51413:51413 -p 51413:51413/udp \
  -v D:\GameLibrary\games:/downloads/games \
  -v D:\GameLibrary\gameLibraryConfigs\tracker\config:/config \
  -v D:\GameLibrary\gameLibraryConfigs\tracker\watch:/watch \
  -v D:\GameLibrary\gameLibraryConfigs\tracker\complete:/downloads/complete \
  -v D:\GameLibrary\gameLibraryConfigs\tracker\incomplete:/downloads/incomplete \
  lscr.io/linuxserver/transmission
```

#### 3. Backend

```powershell
mvn spring-boot:run
```

API будет доступен на `http://localhost:8080/game-library/api/`.

#### 4. Frontend (Vite dev server)

```powershell
cd frontend
npm install
npm run dev
```

Frontend откроется на `http://localhost:5173`, API проксируется на `:8080`.

### Настройка путей в Windows

При использовании Docker Desktop volumes могут монтироваться только из директорий, shared в настройках Docker Desktop:
- Settings → Resources → File Sharing
- Добавьте `D:\` или папку с играми (`D:\GameLibrary`)

Для локального запуска без Docker используйте прямые Windows-пути в `application.yml`.
Файл `information.json` должен содержать пути с `/` (Unix-style), так как Java
нормализует их автоматически на любой платформе.

## Типовые проблемы при развёртывании

### Frontend не грузится, пустая страница
- Проверьте nginx.conf: location `/game-library` должен проксироваться на backend.
- При локальной разработке Vite dev server должен быть на `:5173`, а backend на `:8080`.
- Откройте F12 (Console/Network) — смотрите ошибки загрузки.

### Backend не видит PostgreSQL
- Проверьте, что PostgreSQL запущен и доступен (в Docker: `docker compose logs postgresdb`).
- Проверьте `DB_HOST` и `DB_PORT`: при запуске в Docker имя сервиса `postgresdb`, при локальном — `localhost`.
- Проверьте пароль: `2wq345tgfiNcbBBwee3` для пользователя `library-manager-user`.

### Transmission не отвечает
- При запуске в Docker: `docker compose logs transmission` — проверьте логи.
- Проверьте `TRANSMISSION_RPC_URL`: в Docker `http://transmission:9091/transmission/rpc`, локально `http://localhost:9091/transmission/rpc`.
- Проверьте, что Transmission запущен: `curl -X GET http://localhost:9091/transmission/rpc` — должен вернуть заголовок `X-Transmission-Session-Id`.
- Убедитесь, что `PUID`/`PGID` в docker-compose.yml соответствуют владельцу файлов игр.
- Если в логах ошибка `Couldn't bind to [::]:9091` — Transmission пытается слушать IPv6, но Docker Desktop на Windows/WSL не поддерживает его. Отредактируйте `gameLibraryConfigs/tracker/config/settings.json` на хосте, замените `"rpc-bind-address": "[::]"` на `"rpc-bind-address": "0.0.0.0"` и перезапустите контейнер.

### Изображения не отображаются
- После миграции изображения хранятся на диске, путь должен совпадать с `IMAGES_DIRECTORY`.
- Если файл отсутствует — будет fallback на БД (работает медленнее).
- Запустите миграцию: `POST /api/admin/migrate-images` или `scripts/migrate-images.sh`.

### Transmission сидирует, но торрент-клиенты не могут скачать (uTorrent и др.)

Трекер работает (логи backend показывают `returning complete=1`), uTorrent видит сида, но данные не передаются.

**Решение:** включите uTP в настройках Transmission. Отредактируйте `gameLibraryConfigs/tracker/config/settings.json` на хосте:

```json
{
    "preferred_transports": ["utp", "tcp"],
    "utp-enabled": true
}
```

Затем перезапустите контейнер:

```bash
docker-compose restart transmission
```

**Важно:** в Transmission 4.x ключ `utp-enabled` считается устаревшим. Основной способ управления — `preferred_transports`. Если в нём нет `"utp"`, то uTP будет выключен, даже если `utp-enabled: true`. Убедитесь, что указаны оба ключа.

Переменная окружения `TRANSMISSION_UTP_ENABLED=true` в `docker-compose.yml` **не работает** — init-скрипт контейнера linuxserver/transmission не обрабатывает `TRANSMISSION_*` переменные (поддерживаются только `USER`, `PASS`, `WHITELIST`, `HOST_WHITELIST`, `PEERPORT`, `UMASK`). Настройки необходимо менять напрямую в `settings.json` на хосте — они сохранятся после перезапусков.

### Ошибка "no suitable method found for create"
- OkHttp 3.x: `RequestBody.create(MediaType, String)` — сначала MediaType, потом String.
- Если вы обновили OkHttp до 4.x, API изменился. Проект использует OkHttp 3.14.9.
