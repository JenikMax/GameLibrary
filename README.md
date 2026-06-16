# GameLibrary

Каталогизатор компьютерных игр. Приложение сканирует файловую систему, формирует библиотеку с метаданными и предоставляет веб-интерфейс для просмотра, поиска, редактирования и скачивания игр.

## Технологический стек

| Компонент | Технология |
|-----------|-----------|
| Backend | Spring Boot 2.7.1, Java 1.8 |
| База данных | PostgreSQL (схема `library`) |
| ORM / JDBC | Hibernate 5.6, Spring Data JPA, Commons DBCP 1.4 |
| Шаблонизатор | Thymeleaf, Spring Security integration |
| Безопасность | Spring Security (form login, BCrypt, роли ADMIN/USER) |
| Сборка | Maven, WAR packaging |
| Развёртывание | Tomcat 8 (внешний или через Docker) |
| Torrent | ttorrent-core (встроенный трекер) |
| Сеть | OkHttp 3, Jsoup, HtmlUnit (скрапинг) |

## Архитектура

### Структура файлового хранилища

```
<games_directory>/games/
└── <platform>/                       (например: PC, PlayStation, Xbox)
    └── <game_name>/                  (название каталога = название игры)
        ├── <файлы игры>...
        └── information/              (создаётся при сканировании)
            ├── logo.jpg              (постер; по умолчанию — default.jpg)
            ├── information.json      (название, год, жанры, описание, трейлер, инструкция)
            └── img/                  (скриншоты .jpg)
```

При сканировании приложение обходит структуру `games/<platform>/<game_name>/`. Если каталог `information/` отсутствует, он создаётся и заполняется значениями по умолчанию: название берётся из имени каталога, платформа — из родительского каталога, логотип — изображение по умолчанию.

### Пакетная структура backend

```
com.jenikmax.game.library
├── GameLibraryAppApplication.java      — точка входа Spring Boot
├── ServletInitializer.java             — WAR-инициализатор для внешнего Tomcat
│
├── config/
│   ├── SecurityConfig.java             — form login, BCrypt, RBAC (ADMIN/USER)
│   ├── DatabaseConfig.java             — DataSource (BasicDataSource)
│   ├── WebMvcConfig.java               — Thymeleaf, i18n, locale resolver
│   ├── TrackerConfig.java              — встроенный BitTorrent-трекер (ttorrent-core)
│   └── AppConfig.java                  — JdbcTemplate, NamedParameterJdbcTemplate
│
├── controller/
│   └── view/
│       ├── MainViewController.java     — GET / → redirect:/library
│       ├── LibraryViewController.java   — /library, /library/game/{id}, фильтры, скачивание
│       ├── UserViewController.java      — /login, /register, /profile
│       └── ErrorViewController.java     — /error403
│
├── dao/
│   ├── api/ (GameRepository, UserRepository, SqlDao и др.)
│   └── SqlDaoImpl.java                 — raw SQL через JdbcTemplate
│
├── model/
│   ├── entity/ (Game, User, GameGenre, Screenshot + enum Genre)
│   ├── dto/ (GameShortDto, GameDto, UserDto, LoginForm, RegistrationForm и др.)
│   └── converter/GameConverter.java    — Game ↔ GameDto
│
├── service/
│   ├── LibraryOperationService.java     — оркестратор: сканирование, CRUD, скачивание, граббинг
│   ├── data/
│   │   ├── GameDataService.java         — CRUD игр + фильтрация/сортировка через raw SQL
│   │   ├── UserDataService.java         — CRUD пользователей
│   │   └── LibraryUserDetailsService.java — UserDetailsService для Spring Security
│   ├── scraper/
│   │   ├── ScraperFactory.java          — регистрация scraper-ов по типу
│   │   ├── Scraper.java / ScrapInfo.java
│   │   └── scrapers/ (Steam, MobyGames, IGDB, Igromania, TheGameDB, Playground, WorldArt)
│   ├── downloads/
│   │   ├── DownloadFileService.java     — упаковка в ZIP / формирование .torrent
│   │   └── DownloadTorrentService.java  — создание .torrent + регистрация в трекере
│   └── scaner/
│       └── GameScanerService.java       — обход ФС, чтение/запись information.json
```

### Frontend

