-- ============================================================
-- 05_rating.sql — Оценки игр пользователями
-- Каждый пользователь может поставить одну оценку от 1 до 10.
-- ============================================================
\c "game-library"

-- Таблица оценок (рейтинг 1-10, уникальность по паре игра+пользователь)
create sequence if not exists library.game_rating_id_seq start 1;
create table if not exists library.game_rating
(
    id        bigserial primary key,             -- Уникальный идентификатор оценки
    game_id   bigint  not null references library.game_data (id),  -- Идентификатор игры
    user_id   bigint  not null references library.library_user (id), -- Идентификатор пользователя
    rating    integer not null check (rating >= 1 and rating <= 10), -- Оценка от 1 до 10
    created_at timestamp without time zone default now(),           -- Дата и время оценки
    unique(game_id, user_id)                    -- Одна оценка на пользователя на игру
);

GRANT ALL ON library.game_rating TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
