# GameLibrary

<p align="center">
  <img src="https://img.shields.io/badge/Java-11-blue?logo=openjdk">
  <img src="https://img.shields.io/badge/Spring_Boot-2.7.1-brightgreen?logo=spring">
  <img src="https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs">
  <img src="https://img.shields.io/badge/PostgreSQL-16-316192?logo=postgresql">
  <img src="https://img.shields.io/badge/Docker-✅-2496ED?logo=docker">
</p>

<p align="center">
  <b>EN:</b> Game catalog manager for NAS — scan filesystem, fetch metadata from 6 scrapers, browse & download via P2P torrents.
  <br>
  <b>RU:</b> Каталогизатор компьютерных игр для NAS — сканирование ФС, сбор метаданных из 6 скраперов, просмотр и скачивание через P2P-торренты.
</p>

---

<p align="center">
  <a href="#-features">Features</a> &nbsp;•&nbsp;
  <a href="#-quick-start">Quick Start</a> &nbsp;•&nbsp;
  <a href="#-tech-stack">Tech Stack</a> &nbsp;•&nbsp;
  <a href="#-architecture">Architecture</a> &nbsp;•&nbsp;
  <a href="#-configuration">Configuration</a> &nbsp;•&nbsp;
  <a href="#-scrapers">Scrapers</a> &nbsp;•&nbsp;
  <a href="#-deployment">Deployment</a> &nbsp;•&nbsp;
  <a href="#-troubleshooting">Troubleshooting</a>
</p>

<p align="center">
  <a href="#en">🇬🇧 English</a> &nbsp;|&nbsp; <a href="#ru">🇷🇺 Русский</a>
</p>

---

<a name="en"></a>

# English

## ✨ Features

| For users | For admins |
|-----------|-----------|
| Game grid with posters & filters | Filesystem scanning & auto-indexing |
| Search by name, platform, genre, year | Metadata scraping (6 scrapers) |
| Sorting & pagination | Game editor with Quill rich text |
| ZIP download (<5 GB) / .torrent download (≥5 GB) | User management (roles, block, reset password) |
| P2P seeding via Transmission | Scraper config panel (API keys, enable/disable) |
| Profile, avatar, password change | Image migration DB→FS |
| Russian / English UI | |

## ⚡ Quick Start

```bash
make all   # builds backend + frontend, starts docker-compose
```

Opens at `http://localhost` — login as `admin` / `admin`.

## 📦 Tech Stack

| Component | Technology |
|-----------|-----------|
| Backend | Spring Boot 2.7.1, Java 11 |
| Frontend | Vue 3 + Vite 5, PrimeVue 4, Pinia, VueQuill (Quill 2) |
| Database | PostgreSQL 16 (schema `library`) |
| ORM / JDBC | Hibernate 5.6.15, Spring Data JPA, Commons DBCP 1.4 |
| REST API | Spring MVC `@RestController`, JWT auth (form login fallback) |
| API Docs | OpenAPI / Swagger UI at `/game-library/swagger-ui.html` |
| Downloads | ZIP streaming + BitTorrent via Transmission (JSON-RPC) |
| P2P Tracker | Built-in HTTP tracker at `/api/tracker/announce` |
| Scraping | OkHttp 3, Jsoup, Steam Storefront API, Twitch OAuth (IGDB) |
| Build | Maven (JAR) + npm / Vite |
| Containerization | Docker, docker-compose (4 services) |

## 🏗 Architecture

```
┌─────────┐   :80   ┌──────────┐   :8080  ┌──────────────────┐
│ Browser │ ──────▶ │  Nginx   │ ──────▶  │   Backend        │
└─────────┘         │ (Vue SPA)│          │  (REST API)      │
                    └──────────┘          │  + Tracker       │
                                          └───┬────────┬─────┘
                                              │        ▲
                                              │        │ announce
                                              │  ┌─────┴──────────┐
                                              │  │  Transmission  │ :9091 RPC
                                              │  │  (seeder)      │ :51413 P2P
                                              │  └──────┬─────────┘
                                              │         │
                     ┌──────────────┐         │
                     │  PostgreSQL  │  :5432  │
                     └──────────────┘         │
                     User torrent clients
                     (qBittorrent, Transmission, etc.)
                         │              ▲
                         └───── P2P ────┘
```

### Frontend Routes

