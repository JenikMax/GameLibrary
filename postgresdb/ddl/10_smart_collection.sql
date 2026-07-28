-- ============================================================
-- 10_smart_collection.sql — Умные коллекции
-- Добавляет поля is_smart и smart_rules в таблицу
-- game_collection для поддержки динамических подборок.
-- ============================================================
\c "game-library"

-- Флаг: является ли коллекция умной (динамической)
ALTER TABLE library.game_collection ADD COLUMN IF NOT EXISTS is_smart BOOLEAN NOT NULL DEFAULT false;
-- JSON-правила для автоматического наполнения коллекции
ALTER TABLE library.game_collection ADD COLUMN IF NOT EXISTS smart_rules TEXT;

GRANT ALL ON library.game_collection TO "library-manager-user";
