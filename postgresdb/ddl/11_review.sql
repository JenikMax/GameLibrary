\c "game-library"

CREATE SEQUENCE IF NOT EXISTS library.game_review_id_seq START 1;

CREATE TABLE IF NOT EXISTS library.game_review (
    id BIGINT PRIMARY KEY DEFAULT nextval('library.game_review_id_seq'),
    game_id BIGINT NOT NULL REFERENCES library.game_data(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES library.library_user(id) ON DELETE CASCADE,
    text TEXT,
    pros TEXT,
    cons TEXT,
    gameplay_score INT CHECK (gameplay_score >= 1 AND gameplay_score <= 10),
    graphics_score INT CHECK (graphics_score >= 1 AND graphics_score <= 10),
    story_score INT CHECK (story_score >= 1 AND story_score <= 10),
    music_score INT CHECK (music_score >= 1 AND music_score <= 10),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    UNIQUE(game_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_game_review_game ON library.game_review(game_id);
CREATE INDEX IF NOT EXISTS idx_game_review_user ON library.game_review(user_id);

GRANT ALL ON library.game_review TO "library-manager-user";
GRANT ALL ON SEQUENCE library.game_review_id_seq TO "library-manager-user";
