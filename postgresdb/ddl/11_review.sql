-- ============================================================
-- 11_review.sql — Рецензии на игры
-- Развёрнутые отзывы с оценками по 4 категориям (1-10)
-- и полями для плюсов/минусов.
-- ============================================================
\c "game-library"

-- Таблица рецензий (одна рецензия на пользователя на игру)
CREATE SEQUENCE IF NOT EXISTS library.game_review_id_seq START 1;

CREATE TABLE IF NOT EXISTS library.game_review (
    id BIGINT PRIMARY KEY DEFAULT nextval('library.game_review_id_seq'),  -- Уникальный идентификатор
    game_id BIGINT NOT NULL REFERENCES library.game_data(id) ON DELETE CASCADE,  -- Игра
    user_id BIGINT NOT NULL REFERENCES library.library_user(id) ON DELETE CASCADE, -- Автор
    text TEXT,                          -- Текст рецензии
    pros TEXT,                          -- Плюсы (перечисление)
    cons TEXT,                          -- Минусы (перечисление)
    gameplay_score INT CHECK (gameplay_score >= 1 AND gameplay_score <= 10),  -- Оценка геймплея
    graphics_score INT CHECK (graphics_score >= 1 AND graphics_score <= 10),  -- Оценка графики
    story_score INT CHECK (story_score >= 1 AND story_score <= 10),          -- Оценка сюжета
    music_score INT CHECK (music_score >= 1 AND music_score <= 10),          -- Оценка музыки
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),                      -- Дата создания
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),                      -- Дата обновления
    UNIQUE(game_id, user_id)            -- Одна рецензия на пользователя на игру
);

-- Индексы для поиска рецензий по игре и по пользователю
CREATE INDEX IF NOT EXISTS idx_game_review_game ON library.game_review(game_id);
CREATE INDEX IF NOT EXISTS idx_game_review_user ON library.game_review(user_id);

GRANT ALL ON library.game_review TO "library-manager-user";
GRANT ALL ON SEQUENCE library.game_review_id_seq TO "library-manager-user";