Шаблоны: `src/main/resources/templates/`. I18n: `src/main/resources/msg/messages_{en,ru}.properties`.

| URL | Доступ | Описание |
|-----|--------|----------|
| `/login` | все | Форма входа |
| `/register` | все | Регистрация (username 3–20 символов, пароль ≥6 символов) |
| `/library` | USER, ADMIN | Список игр (постеры), пагинация (12 шт.), сортировка, фильтры |
| `/library/game/{id}` | USER, ADMIN | Детальная карточка игры: постер, описание, скриншоты, трейлер, инструкция, скачивание |
| `/library/game/{id}/edit` | ADMIN | Редактирование + граббинг метаданных |
| `/library/game/{id}/download` | USER, ADMIN | Скачивание (ZIP или .torrent) |
| `/profile` | USER | Профиль: смена аватара и пароля |
| `/profile` | ADMIN | Админ-панель: управление пользователями (роль, активность, сброс пароля) |
| `/error403` | все | Страница «Доступ запрещён» |

## Функциональные возможности

### Сканирование библиотеки (только ADMIN)
- Обход структуры `<games>/<platform>/<game_name>/`
- Создание `information/` с `information.json`, `logo.jpg`, `img/` при отсутствии
- Добавление новых игр в БД и удаление записей об отсутствующих на диске
- Чтение метаданных из `information.json` и замена умолчаний

### Просмотр и фильтрация
- Отображение списка игр с постерами, названием, платформой, годом
- Фильтрация по платформе, жанру, названию
- Сортировка по названию, году выпуска, дате добавления
- Пагинация (12 игр на страницу)
- Игры без жанра выносятся в отдельную категорию

### Редактирование игр (только ADMIN)
- Изменение названия, года выпуска, описания
- Загрузка/удаление постера (логотипа)
- Добавление/удаление жанров из общего справочника
- Загрузка/удаление скриншотов
- Изменение ссылки на YouTube-трейлер
- Редактирование инструкции по установке

### Сбор метаданных (Grab)
Ручной выбор источника в редакторе игры:
- **Playground.ru** — парсинг страницы статьи
- **Igromania.ru** — парсинг страницы статьи

Возможность выборочного сбора: название, постер, описание, год, жанры, скриншоты.
Реализована инфраструктура для Steam, MobyGames, IGDB, TheGamesDB, WorldArt (единый интерфейс `Scraper`).

### Скачивание игр
- **Малые игры (<1 ГБ)**: потоковая упаковка в ZIP и отдача клиенту
- **Крупные игры (>1 ГБ)**: создание .torrent-файла с announce на встроенный трекер, отдача клиенту + регистрация в трекере и копирование в watch-директорию Deluge для раздачи

### Пользователи и роли
- **ADMIN**: сканирование библиотеки, редактирование игр, граббинг, управление пользователями
- **USER**: просмотр библиотеки, скачивание, редактирование своего профиля
- Встроенные учётные записи: `admin` / `guest` (BCrypt-пароли в seed DDL)

### Локализация
Русский и английский языки. Переключение через параметр `?lang=ru` или `?lang=en`. Имена жанров локализованы.

## База данных (PostgreSQL, схема `library`)

| Таблица / Представление | Назначение |
|---|---|
| `game_data` | Игры (id, name, platform, release_date, description, instruction, trailer_url, logo bytea, directory_path) |
| `game_genre` | Справочник жанров (code, description, description_ru) — ~70 жанров |
| `game_data_genre` | M:N связь игры и жанров |
| `game_screenshot` | Скриншоты (bytea) |
| `library_user` | Пользователи (user_name, pass BCrypt, is_admin, is_active, avatar bytea) |
| `v_platform` | Представление — список платформ |
| `v_release_date` | Представление — список годов выпуска |

Три DDL-файла в `postgresdb/ddl/`:
1. `1_init.sql` — пользователь БД `library-manager-user`, схема `library`
2. `2_library.sql` — таблицы библиотеки + жанры
3. `3_user.sql` — таблица пользователей + admin/guest

## Разработка и запуск

```bash
mvn clean package                            # → target/game-library.war
mvn spring-boot:run -Dspring.profiles.active=alone   # локально (нужен PostgreSQL)
docker compose up -d                         # полное окружение (postgres + deluge + app)
```

Подробнее о конфигурации и подводных камнях — см. `AGENTS.md`.
