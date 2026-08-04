# GameLibrary

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-blue?logo=openjdk">
  <img src="https://img.shields.io/badge/Spring_Boot-4.0.7-brightgreen?logo=spring">
  <img src="https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs">
  <img src="https://img.shields.io/badge/PostgreSQL-16-316192?logo=postgresql">
  <img src="https://img.shields.io/badge/Docker-ready-2496ED?logo=docker">
</p>

<p align="center">
  <b>RU:</b> Каталогизатор компьютерных игр для NAS — сканирование файловой системы, сбор метаданных из 7 скраперов, просмотр и скачивание через P2P-торренты.
  <br>
  <b>EN:</b> Game catalog manager for NAS — filesystem scanning, metadata from 7 scrapers, browse & download via P2P torrents.
</p>

<p align="center">
  <a href="#-features">Возможности</a> &nbsp;•&nbsp;
  <a href="#-quick-start">Быстрый старт</a> &nbsp;•&nbsp;
  <a href="docs/INSTRUCTION.md">Инструкция</a> &nbsp;•&nbsp;
  <a href="docs/API.md">API</a> &nbsp;•&nbsp;
  <a href="#-tech-stack">Стек</a> &nbsp;•&nbsp;
  <a href="#-конфигурация">Конфигурация</a> &nbsp;•&nbsp;
  <a href="#-скраперы">Скраперы</a> &nbsp;•&nbsp;
  <a href="#-развёртывание">Развёртывание</a>
</p>

---

## ✨ Возможности

| Для пользователей | Для администраторов |
|------------------|-------------------|
| Сетка игр с постерами и фильтрами | Сканирование ФС и авто-индексация |
| Поиск по названию, платформе, жанру, году | Сбор метаданных из 7 скраперов |
| Сортировка и пагинация | Редактор игр с Quill (rich text) |
| Скачивание ZIP (<5 ГБ) / .torrent (≥5 ГБ) | Управление пользователями (роли, блокировка, сброс пароля) |
| P2P-раздача через Transmission | Панель конфигурации скраперов (API-ключи, вкл/выкл) |
| Профиль, аватар, смена пароля | |
| Русский / английский интерфейс | |
| ⭐ Рейтинг игр 1-10 | |
| ❤️ Избранное с фильтром в боковой панели | |
| 🌓 Тёмная тема (авто-определение по системе, ручное переключение) | |
| 🖥️ 4 визуальные темы (светлая/тёмная, ретро-терминал, жёлтый CRT) | |
| 💬 Комментарии на странице игры | |
| 🔔 Уведомления (торрент готов, сканирование завершено и т.д.) | |
| 👁 История просмотров (последние 20, localStorage) | |
| 🔗 Связанные игры (жанр или похожее название) | |
| 📊 Статистика библиотеки (диаграммы по платформам/жанрам/годам, топы) | |
| 📂 Коллекции игр (плейлисты, публичные/приватные, сортировка) | |
| 📝 Рецензии (оценки геймплея/графики/сюжета/музыки 1-10, плюсы/минусы) | |
| 🏷️ Теги (пользовательские метки, фильтр в боковой панели) | |
| 🧠 Умные коллекции (правила оцениваются на сервере, авто-подбор игр) | |
| 🔍 Семантический поиск (векторный поиск по смыслу, Python AI + pgvector) | |
| 🌐 Перевод ru↔en (описания игр через Python AI, кешируется в БД) | |
| 🏷️ Авто-теги (подбор тегов и жанров по ключевым словам) | |

## ⚡ Быстрый старт

```bash
cp .env.example .env          # сначала указать секреты
make all                      # сборка backend + frontend, запуск docker-compose
```

Открыть `http://localhost:8090` — войти как `admin` / `password`.

> 📖 **Пошаговая инструкция** — [docs/INSTRUCTION.md](docs/INSTRUCTION.md)
> 📋 **Описание API** — [docs/API.md](docs/API.md)

### AI-фичи (опционально)

Семантический поиск и перевод работают через Python AI сервис (`ai-service`). Авто-теги работают без AI.

