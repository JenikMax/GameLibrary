-- ============================================================
-- 13_vector.sql — Векторные эмбеддинги (pgvector + HNSW)
-- Семантический поиск и перевод описаний через AI-сервис.
-- Требует расширение pgvector и pgvector/pgvector:pg16.
-- ============================================================
\c "game-library"

-- Расширение pgvector для работы с векторными эмбеддингами
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA library;

-- Колонка эмбеддинга (384-мерный вектор от intfloat/multilingual-e5-small)
ALTER TABLE library.game_data ADD COLUMN IF NOT EXISTS embedding library.vector(384);

-- Кэш перевода описания (ru↔en через AI-сервис)
ALTER TABLE library.game_data ADD COLUMN IF NOT EXISTS description_translated text;

-- HNSW-индекс для быстрого косинусного поиска ближайших соседей
CREATE INDEX IF NOT EXISTS idx_game_data_embedding
    ON library.game_data USING hnsw (embedding library.vector_cosine_ops);
