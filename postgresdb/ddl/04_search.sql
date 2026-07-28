-- ============================================================
-- 04_search.sql — Полнотекстовый поиск по играм
-- Добавляет tsvector-колонку search_vector, триггер
-- автоматического обновления и GIN-индекс.
-- ============================================================
\c "game-library"

-- Вектор полнотекстового поиска (русский язык)
ALTER TABLE library.game_data ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Функция-триггер: автоматически обновляет search_vector
-- Вес A — название, вес B — описание
CREATE OR REPLACE FUNCTION library.game_data_search_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('russian', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('russian', COALESCE(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггер срабатывает до вставки или обновления записи
DROP TRIGGER IF EXISTS trg_game_data_search ON library.game_data;
CREATE TRIGGER trg_game_data_search
    BEFORE INSERT OR UPDATE ON library.game_data
    FOR EACH ROW EXECUTE FUNCTION library.game_data_search_update();

-- Перестроение поисковых векторов для существующих записей
UPDATE library.game_data
SET search_vector =
    setweight(to_tsvector('russian', COALESCE(name, '')), 'A') ||
    setweight(to_tsvector('russian', COALESCE(description, '')), 'B');

-- GIN-индекс для быстрого полнотекстового поиска
CREATE INDEX IF NOT EXISTS idx_game_data_search ON library.game_data USING GIN(search_vector);
