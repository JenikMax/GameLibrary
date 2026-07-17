\c "game-library"

CREATE TABLE IF NOT EXISTS library.game_tag (
    code VARCHAR(50) NOT NULL PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    description_ru VARCHAR(200) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS library.game_data_tag_id_seq START 1;

CREATE TABLE IF NOT EXISTS library.game_data_tag (
    id BIGINT PRIMARY KEY DEFAULT nextval('library.game_data_tag_id_seq'),
    game_id BIGINT NOT NULL REFERENCES library.game_data(id) ON DELETE CASCADE,
    tag_code VARCHAR(50) NOT NULL REFERENCES library.game_tag(code) ON DELETE CASCADE,
    UNIQUE(game_id, tag_code)
);

CREATE INDEX IF NOT EXISTS idx_game_data_tag_game ON library.game_data_tag(game_id);
CREATE INDEX IF NOT EXISTS idx_game_data_tag_code ON library.game_data_tag(tag_code);

GRANT ALL ON library.game_tag TO "library-manager-user";
GRANT ALL ON library.game_data_tag TO "library-manager-user";
GRANT ALL ON SEQUENCE library.game_data_tag_id_seq TO "library-manager-user";
