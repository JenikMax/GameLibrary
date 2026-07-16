# GameLibrary — Пошаговая инструкция по развёртыванию

> Полное руководство для тех, кто впервые запускает приложение.  
> Если что-то пошло не так — смотрите раздел [Типовые проблемы](#10-типовые-проблемы) или [Common issues](#10-common-issues).

<p align="center">
  <a href="#ru">🇷🇺 Русский</a> &nbsp;|&nbsp; <a href="#en">🇬🇧 English</a>
</p>

---

<a name="ru"></a>

## Содержание

1. [Что нужно для запуска](#1-что-нужно-для-запуска)
2. [Где взять проект](#2-где-взять-проект)
3. [Настройка секретов (.env)](#3-настройка-секретов-env)
4. [Создание структуры папок](#4-создание-структуры-папок)
5. [Сборка и запуск (2 способа)](#5-сборка-и-запуск-2-способа)
6. [Проверка — всё работает?](#6-проверка--всё-работает)
7. [Первый вход в систему](#7-первый-вход-в-систему)
8. [Добавление игр в библиотеку](#8-добавление-игр-в-библиотеку)
9. [Настройка скраперов (IGDB, TheGamesDB)](#9-настройка-скраперов-igdb-thegamesdb)
10. [Если что-то пошло не так](#10-типовые-проблемы)

---

## 1. Что нужно для запуска

У вас должны быть установлены:

| Программа | Версия | Зачем |
|-----------|--------|-------|
| **Git** | любая | скачать проект |
| **Docker** | 19.03+ | запуск контейнеров |
| **Docker Compose** | входит в Docker | управление сервисами |

**Необязательно** (только если будете собирать вручную без Makefile):
- Java 25 (JDK)
- Maven 3.6+
- Node.js 18+

Проверьте, что установлено:

```bash
git --version
docker --version
docker compose version
```

Если какой-то команды нет — установите её через пакетный менеджер вашей системы:

```bash
# Ubuntu / Debian
sudo apt install git docker.io docker-compose-v2

# Windows — скачать Docker Desktop: https://www.docker.com/products/docker-desktop/
# macOS — скачать Docker Desktop или: brew install --cask docker
```

---

## 2. Где взять проект

Склонируйте репозиторий:

```bash
git clone https://github.com/ваш-username/GameLibrary.git
cd GameLibrary
```

Если у вас нет доступа к GitHub — скопируйте папку проекта через USB/сеть и перейдите в неё.

---

## 3. Настройка секретов (.env)

Скопируйте файл-шаблон:

```bash
cp .env.example .env
```

Теперь откройте `.env` в любом текстовом редакторе и **обязательно** заполните 4 поля:

```
POSTGRES_PASSWORD=придумайте_пароль_для_postgres
DB_PASSWORD=придумайте_пароль_для_приложения
JWT_SECRET=openssl rand -hex 32
SCRAPER_ENCRYPTION_KEY=openssl rand -base64 32
```

Как придумать пароли:

```bash
# Сгенерировать надёжный JWT_SECRET:
openssl rand -hex 32

# Сгенерировать SCRAPER_ENCRYPTION_KEY:
openssl rand -base64 32
```

> **Важно:** Не используйте пароли из примеров. Сгенерируйте свои.  
> Пароли могут быть любыми, но надёжными — хотя бы 12 символов, буквы + цифры.

Пример готового `.env`:

```
POSTGRES_PASSWORD=MyStr0ng!Pass2024
DB_PASSWORD=Libr4ry_DB_2024!
JWT_SECRET=a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6
SCRAPER_ENCRYPTION_KEY=u9Lx+7kPq3Rm8Yv2Wn5Bt0Jf4Dg1Hs6Kc= 
```

---

## 4. Создание структуры папок

Приложению нужны папки для хранения игр, изображений и настроек Transmission.

Выполните:

```bash
mkdir -p /mnt/nas/gameLibrary/{games,images,gameLibraryConfigs/{db/data,scrapers,tracker/{config,watch,complete,incomplete,torrents}}}
```

Что получится:

```
/mnt/nas/gameLibrary/
├── games/                          # сюда кладёте папки с играми
├── images/                         # сюда будут сохраняться обложки и скриншоты
└── gameLibraryConfigs/
    ├── db/data/                    # файлы базы данных PostgreSQL (создадутся сами)
    ├── scrapers/                   # конфигурация скраперов (scrapers-config.json)
    └── tracker/
        ├── config/                 # сюда Transmission сохранит settings.json
        ├── watch/                  # киньте .torrent — Transmission сам подхватит
        ├── complete/               # завершённые загрузки
        ├── incomplete/             # незавершённые загрузки
        └── torrents/               # .torrent файлы от GameLibrary
```

Если вы используете Windows (WSL или Docker Desktop), замените путь на Windows-стиль:

```powershell
mkdir D:\GameLibrary\games
mkdir D:\GameLibrary\images
mkdir D:\GameLibrary\gameLibraryConfigs\db\data
mkdir D:\GameLibrary\gameLibraryConfigs\scrapers
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\config
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\watch
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\complete
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\incomplete
mkdir D:\GameLibrary\gameLibraryConfigs\tracker\torrents
```

> **Для Windows:** Эти папки нужно будет «расшарить» в Docker Desktop:  
> Settings → Resources → File Sharing — добавить `D:\GameLibrary`.

---

## 5. Сборка и запуск (2 способа)

### Способ A: Быстрый (через Makefile)

Если у вас установлен `make`:

```bash
make all
```

Эта команда сделает всё сама: соберёт backend, frontend и запустит Docker Compose.

### Способ B: Пошагово (подходит всем)

#### Шаг 1 — Сборка backend

```bash
mvn clean package -DskipTests
```

Если Maven не установлен, можно установить:

```bash
# Ubuntu / Debian
sudo apt install maven

# macOS
brew install maven

# Windows — скачать с https://maven.apache.org/download.cgi
```

**Ожидаемый результат:** В конце должно появиться `BUILD SUCCESS`.  
Файл `target/game-library.jar` — собранный backend.

**Если Maven нет и ставить не хотите** — можно пропустить этот шаг, Docker сам соберёт backend внутри контейнера при `docker compose up --build`.

#### Шаг 2 — Сборка frontend

```bash
cd frontend
npm install
npm run build
cd ..
```

Если Node.js не установлен:

```bash
# Ubuntu / Debian
sudo apt install nodejs npm

# macOS
brew install node

# Windows — скачать с https://nodejs.org/
```

**Ожидаемый результат:** Появится папка `frontend/dist/` — внутри собранный сайт.

#### Шаг 3 — Запуск Docker Compose

```bash
docker compose up --build -d
```

`--build` — пересобрать образы (нужно при первом запуске или после изменений).  
`-d` — запустить в фоне.

**Ожидаемый результат:** Docker скачает образы (PostgreSQL, Transmission) и запустит 4 контейнера.

Проверить, что всё запустилось:

```bash
docker compose ps
```

Должны гореть `Up` напротив всех четырёх сервисов:

```
NAME                    STATUS
game-library-backend    Up
game-library-frontend   Up  
game-library-db         Up  
game-library-transmission Up
```

---

## 6. Проверка — всё работает?

Откройте браузер и перейдите по адресу:

```
http://localhost
```

Если вы запускаете на NAS или другом компьютере — замените `localhost` на IP-адрес:

```
http://192.168.1.100
```

**Что должно появиться:** Страница входа в GameLibrary.

Если страница не открывается — проверьте логи:

```bash
docker compose logs -f
```

Нажмите `Ctrl+C`, чтобы выйти из просмотра логов.

---

## 7. Первый вход в систему

После первого запуска в базе уже есть администратор:

| Поле | Значение |
|------|----------|
| Логин | `admin` |
| Пароль | `password` |

Войдите с этими данными.

**Что делать дальше:**

1. Сразу смените пароль администратора:
   - Нажмите на иконку профиля (правый верхний угол)
   - Профиль → «Сменить пароль»
   - Введите старый (`password`) и новый пароль

2. Создайте обычных пользователей (если нужно):
   - Админ-панель → Пользователи
   - Кнопка «Добавить пользователя» (если она есть в UI) — или пользователи сами зарегистрируются через страницу `/register`

---

## 8. Добавление игр в библиотеку

Есть два способа:

### Способ A: Сканирование файловой системы

Если у вас уже есть папки с играми, разложите их по платформам:

```
/mnt/nas/gameLibrary/games/
├── PC/
│   ├── Half-Life 2/
│   │   ├── hl2.exe
│   │   ├── ...
│   │   └── information/         # создастся при сканировании
│   └── Portal/
│       └── ...
├── PlayStation/
│   └── ...
└── Xbox/
    └── ...
```

> Имя папки игры — это название, которое попадёт в библиотеку.

После того как разложили игры, в админ-панели:

1. Зайдите в **Администрирование** → **Сканирование**
2. Нажмите **«Запустить сканирование»**
3. Дождитесь завершения (в логах backend или в уведомлениях)

### Способ B: Добавить игру вручную (если нет файлов)

1. На странице библиотеки нажмите **«Добавить игру»** (если есть)  
   *Или* откройте адрес `http://localhost/game/0/edit` (число = автоматический новый ID)

2. Заполните название, платформу, год

3. Используйте **скраперы** для автозаполнения:
   - Справа выберите скрапер (например, Playground, Steam, IGDB)
   - Нажмите **«Скрапить»**
   - Описание, жанры, скриншоты подтянутся автоматически

4. Нажмите **«Сохранить»** — попадёте на карточку игры

---

## 9. Настройка скраперов (IGDB, TheGamesDB)

Некоторые скраперы работают без ключа (Playground, Steam, World-Art, PsxDataCenter).  
Для IGDB и TheGamesDB нужны API-ключи.

### IGDB (через Twitch)

**Шаг 1.** Зайдите на https://dev.twitch.tv/console/apps/create  
**Шаг 2.** Создайте приложение:
- Name: любое (например `GameLibrary`)
- OAuth Redirect URLs: `http://localhost`
- Category: выберите любой

**Шаг 3.** Скопируйте **Client-ID** (показывается в списке приложений)  
**Шаг 4.** Нажмите **New Secret** → скопируйте **Client Secret**

**Шаг 5.** Получите access token:

```bash
curl -X POST "https://id.twitch.tv/oauth2/token?client_id=ВАШ_CLIENT_ID&client_secret=ВАШ_SECRET&grant_type=client_credentials"
```

Ответ будет таким:

```json
{
  "access_token": "abcdef123456...",
  "expires_in": 5085600,
  "token_type": "bearer"
}
```

Скопируйте `access_token`.

**Шаг 6.** Зайдите в админку приложения: `http://localhost/admin/scrapers`  
**Шаг 7.** Выберите **IGDB**
- Поле `Client-ID` → вставьте Client-ID из шага 3
- Поле `encryptedApiKey` → вставьте `access_token` из шага 5

**Шаг 8.** Нажмите **Сохранить**

### TheGamesDB

**Шаг 1.** Зарегистрируйтесь на https://thegamesdb.net/register.php  
**Шаг 2.** Подтвердите email  
**Шаг 3.** Зайдите на https://api.thegamesdb.net/key.php — скопируйте ключ  
**Шаг 4.** В админке (`/admin/scrapers`) → TheGamesDB → вставьте ключ в `encryptedApiKey`  
**Шаг 5.** Сохраните

---

## 10. Типовые проблемы

| Симптом | Причина | Что делать |
|---------|---------|------------|
| `docker compose` не найдена | Старая версия Docker | Используйте `docker-compose` (с дефисом) или обновите Docker |
| Контейнеры падают с ошибкой | Не заполнен `.env` | Проверьте, что все 4 переменные в `.env` не пустые |
| Страница не открывается (Connection refused) | Контейнеры не запустились | `docker compose ps` — все ли `Up`? `docker compose logs -f` — смотрите ошибки |
| 403 Forbidden при запросе к API | CORS | Добавьте в `.env`: `CORS_ALLOWED_ORIGINS=http://localhost` и перезапустите |
| Не открывается админка | Вы не администратор | Войдите как `admin` / `password` |
| Скрапер не находит игру | Нет API-ключа | Настройте IGDB или TheGamesDB (см. [раздел 9](#9-настройка-скраперов-igdb-thegamesdb)) |
| Transmission не раздаёт | uTP выключен | Отредактируйте `gameLibraryConfigs/tracker/config/settings.json` — добавьте `"preferred_transports": ["utp", "tcp"]` и перезапустите `docker compose restart transmission` |
| Торрент не скачивается / застрял на 0% | uTP не включён | См. выше |
| База данных не инициализируется | CRLF в SQL-скриптах | Убедитесь, что `.gitattributes` корректен, или выполните `git config core.autocrlf false` |

---

## ❗ Важные замечания

- **Пароль admin / password** — смените его сразу после входа.
- **Файл `.env`** содержит пароли — не коммитьте его в Git (он уже в `.gitignore`).
- **Порты:** Если порт 80 занят (Apache, Nginx), отредактируйте `docker-compose.yml` — измените `"80:80"` на `"8080:80"`, тогда сайт будет на `http://localhost:8080`.
- После изменения `.env` нужно перезапустить контейнеры: `docker compose down && docker compose up -d`.

---

## Глоссарий

| Термин | Что значит |
|--------|------------|
| **Backend** | Серверная часть на Java, обрабатывает запросы, работает с БД |
| **Frontend** | Веб-интерфейс на Vue.js, который вы видите в браузере |
| **Docker** | Система контейнеризации — упаковывает приложение со всем окружением |
| **Docker Compose** | Инструмент для запуска нескольких контейнеров сразу |
| **Скрапер** | Программа, которая собирает информацию об игре с сайта (описание, жанры, скриншоты) |
| **Transmission** | Торрент-клиент для раздачи игр |
| **PostgreSQL** | База данных, где хранятся игры, пользователи, настройки |
| **uTP** | Протокол для P2P-соединений; без него uTorrent не может скачивать |
| **JWT** | Токен для входа — приложение запоминает его и вы не вводите пароль каждый раз |

---

<a name="en"></a>

# English version

## 1. Prerequisites

| Software | Version | Purpose |
|----------|---------|---------|
| **Git** | any | download project |
| **Docker** | 19.03+ | run containers |
| **Docker Compose** | included in Docker | manage services |

**Optional** (only if building manually without Makefile):
- Java 25 (JDK)
- Maven 3.6+
- Node.js 18+

Check what's installed:

```bash
git --version
docker --version
docker compose version
```

## 2. Get the project

```bash
git clone https://github.com/your-username/GameLibrary.git
cd GameLibrary
```

## 3. Configure secrets (.env)

```bash
cp .env.example .env
```

Open `.env` and **fill in 4 required fields**:

```
POSTGRES_PASSWORD=your_postgres_password
DB_PASSWORD=your_app_password
JWT_SECRET=openssl rand -hex 32
SCRAPER_ENCRYPTION_KEY=openssl rand -base64 32
```

Generate strong values:
```bash
openssl rand -hex 32          # for JWT_SECRET
openssl rand -base64 32       # for SCRAPER_ENCRYPTION_KEY
```

## 4. Create directory structure

```bash
mkdir -p /mnt/nas/gameLibrary/{games,images,gameLibraryConfigs/{db/data,scrapers,tracker/{config,watch,complete,incomplete,torrents}}}
```

On Windows (PowerShell):
```powershell
mkdir D:\GameLibrary\games, D:\GameLibrary\images, D:\GameLibrary\gameLibraryConfigs\db\data, D:\GameLibrary\gameLibraryConfigs\scrapers, D:\GameLibrary\gameLibraryConfigs\tracker\config, D:\GameLibrary\gameLibraryConfigs\tracker\watch, D:\GameLibrary\gameLibraryConfigs\tracker\complete, D:\GameLibrary\gameLibraryConfigs\tracker\incomplete, D:\GameLibrary\gameLibraryConfigs\tracker\torrents
```

> **Windows:** Share these folders in Docker Desktop → Settings → Resources → File Sharing.

## 5. Build & run

### Option A: Quick (using Makefile)

```bash
make all
```

### Option B: Step by step

```bash
# 1. Build backend
mvn clean package -DskipTests

# 2. Build frontend
cd frontend && npm install && npm run build && cd ..

# 3. Start all services
docker compose up --build -d
```

## 6. Verify it works

Open `http://localhost` in your browser.

Check containers:
```bash
docker compose ps
```

All 4 services should show `Up`.

## 7. First login

| Field | Value |
|-------|-------|
| Login | `admin` |
| Password | `password` |

**Change password immediately** via profile menu → "Change Password".

## 8. Add games

### Option A: Scan filesystem

Place games in `/mnt/nas/gameLibrary/games/<Platform>/<Game Name>/`, then go to **Admin** → **Scan** → **Start Scan**.

### Option B: Add manually + scrape

1. Go to Library → add game (or visit `/game/0/edit`)
2. Fill in name, platform, year
3. Select a scraper (Playground, Steam, IGDB, etc.) and click **Scrape**
4. Save

## 9. Configure scrapers (IGDB, TheGamesDB)

### IGDB

1. Create app at https://dev.twitch.tv/console/apps/create
2. Copy **Client-ID**, create **Client Secret**
3. Get access token:
   ```bash
   curl -X POST "https://id.twitch.tv/oauth2/token?client_id=YOUR_ID&client_secret=YOUR_SECRET&grant_type=client_credentials"
   ```
4. Admin panel (`/admin/scrapers`) → IGDB → set `Client-ID` and `encryptedApiKey`

### TheGamesDB

1. Register at https://thegamesdb.net/register.php
2. Get key at https://api.thegamesdb.net/key.php
3. Admin panel → TheGamesDB → set `encryptedApiKey`

## 10. Common issues

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `docker compose` not found | Old Docker | Use `docker-compose` (with hyphen) or update Docker |
| Containers crash | Empty `.env` | Fill all 4 variables in `.env` |
| Page not opening (Connection refused) | Containers not running | `docker compose ps`, check logs with `docker compose logs -f` |
| 403 Forbidden on API | CORS | Add `CORS_ALLOWED_ORIGINS=http://localhost` to `.env` and restart |
| Can't open admin panel | Not admin | Login as `admin` / `password` |
| Scraper finds nothing | No API key | Configure IGDB or TheGamesDB |
| No P2P transfer | uTP disabled | Edit `settings.json`: add `"preferred_transports": ["utp", "tcp"]` |

---

## Important notes

- Change the `admin` password immediately after first login.
- **Never commit `.env`** to Git (it's already in `.gitignore`).
- If port 80 is busy, change `"80:80"` to `"8080:80"` in `docker-compose.yml`.
- Restart containers after changing `.env`: `docker compose down && docker compose up -d`.