**AI-модели** (скачиваются автоматически с HuggingFace при первом запуске):
- Embedding: [`intfloat/multilingual-e5-small`](https://huggingface.co/intfloat/multilingual-e5-small) — 384-мерные векторы
- Перевод: [`facebook/nllb-200-distilled-600M`](https://huggingface.co/facebook/nllb-200-distilled-600M) — одна модель для обоих направлений

Если AI не нужен — закомментируйте блок `ai-service` в `docker-compose.yml` (экономия ~2 ГБ RAM и ~2 ГБ диска).

## 📦 Технологический стек

| Компонент | Технология |
|-----------|-----------|
| Backend | Spring Boot 4.0.7, Java 25, Virtual Threads (Project Loom) |
| Frontend | Vue 3 + Vite 5, PrimeVue 4, Pinia, VueQuill (Quill 2) |
| База данных | PostgreSQL 16 (схема `library`) + pgvector |
| ORM / JDBC | Hibernate, Spring Data JPA, HikariCP |
| REST API | Spring MVC, JWT + form login |
| Документация API | Swagger UI — `/game-library/swagger-ui.html` |
| Скачивание | ZIP (STORED, без сжатия) + BitTorrent через Transmission |
| P2P-трекер | Встроенный HTTP-трекер — `/api/tracker/announce` |
| Скрапинг | OkHttp 4, Jsoup, Steam Storefront API, Twitch OAuth (IGDB) |
| Rate Limiting | bucket4j 8.7.0 (in-memory, per-IP) |
| Изображения | DB bytea + FS override, ETag + Cache-Control (24ч), lazy loading |
| AI / ML | Python FastAPI, PyTorch, HuggingFace |
| Контейнеризация | Docker, docker-compose (5 сервисов) |

## 🏗 Маршруты

| URL | Доступ | Описание |
|-----|--------|----------|
| `/login` | все | Вход |
| `/register` | все | Регистрация |
| `/` | USER, ADMIN | Библиотека (сетка, фильтры, пагинация) |
| `/game/:id` | USER, ADMIN | Карточка игры |
| `/game/:id/edit` | ADMIN | Редактирование + скрапинг |
| `/profile` | USER | Профиль (аватар, пароль, статистика) |
| `/admin/users` | ADMIN | Управление пользователями |
| `/admin/scrapers` | ADMIN | Настройка скраперов |
| `/downloads` | USER, ADMIN | Статус раздач Transmission |
| `/statistics` | USER, ADMIN | Статистика библиотеки |
| `/collections` | USER, ADMIN | Коллекции игр |
| `/collections/:id` | USER, ADMIN | Детали коллекции |

## 🔧 Конфигурация

### Структура файлов

```
<games_directory>/games/
└── <platform>/                       (PC, PlayStation, Xbox, ...)
    └── <game_name>/
        ├── <файлы игры>...
        └── information/              (создаётся при сканировании)
            ├── logo.jpg
            ├── information.json      (название, год, жанры, описание, трейлер)
            └── img/                  (скриншоты .jpg)
```

### Переменные окружения

#### Обязательные (задаются в `.env`)

| Переменная | Описание |
|-----------|----------|
| `POSTGRES_PASSWORD` | Пароль суперпользователя PostgreSQL |
| `DB_PASSWORD` | Пароль пользователя приложения (`library-manager-user`) |
| `JWT_SECRET` | Секрет подписи JWT (`openssl rand -hex 32`) |
| `SCRAPER_ENCRYPTION_KEY` | AES-256 ключ (base64) для шифрования API-ключей скраперов (`openssl rand -base64 32`) |

#### Опциональные (со значениями по умолчанию)

| Переменная | По умолчанию | Описание |
|-----------|-------------|----------|
| `SERVER_PORT` | `8080` | Порт backend |
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `GAMES_DIRECTORY` | `/gameLibrary` | Корень с играми |
| `IMAGES_DIRECTORY` | `/gameLibrary/images` | Путь к изображениям на ФС |
| `TRACKER_ANNOUNCE_URL` | `http://localhost:8080/game-library/api/tracker/announce` | Announce URL в .torrent |
| `TRANSMISSION_RPC_URL` | `http://transmission:9091/transmission/rpc` | RPC-эндпоинт Transmission |
| `TRANSMISSION_DOWNLOAD_DIR` | `/downloads` | Папка загрузок Transmission |
| `CORS_ALLOWED_ORIGINS` | *(только same-origin)* | Разрешённые CORS-источники |
| `AI_SERVICE_URL` | `http://ai-service:8000` | Эндпоинт AI сервиса |
| `JWT_EXPIRATION_MS` | `86400000` | Время жизни токена (24ч) |
| `SCRAPER_CONFIG_DIR` | `/gameLibrary/gameLibraryConfigs/scrapers` | Директория с `scrapers-config.json` |
| `RESET_PASSWORD_DEFAULT` | *(авто-генерация)* | Пароль по умолчанию при сбросе админом |

### База данных

Схема `library`:

| Таблица | Назначение |
|---------|-----------|
| `game_data` | Игры (название, платформа, описание, embedding vector(384), размер) |
| `game_genre` | Справочник жанров (~70 записей) |
| `game_data_genre` | M:N игра ↔ жанр |
| `game_screenshot` | Скриншоты (bytea) |
| `game_rating` | Оценки 1-10 (unique user+game) |
| `favorite_game` | Избранное |
| `game_comment` | Комментарии |
| `notification` | Уведомления |
| `game_collection` | Коллекции (обычные + умные) |
| `game_collection_entry` | M:N коллекция ↔ игра |
| `game_tag` | Справочник тегов |
| `game_data_tag` | M:N игра ↔ тег |
| `game_review` | Рецензии (4 категории 1-10, плюсы/минусы) |
| `library_user` | Пользователи (BCrypt, роли) |

DDL: `postgresdb/ddl/` — выполняются при первом запуске контейнера PostgreSQL в алфавитном порядке.

## 🔒 Безопасность

Все учётные данные вынесены в `.env` (`.env` в `.gitignore` — **не коммитить**).

| Секрет | Путь |
|--------|------|
| `POSTGRES_PASSWORD` | `.env` → docker-compose → postgres |
| `DB_PASSWORD` | `.env` → docker-compose → backend → `application.yml` |
| `JWT_SECRET` | `.env` → docker-compose → backend → подпись JWT |
| `SCRAPER_ENCRYPTION_KEY` | `.env` → docker-compose → backend → AES-256 шифрование ключей скраперов |

- JWT access token (15 мин) + refresh token (7 дн), refresh — silently через Axios interceptor
- Rate limiting: login 5 запр/мин (IP+User-Agent), API 100 запр/мин (IP) → HTTP 429
- Пароль БД никогда не хранится в `application.yml` в открытом виде
- Сброс пароля админом: `SecureRandom` генерирует 8-байтовый base64-пароль

## 🕷 Скраперы

Конфиг: `${SCRAPER_CONFIG_DIR}/scrapers-config.json`, управление через `/api/admin/scraper-config`.

| Скрапер | Метод | Ключ | Что собирает |
|---------|-------|------|-------------|
| **Playground** (playground.ru) | CSS + search API | — | Название, описание, жанры, скриншоты |
| **Igromania** (igromania.ru) | JSON Path | — | Данные через `initialStoreState` |
| **Steam** (store.steampowered.com) | Storefront API | — | Название, описание, скриншоты, жанры |
| **IGDB** (api.igdb.com) | REST API | Twitch OAuth 2.0 | Полные метаданные |
| **TheGamesDB** (api.thegamesdb.net) | REST API | API-ключ | Полные метаданные (1000 запр/мес) |
| **World-Art** (world-art.ru) | CSS | — | Карточка + поиск |
| **PsxDataCenter** (psxdatacenter.com) | JSoup | — | PS1/PS2: описание, жанры, скриншоты, серийный номер |

### Настройка IGDB

1. Создать приложение: https://dev.twitch.tv/console/apps/create
2. Скопировать Client-ID, создать Client Secret
3. Получить токен:
   ```bash
   curl -X POST "https://id.twitch.tv/oauth2/token?client_id=ID&client_secret=SECRET&grant_type=client_credentials"
   ```
4. В админке (`/admin/scrapers`) → IGDB → `headers.Client-ID` и `encryptedApiKey`

### Настройка TheGamesDB

1. Зарегистрироваться: https://thegamesdb.net/register.php
2. Получить ключ: https://api.thegamesdb.net/key.php
3. В админке → TheGamesDB → `encryptedApiKey`

## 🚀 Развёртывание

### Системные требования

#### Минимальные (без AI)

| Ресурс | Минимум |
|--------|---------|
| CPU | 1 ядро |
| RAM | 2 ГБ |
| Диск | 100 МБ (приложение) + игры |

#### С AI (рекомендуется)

| Ресурс | Минимум |
|--------|---------|
| CPU | 2 ядра |
| RAM | 4 ГБ |
| Диск | 2 ГБ (приложение + AI-модели) + игры |
| PostgreSQL | pgvector (`pgvector/pgvector:pg16`) |

Embedding: ~200-500ms/игру. Перевод: ~1-5s/описание. Всё на CPU — GPU не требуется.

### Docker (рекомендуется)

```bash
# 1. Подготовить секреты
cp .env.example .env
# Отредактировать .env

# 2. Создать структуру папок
mkdir -p /mnt/nas/gameLibrary/{games,images,gameLibraryConfigs/{db/data,scrapers,models,tracker/{config,watch,complete,incomplete,torrents}}}

# 3. Запустить
make all
```

Структура на хосте:

```
/mnt/nas/gameLibrary/
├── games/                             # Файлы игр
├── images/                            # Скриншоты и обложки
└── gameLibraryConfigs/
    ├── db/data/                       # Данные PostgreSQL
    ├── scrapers/                      # scrapers-config.json
    ├── models/                        # HuggingFace модели
    └── tracker/
        ├── config/                    # Transmission settings.json
        ├── watch/                     # Авто-добавление .torrent
        ├── complete/                  # Завершённые загрузки
        ├── incomplete/                # Незавершённые загрузки
        └── torrents/                  # .torrent файлы
```

Порты:

| Порт | Сервис | Назначение |
|------|--------|-----------|
| `:8090` | Nginx | Vue SPA + прокси на API |
| `:8080` | Backend | REST API + HTTP-трекер |
| `:8000` | AI сервис | Перевод + embedding |
| `:9091` | Transmission | RPC веб-интерфейс |
| `:51413` | Transmission | P2P-трафик |
| `:5432` | PostgreSQL | База данных |

### Локальная разработка

```bash
export $(grep -v '^#' .env | xargs)

# Backend (нужен локальный PostgreSQL)
mvn spring-boot:run -Dspring.profiles.active=alone

# Frontend (Vite dev server, прокси на :8080)
cd frontend && npm run dev
```

Makefile:

```bash
make dev-backend    # mvn spring-boot:run
make dev-frontend   # npm run dev
make logs           # docker compose logs -f
make clean          # docker compose down -v + mvn clean + rm -rf frontend/dist
```

## 🔍 Типовые проблемы

| Симптом | Причина | Решение |
|---------|---------|---------|
| Пустая страница | Неправильный прокси nginx | Проверить location `/game-library` в nginx.conf |
| Backend не видит БД | Неверный хост/порт/пароль | Проверить `DB_HOST`, `DB_PORT` |
| Торрент не скачивается | uTP выключен | `"preferred_transports": ["utp", "tcp"]` в `settings.json` |
| `403 Invalid CORS request` | Не задан `CORS_ALLOWED_ORIGINS` | Добавить в `.env` и `docker-compose.yml` |
| Контейнеры падают | Не заполнен `.env` | Проверить все 4 обязательные переменные |
| AI не работает | Модели не загружены | Дождаться первой загрузки с HuggingFace (~2 ГБ) |