| URL | Access | Description |
|-----|--------|-------------|
| `/login` | all | Login form |
| `/register` | all | Registration |
| `/` | USER, ADMIN | Library grid — filters, sorting, pagination |
| `/game/:id` | USER, ADMIN | Game detail page |
| `/game/:id/edit` | ADMIN | Editor + scraping panel |
| `/profile` | USER | Profile, avatar, password change |
| `/admin/users` | ADMIN | User management |
| `/admin/scrapers` | ADMIN | Scraper config (API keys, on/off) |
| `/downloads` | USER, ADMIN | Transmission seeding status |

### API Endpoints

All under `/game-library/api/`. Auth: JWT Bearer token.

| Endpoint | Access | Description |
|----------|--------|-------------|
| `POST /auth/login` | all | Login → JWT |
| `POST /auth/register` | all | Register new user |
| `GET /auth/me` | USER, ADMIN | Current user info |
| `GET /games` | USER, ADMIN | Game list (filters, sort, page) |
| `GET /games/{id}` | USER, ADMIN | Game details |
| `POST /games/{id}/edit` | ADMIN | Save game edits |
| `POST /games/{id}/grab` | ADMIN | Scrape metadata |
| `GET /games/{id}/download` | USER, ADMIN | Download ZIP / .torrent |
| `POST /games/{id}/seed` | USER, ADMIN | Start seeding via Transmission |
| `GET /downloads/active` | USER, ADMIN | Active Transmission seeds |
| `GET /tracker/announce` | all | BitTorrent HTTP tracker announce |
| `GET /profile` | USER, ADMIN | Profile data |
| `POST /profile/update` | USER | Update profile |
| `POST /profile/password` | USER | Change password |
| `POST /scan` | ADMIN | Start library scan |
| `GET /scan/status` | ADMIN | Scan progress |
| `GET /admin/users` | ADMIN | List users |
| `POST /admin/users/{id}/toggle-admin` | ADMIN | Toggle admin role |
| `POST /admin/users/{id}/toggle-active` | ADMIN | Block / unblock |
| `POST /admin/users/{id}/reset-password` | ADMIN | Reset password |
| `GET /admin/scraper-config` | ADMIN | Get scraper configs |
| `POST /admin/scraper-config` | ADMIN | Save scraper configs |
| `POST /admin/scraper-config/reload` | ADMIN | Reload from file |
| `POST /admin/migrate-images` | ADMIN | Migrate images DB → FS |

## 🔧 Configuration

### Filesystem Layout

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

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Backend port |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `GAMES_DIRECTORY` | `/gameLibrary` | Game files root |
| `IMAGES_DIRECTORY` | `/gameLibrary/images` | Images on filesystem |
| `TRACKER_ANNOUNCE_URL` | `http://localhost:8080/game-library/api/tracker/announce` | Announce URL in .torrent files (must be reachable by clients) |
| `TRANSMISSION_RPC_URL` | `http://transmission:9091/transmission/rpc` | Transmission RPC endpoint |
| `TRANSMISSION_DOWNLOAD_DIR` | `/downloads` | Download dir in Transmission container |
| `JWT_SECRET` | default key | JWT signing secret |
| `JWT_EXPIRATION_MS` | `86400000` | Token TTL (24 hours) |
| `SCRAPER_CONFIG_DIR` | `/gameLibrary/gameLibraryConfigs/scrapers` | Directory with `scrapers-config.json` |
| `SCRAPER_ENCRYPTION_KEY` | not set | AES-256 base64 key for scraper API key encryption. **Required to persist encrypted keys.** Generate: `openssl rand -base64 32` |
| `TORRENT_DIR_TMP` | `/torrentDirTmp` | Temp directory for .torrent files |
| `TTORRENT_HASHING_THREADS` | `2` | Threads for torrent hashing (lower on low-CPU NAS) |

### Database Schema

Schema `library`:

| Table | Purpose |
|-------|---------|
| `game_data` | Games (id, name, platform, release_date, description, instruction, trailer_url, logo, directory_path) |
| `game_genre` | Genre dictionary (code, description, description_ru) — ~70 genres |
| `game_data_genre` | M:N game ↔ genre |
| `game_screenshot` | Screenshots (bytea) |
| `library_user` | Users (user_name, pass BCrypt, is_admin, is_active, avatar bytea) |

DDL: `postgresdb/ddl/` — `1_init.sql` (schema), `2_library.sql` (tables + genres), `3_user.sql` (users + seed).

