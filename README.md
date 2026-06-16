# GameLibrary

Каталогизатор компьютерных игр для NAS. Сканирует файловую систему, формирует библиотеку с метаданными и предоставляет веб-интерфейс для просмотра, поиска, редактирования и скачивания игр.

## Технологический стек

| Компонент | Технология |
|-----------|-----------|
| Backend | Spring Boot 2.7.1, Java 1.8 |
| Frontend | Vue 3 + Vite, PrimeVue 4 |
| База данных | PostgreSQL (схема `library`) |
| ORM / JDBC | Hibernate 5.6, Spring Data JPA, Commons DBCP 1.4 |
| REST API | Spring MVC `@RestController`, JWT auth |
| Документация API | OpenAPI / Swagger UI |
| Скачивание | aria2 (JSON-RPC) + AriaNg Web UI |
| Сеть | OkHttp 3, Jsoup, HtmlUnit (скрапинг) |
| Сборка backend | Maven, JAR packaging |
| Сборка frontend | npm / Vite |
| Контейнеризация | Docker, docker-compose |

## Архитектура

```
┌─────────┐   :80   ┌──────────┐   :8080  ┌──────────────┐
│ Browser │ ──────▶ │  Nginx   │ ──────▶  │   Backend    │
└─────────┘         │ (Vue SPA)│          │  (REST API)  │
                    └──────────┘          └──────┬───────┘
                                                  │
                                          ┌───────▼───────┐
                                          │    aria2      │ :6800 RPC
                                          │   (JSON-RPC)  │ :6888 DHT
                                          └───────┬───────┘
                                                  │
                                          ┌───────▼───────┐
                                          │    AriaNg     │ :6880 Web UI
                                          └───────────────┘
                           :5432
                    ┌──────────────┐
                    │  PostgreSQL  │
                    └──────────────┘
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
| `/downloads` | USER, ADMIN | Статус загрузок aria2 |

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
| `POST /api/games/{id}/seed` | USER, ADMIN | Запуск раздачи через aria2 |
| `GET /api/downloads/active` | USER, ADMIN | Активные загрузки aria2 |
| `GET /api/profile` | USER, ADMIN | Профиль |
| `POST /api/scan` | ADMIN | Сканирование библиотеки |
| `GET /api/admin/users` | ADMIN | Список пользователей |
| `POST /api/admin/users/{id}/toggle-admin` | ADMIN | Смена роли |
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
- Название, год, платформа, описание, инструкция, трейлер
- Жанры из общего справочника
- Сбор метаданных с Playground.ru / Igromania.ru

### Скачивание игр
- **Малые игры (<1 ГБ)**: потоковая упаковка в ZIP
- **Крупные игры (≥1 ГБ)**: создание .torrent (DHT/PEX) + раздача через aria2
- **Seed via aria2**: кнопка на странице игры — создаёт торрент и запускает сидирование на NAS

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
| `:8080` | backend | Spring Boot REST API |
| `:6880` | AriaNg | Web UI для aria2 |
| `:6888` | aria2 | DHT (торренты) |
| `:5432` | postgresdb | PostgreSQL |

### Локальная разработка

```bash
# Backend (нужен PostgreSQL + aria2 локально)
mvn spring-boot:run

# Frontend (Vite dev server с прокси на localhost:8080)
cd frontend && npm run dev
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

DDL: `postgresdb/ddl/1_init.sql`, `2_library.sql`, `3_user.sql`.

## Окружение

Переменные среды (с значениями по умолчанию):

| Переменная | По умолчанию | Описание |
|-----------|-------------|----------|
| `SERVER_PORT` | `8080` | Порт backend |
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `GAMES_DIRECTORY` | `/gameLibrary` | Корень с играми |
| `IMAGES_DIRECTORY` | `/gameLibrary/images` | Изображения на ФС |
| `ARIA2_RPC_URL` | `http://aria2:6800/jsonrpc` | JSON-RPC aria2 |
| `ARIA2_RPC_SECRET` | (пусто) | Секрет aria2 |
| `JWT_SECRET` | (ключ по умолчанию) | Секрет JWT |
| `JWT_EXPIRATION_MS` | `86400000` | Время жизни токена (24ч) |

Подробнее о подводных камнях — см. `AGENTS.md`.

## Развёртывание на Linux

### Требования

| Компонент | Версия | Установка |
|-----------|--------|-----------|
| Java | 8 (JDK) | `apt install openjdk-8-jdk` / `sdk install java 8.0.402-tem` |
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
  aria2:
    rpc-url: http://localhost:6800/jsonrpc   # для локального запуска
    rpc-secret: mysecret                      # если задан секрет aria2

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/game-library?currentSchema=library
    username: library-manager-user
    password: 2wq345tgfiNcbBBwee3
