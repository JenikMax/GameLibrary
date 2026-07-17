\c "game-library"

create sequence if not exists library.notification_id_seq start 1;
create table if not exists library.notification
(
    id         bigserial primary key,
    user_id    bigint  not null references library.library_user (id),
    type       varchar(50) not null,
    title      varchar(255) not null,
    message    text,
    game_id    bigint references library.game_data (id),
    is_read    boolean not null default false,
    created_at timestamp without time zone default now()
);

GRANT ALL ON library.notification TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