### Image Migration (DB → Filesystem)

By default images are stored as `bytea` in DB. To migrate to disk:

```bash
# Script
./scripts/migrate-images.sh /gameLibrary/images

# Or via API (ADMIN)
curl -X POST http://localhost:8080/game-library/api/admin/migrate-images \
  -H "Authorization: Bearer <token>"
```

After migration images are served from disk with automatic DB fallback if the file is missing.

## 🕷 Scrapers

All config stored in `scrapers/scrapers-config.json`, managed via `/api/admin/scraper-config`.

| Scraper | Method | Auth | What it scrapes |
|---------|--------|------|----------------|
| **Playground** (playground.ru) | CSS selectors | — | Name, description, genres, screenshots |
| **Igromania** (igromania.ru) | JSON Path | — | Game data via `initialStoreState` |
| **Steam** (store.steampowered.com) | Storefront API | — | Name, description, screenshots, genres |
| **IGDB** (api.igdb.com) | REST API | Twitch OAuth 2.0 (Client-ID + Bearer) | Full metadata |
| **TheGamesDB** (api.thegamesdb.net) | REST API | API key | Full metadata (limit: 1000 req/month) |
| **World-Art** (world-art.ru) | CSS selectors | — | Card parsing + search |

### IGDB Setup

1. Create app at https://dev.twitch.tv/console/apps/create
2. Copy **Client-ID** and generate **Client Secret**
3. Get access token:
   ```bash
   curl -X POST "https://id.twitch.tv/oauth2/token?client_id=YOUR_ID&client_secret=YOUR_SECRET&grant_type=client_credentials"
   ```
4. In admin panel (`/admin/scrapers`) → IGDB → set `headers.Client-ID` and `encryptedApiKey`

### TheGamesDB Setup

1. Register at https://thegamesdb.net/register.php
2. Get key at https://api.thegamesdb.net/key.php
3. In admin panel → TheGamesDB → set `encryptedApiKey`

## 🚀 Deployment

### Prerequisites

| Component | Version |
|-----------|---------|
| Java | 11 (JDK) |
| Maven | 3.6+ |
| Node.js | 18+ |
| Docker | 19.03+ (with compose) |
| PostgreSQL | 12+ (local dev only) |

### Docker (recommended)

#### Directory structure

Create on host before launching:

```
/mnt/nas/gameLibrary/              # library root
├── games/                         # game files (PC, PS3, ...)
├── images/                        # screenshots and covers
└── gameLibraryConfigs/
    ├── db/                        # PostgreSQL data (Docker volume)
    └── tracker/                   # Transmission-related
        ├── config/                # settings.json (auto-created)
        ├── watch/                 # auto-add .torrent
        ├── complete/              # completed downloads
        ├── incomplete/            # incomplete downloads
        └── torrents/              # .torrent files from backend
```

```bash
mkdir -p /mnt/nas/gameLibrary/games
mkdir -p /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/{config,watch,complete,incomplete,torrents}
```

#### Quick start

```bash
make all
```

Or step by step:
```bash
mvn clean package -DskipTests                # backend
cd frontend && npm install && npm run build   # frontend
docker compose up --build -d                  # start all services
```

| Port | Service | Purpose |
|------|---------|---------|
| `:80` | Nginx | Vue SPA + API proxy |
| `:8080` | Backend | REST API + HTTP tracker |
| `:9091` | Transmission | RPC web UI |
| `:51413` | Transmission | P2P traffic (TCP/UDP) |
| `:5432` | PostgreSQL | Database |

#### Transmission config

Edit `gameLibraryConfigs/tracker/config/settings.json` on host for:
- uTP (`preferred_transports: ["utp", "tcp"]`) — **required** for uTorrent compatibility
- RPC bind address (`"rpc-bind-address": "0.0.0.0"` for Windows/WSL)
- Any other Transmission tuning

⛔ `TRANSMISSION_UTP_ENABLED=true` in docker-compose.yml has **no effect** — the container init script doesn't process it. Edit `settings.json` directly.

### Local Development

```bash
# Backend (requires local PostgreSQL + Transmission)
mvn spring-boot:run -Dspring.profiles.active=alone

# Frontend (Vite dev server, proxies /game-library/* to :8080)
cd frontend && npm run dev
```

