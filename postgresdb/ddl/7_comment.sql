\c "game-library"

create sequence if not exists library.game_comment_id_seq start 1;
create table if not exists library.game_comment
(
    id         bigserial primary key,
    game_id    bigint  not null references library.game_data (id),
    user_id    bigint  not null references library.library_user (id),
    text       text    not null,
    created_at timestamp without time zone default now(),
    updated_at timestamp without time zone default now()
);

GRANT ALL ON library.game_comment TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
