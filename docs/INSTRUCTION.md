# GameLibrary — Пошаговая инструкция по развёртыванию

> Полное руководство для тех, кто впервые запускает приложение.
> Если что-то пошло не так — смотрите раздел [Типовые проблемы](#10-типовые-проблемы).

---

## 1. Что нужно для запуска

| Программа | Версия | Зачем |
|-----------|--------|-------|
| **Git** | любая | скачать проект |
| **Docker** | 19.03+ | запуск контейнеров |
| **Docker Compose** | входит в Docker | управление сервисами |

**Необязательно** (только при сборке вручную): Java 25 (JDK), Maven 3.6+, Node.js 18+.

Проверьте:

```bash
git --version
docker --version
docker compose version
```

Установка при отсутствии:

```bash
# Ubuntu / Debian
sudo apt install git docker.io docker-compose-v2

# Windows — Docker Desktop: https://www.docker.com/products/docker-desktop/
# macOS — Docker Desktop или: brew install --cask docker
```

---

## 2. Где взять проект

```bash
git clone <repository-url>
cd GameLibrary
```

---

## 3. Настройка секретов (.env)

```bash
cp .env.example .env
```

Откройте `.env` и **обязательно** заполните 4 поля:

```
POSTGRES_PASSWORD=придумайте_пароль_для_postgres
DB_PASSWORD=придумайте_пароль_для_приложения
JWT_SECRET=openssl rand -hex 32
SCRAPER_ENCRYPTION_KEY=openssl rand -base64 32
```

Генерация надёжных значений:

```bash
openssl rand -hex 32          # JWT_SECRET
openssl rand -base64 32       # SCRAPER_ENCRYPTION_KEY
```

> Не используйте пароли из примеров. Минимум 12 символов, буквы + цифры.

---

## 4. Создание структуры папок

```bash
mkdir -p /mnt/nas/gameLibrary/{games,images,gameLibraryConfigs/{db/data,scrapers,tracker/{config,watch,complete,incomplete,torrents}}}
```

Результат:

```
/mnt/nas/gameLibrary/
├── games/                          # сюда кладёте папки с играми
├── images/                         # обложки и скриншоты (создадутся сами)
└── gameLibraryConfigs/
    ├── db/data/                    # файлы БД (создадутся сами)
    ├── scrapers/                   # scrapers-config.json
    └── tracker/
        ├── config/                 # settings.json Transmission
        ├── watch/                  # авто-подхват .torrent
        ├── complete/               # завершённые загрузки
        ├── incomplete/             # незавершённые загрузки
        └── torrents/               # .torrent от GameLibrary
```

**Windows (PowerShell):**

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

> Docker Desktop → Settings → Resources → File Sharing — добавить `D:\GameLibrary`.

---

## 5. Сборка и запуск

### Способ A: Быстрый (Makefile)

```bash
make all
```

### Способ B: Пошагово

```bash
# 1. Backend
mvn clean package -DskipTests

# 2. Frontend
cd frontend && npm install && npm run build && cd ..

# 3. Docker
docker compose up --build -d
```

**Если Maven/Node.js не установлены** — Docker соберёт всё сам при `docker compose up --build`.

Проверка:

```bash
docker compose ps
```

Все 5 сервисов должны быть `Up`:

```
NAME                      STATUS
game-library-backend      Up
game-library-frontend     Up
game-library-db           Up
game-library-ai-service   Up
game-library-transmission Up
```

### Отключение AI-сервиса (экономия ресурсов)

Если AI не нужен:

1. Закомментируйте блок `ai-service:` в `docker-compose.yml`
2. Замените `pgvector/pgvector:pg16` на `postgres:16` в `postgresdb/Dockerfile` (опционально)
3. Перезапустите: `docker compose up -d`

Без ai-service работает всё, кроме: семантический поиск, перевод ru↔en.

---

## 6. Проверка — всё работает?

Откройте браузер:

```
http://localhost:8090
```

На NAS или другом компьютере — замените `localhost` на IP-адрес:

```
http://192.168.1.100:8090
```

Если не открывается — логи:

```bash
docker compose logs -f
```

---

## 7. Первый вход

| Поле | Значение |
|------|----------|
| Логин | `admin` |
| Пароль | `password` |

**Что сделать сразу:**

1. Сменить пароль: Профиль → «Сменить пароль»
2. Пользователи регистрируются сами через `/register` или создаются админом

---

## 8. Добавление игр

### Способ A: Сканирование ФС

Разложите игры по платформам:

```
/mnt/nas/gameLibrary/games/
├── PC/
│   ├── Half-Life 2/
│   │   ├── hl2.exe
│   │   └── ...
│   └── Portal/
│       └── ...
├── PlayStation/
│   └── ...
└── Xbox/
    └── ...
```

Имя папки игры = название в библиотеке.

Затем: Администрирование → Сканирование → «Запустить сканирование».

### Способ B: Вручную + скрапинг

1. `/game/0/edit` — страница создания игры
2. Заполните название, платформу, год
3. Выберите скрапер → «Скрапить» → описание, жанры, скриншоты подтянутся
4. «Сохранить»

---

## 9. Настройка скраперов

Некоторые скраперы работают без ключа (Playground, Steam, World-Art, PsxDataCenter). Для IGDB и TheGamesDB нужны API-ключи.

### IGDB (через Twitch)

1. https://dev.twitch.tv/console/apps/create
2. Создайте приложение (OAuth Redirect: `http://localhost`)
3. Скопируйте **Client-ID**
4. **New Secret** → скопируйте **Client Secret**
5. Получите токен:
   ```bash
   curl -X POST "https://id.twitch.tv/oauth2/token?client_id=ID&client_secret=SECRET&grant_type=client_credentials"
   ```
6. Админка → `/admin/scrapers` → IGDB:
   - `headers.Client-ID` = Client-ID
   - `encryptedApiKey` = access_token из ответа curl

### TheGamesDB

1. https://thegamesdb.net/register.php → подтвердить email
2. https://api.thegamesdb.net/key.php → скопировать ключ
3. Админка → TheGamesDB → `encryptedApiKey`

### PsxDataCenter

Не требует ключа. Работает для PS1 и PS2. Поддерживает поиск по серийному номеру (SLUS-12345, SCES-54321).

---

## 10. Типовые проблемы

| Симптом | Причина | Что делать |
|---------|---------|------------|
| `docker compose` не найдена | Старая версия Docker | `docker-compose` (с дефисом) или обновить Docker |
| Контейнеры падают | Не заполнен `.env` | Проверить все 4 обязательные переменные |
| Connection refused | Контейнеры не запущены | `docker compose ps`, `docker compose logs -f` |
| 403 Forbidden (API) | CORS | `.env` → `CORS_ALLOWED_ORIGINS=http://localhost` |
| Не открывается админка | Не админ | Войти как `admin` / `password` |
| Скрапер не находит игру | Нет API-ключа | Настроить IGDB или TheGamesDB (раздел 9) |
| Торрент на 0% / не качается | uTP выключен | `settings.json` → `"preferred_transports": ["utp", "tcp"]` |
| БД не инициализируется | CRLF в SQL | `git config core.autocrlf false` |
| AI не работает | Сервис не запущен | Проверить `docker compose ps` — ai-service Up? |

---

## Важные замечания

- **Смените пароль admin сразу** после первого входа
- `.env` содержит пароли — не коммитить (уже в `.gitignore`)
- Порт 8090 занят? — измените `"8090:80"` в `docker-compose.yml`
- После изменения `.env`: `docker compose down && docker compose up -d`

---

## Глоссарий

| Термин | Значение |
|--------|----------|
| **Backend** | Сервер на Java — обрабатывает запросы, работает с БД |
| **Frontend** | Веб-интерфейс на Vue.js в браузере |
| **Docker** | Система контейнеризации |
| **Docker Compose** | Запуск нескольких контейнеров |
| **Скрапер** | Сбор информации об игре с сайтов |
| **Transmission** | Торрент-клиент для раздачи игр |
| **PostgreSQL** | База данных |
| **uTP** | P2P-протокол (нужен для uTorrent) |
| **JWT** | Токен авторизации |
| **pgvector** | Расширение PostgreSQL для векторного поиска |