Makefile helpers:
```bash
make dev-backend    # mvn spring-boot:run
make dev-frontend   # cd frontend && npm run dev
make logs           # docker-compose logs -f
make clean          # docker-compose down -v && mvn clean && rm -rf frontend/dist
```

### Linux (without Docker)

```bash
# 1. Database
sudo -u postgres psql -f postgresdb/ddl/1_init.sql
sudo -u postgres psql -f postgresdb/ddl/2_library.sql
sudo -u postgres psql -f postgresdb/ddl/3_user.sql

# 2. Transmission
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

# 4. Frontend
cd frontend && npm run dev
```

### Windows (without Docker)

```powershell
# 1. Database
psql -U postgres -f postgresdb\ddl\1_init.sql
psql -U postgres -f postgresdb\ddl\2_library.sql
psql -U postgres -f postgresdb\ddl\3_user.sql

# 2. Transmission (via Docker Desktop)
docker run -d --name transmission `
  -p 9091:9091 -p 51413:51413 -p 51413:51413/udp `
  -v D:\GameLibrary\games:/downloads/games `
  -v D:\GameLibrary\gameLibraryConfigs\tracker\config:/config `
  -v D:\GameLibrary\gameLibraryConfigs\tracker\watch:/watch `
  -v D:\GameLibrary\gameLibraryConfigs\tracker\complete:/downloads/complete `
  -v D:\GameLibrary\gameLibraryConfigs\tracker\incomplete:/downloads/incomplete `
  lscr.io/linuxserver/transmission

# 3. Backend
mvn spring-boot:run

# 4. Frontend
cd frontend
npm install
npm run dev
```

**Windows path notes:** Docker Desktop volumes must be shared in Settings → Resources → File Sharing. Use forward slashes in config files (`D:/GameLibrary`).

## 🔍 Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Blank page | nginx proxy misconfig | Check nginx.conf `/game-library` location |
| Backend can't connect to DB | Wrong host/port/password | Check `DB_HOST`, `DB_PORT`, verify PostgreSQL is running |
| Transmission not responding | Wrong RPC URL or IPv6 bind | Set `TRANSMISSION_RPC_URL`; change `rpc-bind-address` to `0.0.0.0` |
| Images not showing | Missing files after migration | Run migration again: `POST /api/admin/migrate-images` |
| Tracker works but no data transfer | uTP disabled | Set `preferred_transports: ["utp", "tcp"]` in Transmission `settings.json` |
| `no suitable method found for create` | OkHttp version mismatch | Use `RequestBody.create(MediaType, String)` (3.x API) |

---

<a name="ru"></a>

# Русский

## ✨ Возможности

| Для пользователей | Для администраторов |
|------------------|-------------------|
| Сетка игр с постерами и фильтрами | Сканирование ФС и авто-индексация |
| Поиск по названию, платформе, жанру, году | Сбор метаданных (6 скраперов) |
| Сортировка и пагинация | Редактор игр с Quill (rich text) |
| Скачивание ZIP (<5 ГБ) / .torrent (≥5 ГБ) | Управление пользователями |
| P2P-раздача через Transmission | Панель конфигурации скраперов |
| Профиль, аватар, смена пароля | Миграция изображений БД→ФС |
| Русский / английский интерфейс | |

## ⚡ Быстрый старт

```bash
make all   # сборка backend + frontend, запуск docker-compose
```

Открыть `http://localhost` — войти как `admin` / `admin`.

## 📦 Технологический стек

| Компонент | Технология |
|-----------|-----------|
| Backend | Spring Boot 2.7.1, Java 11 |
| Frontend | Vue 3 + Vite 5, PrimeVue 4, Pinia, VueQuill (Quill 2) |
| База данных | PostgreSQL 16 (схема `library`) |
| ORM / JDBC | Hibernate 5.6.15, Spring Data JPA, Commons DBCP 1.4 |
| REST API | Spring MVC `@RestController`, JWT + form login |
| Документация API | OpenAPI / Swagger UI — `/game-library/swagger-ui.html` |
| Скачивание | ZIP + BitTorrent через Transmission (JSON-RPC) |
| P2P-трекер | Встроенный HTTP-трекер — `/api/tracker/announce` |
| Скрапинг | OkHttp 3, Jsoup, Steam Storefront API, Twitch OAuth (IGDB) |
| Сборка | Maven (JAR) + npm / Vite |
| Контейнеризация | Docker, docker-compose (4 сервиса) |

