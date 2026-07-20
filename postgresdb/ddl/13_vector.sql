\c "game-library"

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA library;

ALTER TABLE library.game_data ADD COLUMN IF NOT EXISTS embedding library.vector(384);

ALTER TABLE library.game_data ADD COLUMN IF NOT EXISTS description_en text;

CREATE INDEX IF NOT EXISTS idx_game_data_embedding
    ON library.game_data USING hnsw (embedding library.vector_cosine_ops);
