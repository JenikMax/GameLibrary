-- ============================================================
-- 14_emulator_save.sql — Сейвы браузерного эмулятора (EmulatorJS)
-- Серверное хранение сейвов/состояний: srm (батарейка) и state (снимок).
-- Один слот на (игра, пользователь, kind, slot).
-- ============================================================
\c "game-library"

create sequence if not exists library.emulator_save_id_seq start 1;
create table if not exists library.emulator_save
(
    id         bigserial primary key,                                   -- Уникальный идентификатор сейва
    game_id    bigint       not null references library.game_data (id), -- Игра
    user_id    bigint       not null references library.library_user (id), -- Пользователь (сейвы per-user)
    kind       varchar(10)  not null check (kind in ('srm', 'state')),  -- srm — батарейка, state — снимок состояния
    slot       int          not null default 0,                         -- Номер слота
    name       varchar(255),                                            -- Имя файла сейва (опционально)
    data       bytea,                                                   -- Содержимое сейва
    size_bytes bigint       not null default 0,                         -- Размер в байтах
    updated_at timestamp without time zone default now(),               -- Дата последнего сохранения
    unique (game_id, user_id, kind, slot)                               -- Один слот на пару игра+пользователь
);

GRANT ALL ON library.emulator_save TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