## 🏗 Архитектура

```
┌─────────┐   :80   ┌──────────┐   :8080  ┌──────────────────┐
│ Browser │ ──────▶ │  Nginx   │ ──────▶  │   Backend        │
└─────────┘         │ (Vue SPA)│          │  (REST API)      │
                    └──────────┘          │  + Tracker       │
                                          └───┬────────┬─────┘
                                              │        ▲
                                              │        │ announce
                                              │  ┌─────┴──────────┐
                                              │  │  Transmission  │ :9091 RPC
                                              │  │  (seeder)      │ :51413 P2P
                                              │  └──────┬─────────┘
                                              │         │
                     ┌──────────────┐         │
                     │  PostgreSQL  │  :5432  │
                     └──────────────┘         │
                     Торрент-клиенты
                     (qBittorrent, Transmission, и др.)
                         │              ▲
                         └───── P2P ────┘
```

### Маршруты Frontend

| URL | Доступ | Описание |
|-----|--------|----------|
| `/login` | все | Вход |
| `/register` | все | Регистрация |
| `/` | USER, ADMIN | Библиотека: сетка, фильтры, пагинация |
| `/game/:id` | USER, ADMIN | Детальная карточка игры |
| `/game/:id/edit` | ADMIN | Редактирование + скрапинг |
| `/profile` | USER | Профиль, аватар, пароль |
| `/admin/users` | ADMIN | Управление пользователями |
| `/admin/scrapers` | ADMIN | Настройка скраперов |
| `/downloads` | USER, ADMIN | Статус раздач Transmission |

### API Endpoints

Префикс: `/game-library/api/`. Аутентификация: JWT Bearer.

| Endpoint | Доступ | Описание |
|----------|--------|----------|
| `POST /auth/login` | все | Вход → JWT |
| `POST /auth/register` | все | Регистрация |
| `GET /auth/me` | USER, ADMIN | Текущий пользователь |
| `GET /games` | USER, ADMIN | Список игр (фильтры, сортировка, стр.) |
| `GET /games/{id}` | USER, ADMIN | Детали игры |
| `POST /games/{id}/edit` | ADMIN | Сохранить изменения |
| `POST /games/{id}/grab` | ADMIN | Скраппинг метаданных |
| `GET /games/{id}/download` | USER, ADMIN | Скачать ZIP / .torrent |
| `POST /games/{id}/seed` | USER, ADMIN | Запустить раздачу |
| `GET /downloads/active` | USER, ADMIN | Активные раздачи |
| `GET /tracker/announce` | все | HTTP-трекер BitTorrent |
| `GET /profile` | USER, ADMIN | Данные профиля |
| `POST /profile/update` | USER | Обновить профиль |
| `POST /profile/password` | USER | Сменить пароль |
| `POST /scan` | ADMIN | Сканирование библиотеки |
| `GET /scan/status` | ADMIN | Прогресс сканирования |
| `GET /admin/users` | ADMIN | Список пользователей |
| `POST /admin/users/{id}/toggle-admin` | ADMIN | Смена роли |
| `POST /admin/users/{id}/toggle-active` | ADMIN | Блокировка / разблокировка |
| `POST /admin/users/{id}/reset-password` | ADMIN | Сброс пароля |
| `GET /admin/scraper-config` | ADMIN | Получить конфиги скраперов |
| `POST /admin/scraper-config` | ADMIN | Сохранить конфиги |
| `POST /admin/scraper-config/reload` | ADMIN | Перезагрузить из файла |
| `POST /admin/migrate-images` | ADMIN | Миграция изображений БД→ФС |

## 🔧 Конфигурация

### Структура файлов

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

### Переменные окружения

