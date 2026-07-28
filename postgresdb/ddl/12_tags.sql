-- ============================================================
-- 12_tags.sql — Теги для игр
-- Словарь тегов (game_tag) и связь M:N с играми (game_data_tag).
-- ============================================================
\c "game-library"

-- ============================================================
-- game_tag — справочник тегов
-- ============================================================
CREATE TABLE IF NOT EXISTS library.game_tag (
    code VARCHAR(50) NOT NULL PRIMARY KEY,    -- Код тега (латиница, ключ)
    description VARCHAR(200) NOT NULL,        -- Описание на английском
    description_ru VARCHAR(200) NOT NULL      -- Описание на русском
);

-- ============================================================
-- game_data_tag — связь M:N между играми и тегами
-- ============================================================
CREATE SEQUENCE IF NOT EXISTS library.game_data_tag_id_seq START 1;

CREATE TABLE IF NOT EXISTS library.game_data_tag (
    id BIGINT PRIMARY KEY DEFAULT nextval('library.game_data_tag_id_seq'),  -- Уникальный идентификатор
    game_id BIGINT NOT NULL REFERENCES library.game_data(id) ON DELETE CASCADE,  -- Игра
    tag_code VARCHAR(50) NOT NULL REFERENCES library.game_tag(code) ON DELETE CASCADE,  -- Тег
    UNIQUE(game_id, tag_code)               -- Один тег на игру может быть только раз
);

-- ============================================================
-- Индексы
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_game_data_tag_game ON library.game_data_tag(game_id);
CREATE INDEX IF NOT EXISTS idx_game_data_tag_code ON library.game_data_tag(tag_code);

-- ============================================================
-- Права доступа
-- ============================================================
GRANT ALL ON library.game_tag TO "library-manager-user";
GRANT ALL ON library.game_data_tag TO "library-manager-user";
GRANT ALL ON SEQUENCE library.game_data_tag_id_seq TO "library-manager-user";
