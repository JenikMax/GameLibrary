-- ============================================================
-- 09_collection.sql — Коллекции игр
-- Пользовательские подборки игр (плейлисты).
-- Содержит: game_collection (подборки) и
--           game_collection_entry (игры внутри подборок).
-- ============================================================
\c "game-library"

-- ============================================================
-- Таблица game_collection — подборка игр
-- ============================================================
create sequence library.game_collection_id_seq start 1;
drop table if exists library.game_collection cascade;
create table library.game_collection
(
    id          bigint primary key default nextval('library.game_collection_id_seq'), -- Уникальный идентификатор
    name        varchar(200) not null,              -- Название подборки
    description text,                               -- Описание подборки
    user_id     bigint not null references library.library_user(id) on delete cascade,  -- Владелец
    is_public   boolean not null default false,     -- Публичная ли подборка
    created_at  timestamp without time zone default now(),  -- Дата создания
    updated_at  timestamp without time zone default now()   -- Дата последнего изменения
);

-- ============================================================
-- Таблица game_collection_entry — игра в подборке
-- ============================================================
create sequence library.game_collection_entry_id_seq start 1;
drop table if exists library.game_collection_entry cascade;
create table library.game_collection_entry
(
    id            bigint primary key default nextval('library.game_collection_entry_id_seq'), -- Уникальный идентификатор
    collection_id bigint not null references library.game_collection(id) on delete cascade,  -- Подборка
    game_id       bigint not null references library.game_data(id) on delete cascade,        -- Игра
    sort_order    int not null default 0,              -- Порядок сортировки внутри подборки
    added_at      timestamp without time zone default now(),  -- Дата добавления
    unique(collection_id, game_id)                    -- Одна игра в подборке может быть только один раз
);

-- ============================================================
-- Индексы для быстрого поиска
-- ============================================================
create index idx_collection_entry_collection on library.game_collection_entry(collection_id);
create index idx_collection_entry_game on library.game_collection_entry(game_id);
create index idx_collection_user on library.game_collection(user_id);

GRANT ALL ON ALL TABLES IN SCHEMA library TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
