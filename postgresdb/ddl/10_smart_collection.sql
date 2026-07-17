\c "game-library"

ALTER TABLE library.game_collection ADD COLUMN IF NOT EXISTS is_smart BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE library.game_collection ADD COLUMN IF NOT EXISTS smart_rules TEXT;

GRANT ALL ON library.game_collection TO "library-manager-user";