```

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

# 4. Настроить volume paths в docker-compose.yml
#    Отредактируйте раздел volumes для сервисов backend и aria2:
#
#    backend:
#      volumes:
#        - /mnt/nas/gameLibrary:/gameLibrary      # ваши игры
#        - /mnt/nas/torrents:/torrentDirTmp        # временные .torrent файлы
#
#    aria2:
#      volumes:
#        - /mnt/nas/gameLibrary:/gameLibrary      # для доступа к файлам игр
#        - /mnt/nas/torrents:/torrentDirTmp        # .torrent файлы
#        - /mnt/nas/aria2-config:/config           # конфиг aria2
#        - /mnt/nas/aria2-downloads:/downloads     # загрузки aria2

# 5. Запустить все сервисы
docker compose up --build -d

# 6. Проверить:
#    - Frontend: http://localhost
#    - Swagger UI: http://localhost/game-library/swagger-ui.html
#    - AriaNg: http://localhost:6880
#    - API: http://localhost:8080/game-library/api/games
```

### Сборка и запуск (без Docker)

```bash
# 1. База данных
sudo -u postgres psql -f postgresdb/ddl/1_init.sql
sudo -u postgres psql -f postgresdb/ddl/2_library.sql
sudo -u postgres psql -f postgresdb/ddl/3_user.sql

# 2. aria2 (в отдельном терминале)
docker run -d --name aria2 \
  -p 6800:6800 -p 6888:6888 \
  -v /mnt/nas/gameLibrary:/gameLibrary \
  -v /mnt/nas/torrents:/torrentDirTmp \
  -e SECRET=mysecret \
  p3terx/aria2-pro

# 3. Backend
mvn spring-boot:run

# 4. Frontend (в отдельном терминале)
cd frontend && npm run dev
# Frontend будет на http://localhost:5173, API проксируется на :8080
```

### Настройка aria2 для Linux

Создайте `/mnt/nas/aria2-config/aria2.conf`:

```ini
enable-rpc=true
rpc-listen-all=true
rpc-secret=mysecret
dir=/downloads
max-concurrent-downloads=5
max-connection-per-server=16
seed-ratio=0.0
seed-time=0
bt-enable-lpd=true
enable-dht=true
dht-listen-port=6888
peer-id-prefix=-TR2940-
user-agent=Transmission/2.94
```

## Развёртывание на Windows

### Требования

| Компонент | Версия | Ссылка |
|-----------|--------|--------|
| Java | 8 (JDK) | [Adoptium Temurin 8](https://adoptium.net/temurin/releases/?version=8) |
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
  aria2:
    rpc-url: http://localhost:6800/jsonrpc
    rpc-secret: mysecret

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

# 4. Настроить volume paths в docker-compose.yml
#    Замените Linux-пути на Windows:
#
#    backend:
#      volumes:
#        - D:/GameLibrary:/gameLibrary
#        - D:/torrents:/torrentDirTmp
#
#    aria2:
#      volumes:
#        - D:/GameLibrary:/gameLibrary
#        - D:/torrents:/torrentDirTmp
#        - D:/aria2-config:/config
#        - D:/aria2-downloads:/downloads

# 5. Запустить все сервисы
docker compose up --build -d
```

### Сборка и запуск (без Docker)

На Windows без Docker все компоненты запускаются вручную.

#### 1. PostgreSQL

Установите PostgreSQL, создайте БД через pgAdmin или psql:

```powershell
# Подключиться к PostgreSQL и выполнить DDL
psql -U postgres -f postgresdb\ddl\1_init.sql
psql -U postgres -f postgresdb\ddl\2_library.sql
psql -U postgres -f postgresdb\ddl\3_user.sql
```

#### 2. aria2

Скачайте [aria2 для Windows](https://github.com/aria2/aria2/releases), распакуйте в `C:\aria2`.

Создайте `C:\aria2\aria2.conf`:

```ini
enable-rpc=true
rpc-listen-all=true
rpc-secret=mysecret
dir=C:\aria2-downloads
max-concurrent-downloads=5
max-connection-per-server=16
seed-ratio=0.0
seed-time=0
bt-enable-lpd=true
enable-dht=true
dht-listen-port=6888
```

Запустите aria2:

```powershell
C:\aria2\aria2c.exe --conf-path=C:\aria2\aria2.conf
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

### aria2 не отвечает
- При запуске в Docker: `docker compose logs aria2` — проверьте, что RPC включён.
- Проверьте `ARIA2_RPC_URL`: в Docker `http://aria2:6800/jsonrpc`, локально `http://localhost:6800/jsonrpc`.
- Если задан `rpc-secret`, он должен совпадать в `aria2.conf` и `application.yml`.

### Изображения не отображаются
- После миграции изображения хранятся на диске, путь должен совпадать с `IMAGES_DIRECTORY`.
- Если файл отсутствует — будет fallback на БД (работает медленнее).
- Запустите миграцию: `POST /api/admin/migrate-images` или `scripts/migrate-images.sh`.

### Ошибка "no suitable method found for create"
- OkHttp 3.x: `RequestBody.create(MediaType, String)` — сначала MediaType, потом String.
- Если вы обновили OkHttp до 4.x, API изменился. Держите OkHttp 3.12.x для совместимости с Java 8.