| Переменная | По умолчанию | Описание |
|-----------|-------------|----------|
| `SERVER_PORT` | `8080` | Порт backend |
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `GAMES_DIRECTORY` | `/gameLibrary` | Корень с играми |
| `IMAGES_DIRECTORY` | `/gameLibrary/images` | Путь к изображениям на ФС |
| `TRACKER_ANNOUNCE_URL` | `http://localhost:8080/game-library/api/tracker/announce` | URL announce в .torrent (должен быть доступен клиентам) |
| `TRANSMISSION_RPC_URL` | `http://transmission:9091/transmission/rpc` | RPC-эндпоинт Transmission |
| `TRANSMISSION_DOWNLOAD_DIR` | `/downloads` | Папка загрузок в контейнере Transmission |
| `JWT_SECRET` | ключ по умолчанию | Секрет JWT |
| `JWT_EXPIRATION_MS` | `86400000` | Время жизни токена (24ч) |
| `SCRAPER_CONFIG_DIR` | `/gameLibrary/gameLibraryConfigs/scrapers` | Директория с `scrapers-config.json` |
| `SCRAPER_ENCRYPTION_KEY` | не задан | AES-256 ключ (base64) для шифрования API-ключей. **Обязателен.** Сгенерировать: `openssl rand -base64 32` |
| `TORRENT_DIR_TMP` | `/torrentDirTmp` | Временная папка для .torrent файлов |
| `TTORRENT_HASHING_THREADS` | `2` | Потоков для хеширования торрентов (меньше на слабых NAS) |

### База данных

Схема `library`:

| Таблица | Назначение |
|---------|-----------|
| `game_data` | Игры (id, name, platform, release_date, description, instruction, trailer_url, logo, directory_path) |
| `game_genre` | Справочник жанров (code, description, description_ru) — ~70 жанров |
| `game_data_genre` | M:N игра ↔ жанр |
| `game_screenshot` | Скриншоты (bytea) |
| `library_user` | Пользователи (user_name, pass BCrypt, is_admin, is_active, avatar bytea) |

DDL: `postgresdb/ddl/` — `1_init.sql` (схема), `2_library.sql` (таблицы + жанры), `3_user.sql` (пользователи + seed).

### Миграция изображений (БД → ФС)

По умолчанию изображения хранятся как `bytea` в БД. Перенос на диск:

```bash
./scripts/migrate-images.sh /gameLibrary/images

# Или через API (ADMIN)
curl -X POST http://localhost:8080/game-library/api/admin/migrate-images \
  -H "Authorization: Bearer <token>"
```

После миграции изображения отдаются с диска с fallback на БД при отсутствии файла.

## 🕷 Скраперы

Конфиг: `scrapers/scrapers-config.json`, управление через `/api/admin/scraper-config`.

| Скрапер | Метод | Авторизация | Что собирает |
|---------|-------|-------------|-------------|
| **Playground** (playground.ru) | CSS-селекторы | — | Название, описание, жанры, скриншоты |
| **Igromania** (igromania.ru) | JSON Path | — | Данные через `initialStoreState` |
| **Steam** (store.steampowered.com) | Storefront API | — | Название, описание, скриншоты, жанры |
| **IGDB** (api.igdb.com) | REST API | Twitch OAuth 2.0 (Client-ID + Bearer) | Полные метаданные |
| **TheGamesDB** (api.thegamesdb.net) | REST API | API-ключ | Полные метаданные (лимит: 1000 запр./мес) |
| **World-Art** (world-art.ru) | CSS-селекторы | — | Парсинг карточки + поиск |

### Настройка IGDB

1. Создать приложение: https://dev.twitch.tv/console/apps/create
2. Скопировать **Client-ID**, создать **Client Secret**
3. Получить токен:
   ```bash
   curl -X POST "https://id.twitch.tv/oauth2/token?client_id=ВАШ_ID&client_secret=ВАШ_SECRET&grant_type=client_credentials"
   ```
4. В админке (`/admin/scrapers`) → IGDB → заполнить `headers.Client-ID` и `encryptedApiKey`

### Настройка TheGamesDB

1. Зарегистрироваться: https://thegamesdb.net/register.php
2. Получить ключ: https://api.thegamesdb.net/key.php
3. В админке → TheGamesDB → `encryptedApiKey`

## 🚀 Развёртывание

### Требования

| Компонент | Версия |
|-----------|--------|
| Java | 11 (JDK) |
| Maven | 3.6+ |
| Node.js | 18+ |
| Docker | 19.03+ (с compose) |
| PostgreSQL | 12+ (только локально) |

### Docker (рекомендуется)

#### Структура директорий

Создать на хосте перед запуском:

```
/mnt/nas/gameLibrary/              # корень библиотеки
├── games/                         # игровые файлы
├── images/                        # скриншоты и обложки
└── gameLibraryConfigs/
    ├── db/                        # данные PostgreSQL (volume)
    └── tracker/                   # Transmission
        ├── config/                # settings.json (авто-создание)
        ├── watch/                 # авто-добавление .torrent
        ├── complete/              # завершённые
        ├── incomplete/            # незавершённые
        └── torrents/              # .torrent от backend
```

