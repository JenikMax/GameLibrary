\c "game-library"

ALTER TABLE library.game_data ADD COLUMN IF NOT EXISTS search_vector tsvector;

CREATE OR REPLACE FUNCTION library.game_data_search_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('russian', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('russian', COALESCE(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_game_data_search ON library.game_data;
CREATE TRIGGER trg_game_data_search
    BEFORE INSERT OR UPDATE ON library.game_data
    FOR EACH ROW EXECUTE FUNCTION library.game_data_search_update();

UPDATE library.game_data
SET search_vector =
    setweight(to_tsvector('russian', COALESCE(name, '')), 'A') ||
    setweight(to_tsvector('russian', COALESCE(description, '')), 'B');

CREATE INDEX IF NOT EXISTS idx_game_data_search ON library.game_data USING GIN(search_vector);
