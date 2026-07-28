-- ============================================================
-- 08_notification.sql — Уведомления пользователей
-- Системные уведомления: новые комментарии, рейтинги и т.д.
-- ============================================================
\c "game-library"

-- Таблица уведомлений с типом, заголовком и флагом прочтения
create sequence if not exists library.notification_id_seq start 1;
create table if not exists library.notification
(
    id         bigserial primary key,             -- Уникальный идентификатор уведомления
    user_id    bigint  not null references library.library_user (id),  -- Получатель уведомления
    type       varchar(50) not null,              -- Тип уведомления (comment, rating, review и т.д.)
    title      varchar(255) not null,             -- Заголовок уведомления
    message    text,                              -- Текст уведомления
    game_id    bigint references library.game_data (id),  -- Связанная игра (если применимо)
    is_read    boolean not null default false,    -- Флаг прочтения
    created_at timestamp without time zone default now()  -- Дата и время создания
);

GRANT ALL ON library.notification TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
