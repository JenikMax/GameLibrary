\c "game-library"

create sequence library.game_collection_id_seq start 1;
drop table if exists library.game_collection cascade;
create table library.game_collection
(
    id          bigint primary key default nextval('library.game_collection_id_seq'),
    name        varchar(200) not null,
    description text,
    user_id     bigint not null references library.library_user(id) on delete cascade,
    is_public   boolean not null default false,
    created_at  timestamp without time zone default now(),
    updated_at  timestamp without time zone default now()
);

create sequence library.game_collection_entry_id_seq start 1;
drop table if exists library.game_collection_entry cascade;
create table library.game_collection_entry
(
    id            bigint primary key default nextval('library.game_collection_entry_id_seq'),
    collection_id bigint not null references library.game_collection(id) on delete cascade,
    game_id       bigint not null references library.game_data(id) on delete cascade,
    sort_order    int not null default 0,
    added_at      timestamp without time zone default now(),
    unique(collection_id, game_id)
);

create index idx_collection_entry_collection on library.game_collection_entry(collection_id);
create index idx_collection_entry_game on library.game_collection_entry(game_id);
create index idx_collection_user on library.game_collection(user_id);

GRANT ALL ON ALL TABLES IN SCHEMA library TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
