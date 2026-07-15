\c "game-library"

create sequence if not exists library.game_rating_id_seq start 1;
create table if not exists library.game_rating
(
    id        bigserial primary key,
    game_id   bigint  not null references library.game_data (id),
    user_id   bigint  not null references library.library_user (id),
    rating    integer not null check (rating >= 1 and rating <= 10),
    created_at timestamp without time zone default now(),
    unique(game_id, user_id)
);

GRANT ALL ON library.game_rating TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
