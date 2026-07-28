-- ============================================================
-- 07_comment.sql — Комментарии к играм
-- Пользователи могут оставлять комментарии под играми.
-- ============================================================
\c "game-library"

-- Таблица комментариев с поддержкой дат создания и обновления
create sequence if not exists library.game_comment_id_seq start 1;
create table if not exists library.game_comment
(
    id         bigserial primary key,            -- Уникальный идентификатор комментария
    game_id    bigint  not null references library.game_data (id),     -- Идентификатор игры
    user_id    bigint  not null references library.library_user (id),  -- Идентификатор автора
    text       text    not null,                 -- Текст комментария
    created_at timestamp without time zone default now(),              -- Дата создания
    updated_at timestamp without time zone default now()               -- Дата последнего изменения
);

GRANT ALL ON library.game_comment TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
