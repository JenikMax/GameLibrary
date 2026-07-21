\c "game-library"

DROP INDEX IF EXISTS library.idx_game_data_embedding;

ALTER TABLE library.game_data
    ALTER COLUMN embedding TYPE library.vector(384)
    USING embedding::text::library.vector(384);

UPDATE library.game_data SET embedding = NULL;

CREATE INDEX IF NOT EXISTS idx_game_data_embedding
    ON library.game_data USING hnsw (embedding library.vector_cosine_ops);