```bash
mkdir -p /mnt/nas/gameLibrary/games
mkdir -p /mnt/nas/gameLibrary/gameLibraryConfigs/tracker/{config,watch,complete,incomplete,torrents}
```

#### Быстрый старт

```bash
make all
```

Или пошагово:
```bash
mvn clean package -DskipTests                # backend
cd frontend && npm install && npm run build   # frontend
docker compose up --build -d                  # запуск всех сервисов
```

| Порт | Сервис | Назначение |
|------|--------|-----------|
| `:80` | Nginx | Vue SPA + прокси на API |
| `:8080` | Backend | REST API + HTTP-трекер |
| `:9091` | Transmission | RPC веб-интерфейс |
| `:51413` | Transmission | P2P-трафик (TCP/UDP) |
| `:5432` | PostgreSQL | База данных |

#### Настройка Transmission

Редактировать `gameLibraryConfigs/tracker/config/settings.json` на хосте:
- uTP: `preferred_transports: ["utp", "tcp"]` — **обязательно** для совместимости с uTorrent
- RPC bind: `"rpc-bind-address": "0.0.0.0"` для Windows/WSL
- Остальные параметры Transmission

⛔ `TRANSMISSION_UTP_ENABLED=true` в docker-compose.yml **не работает** — настройки только через `settings.json`.

### Локальная разработка

```bash
# Backend (нужен локальный PostgreSQL + Transmission)
mvn spring-boot:run -Dspring.profiles.active=alone

# Frontend (Vite dev server, проксирует /game-library/* на :8080)
cd frontend && npm run dev
```

Makefile:
```bash
make dev-backend    # mvn spring-boot:run
make dev-frontend   # cd frontend && npm run dev
make logs           # docker-compose logs -f
make clean          # docker-compose down -v && mvn clean && rm -rf frontend/dist
```

### Linux (без Docker)

```bash
# 1. База данных
sudo -u postgres psql -f postgresdb/ddl/1_init.sql
sudo -u postgres psql -f postgresdb/ddl/2_library.sql
sudo -u postgres psql -f postgresdb/ddl/3_user.sql

# 2. Transmission
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

# 4. Frontend
cd frontend && npm run dev
```

### Windows (без Docker)

```powershell
# 1. База данных
psql -U postgres -f postgresdb\ddl\1_init.sql
psql -U postgres -f postgresdb\ddl\2_library.sql
psql -U postgres -f postgresdb\ddl\3_user.sql

# 2. Transmission (через Docker Desktop)
docker run -d --name transmission `
  -p 9091:9091 -p 51413:51413 -p 51413:51413/udp `
  -v D:\GameLibrary\games:/downloads/games `
  -v D:\GameLibrary\gameLibraryConfigs\tracker\config:/config `
  -v D:\GameLibrary\gameLibraryConfigs\tracker\watch:/watch `
  -v D:\GameLibrary\gameLibraryConfigs\tracker\complete:/downloads/complete `
  -v D:\GameLibrary\gameLibraryConfigs\tracker\incomplete:/downloads/incomplete `
  lscr.io/linuxserver/transmission

# 3. Backend
mvn spring-boot:run

# 4. Frontend
cd frontend
npm install
npm run dev
```

**Пути в Windows:** Docker Desktop требует shared-директории в Settings → Resources → File Sharing. В конфигах использовать `/` (например `D:/GameLibrary`).

## 🔍 Типовые проблемы

| Симптом | Причина | Решение |
|---------|---------|---------|
| Пустая страница | Неправильный прокси nginx | Проверить location `/game-library` в nginx.conf |
| Backend не видит БД | Неверный хост/порт/пароль | Проверить `DB_HOST`, `DB_PORT`, работает ли PostgreSQL |
| Transmission не отвечает | Неверный RPC URL или IPv6 | Указать `TRANSMISSION_RPC_URL`; сменить `rpc-bind-address` на `0.0.0.0` |
| Нет изображений | Отсутствуют файлы после миграции | Запустить миграцию: `POST /api/admin/migrate-images` |
| Трекер работает, данных нет | Выключен uTP | Установить `preferred_transports: ["utp", "tcp"]` в `settings.json` |
| `no suitable method found for create` | Не та версия OkHttp | OkHttp 3.x: `RequestBody.create(MediaType, String)` |
