\c "game-library"

create sequence if not exists library.favorite_game_id_seq start 1;
create table if not exists library.favorite_game
(
    id         bigserial primary key,
    user_id    bigint  not null references library.library_user (id),
    game_id    bigint  not null references library.game_data (id),
    created_at timestamp without time zone default now(),
    unique(user_id, game_id)
);

GRANT ALL ON library.favorite_game TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
