\c "game-library"

ALTER TABLE library.game_data RENAME COLUMN description_en TO description_translated;

UPDATE library.game_data SET embedding = NULL;
