\c "game-library"
--SET search_path TO library;

create sequence game_data_id_seq start 1;
drop table if exists library.game_data;
create table library.game_data
(
    id                bigserial primary key,
    create_ts         timestamp without time zone,
    name              varchar(225),
    release_date      varchar(20),
    directory_path    varchar(255),
    trailer_url       varchar(255),
    platform          varchar(225),
    description       text,
    instruction       text,
    logo              bytea
);

drop table if exists library.game_genre;
create table library.game_genre
(
    code        varchar(50)  not null,
    description varchar(200) not null,
    constraint game_genre_pkey primary key (code)
);

COMMENT ON TABLE library.game_genre IS '';
COMMENT ON COLUMN library.game_genre.code IS '';
COMMENT ON COLUMN library.game_genre.description IS '';

INSERT INTO library.game_genre (code, description) VALUES ('RPG', 'rpg');
INSERT INTO library.game_genre (code, description) VALUES ('ACTION', 'action');
INSERT INTO library.game_genre (code, description) VALUES ('JRPG', 'jrpg');
INSERT INTO library.game_genre (code, description) VALUES ('GAME_GENRE_4', '');


create sequence game_data_genre_id_seq start 1;
drop table if exists library.game_data_genre;
create table library.game_data_genre
(
    id bigserial primary key,
    game_id      bigint           not null references library.game_data (id),
    genre_code   varchar(200)      not null references library.game_genre (code)
);


create sequence game_screenshot_id_seq start 1;
drop table if exists library.game_screenshot;
create table library.game_screenshot
(
    id bigserial primary key,
    game_id      bigint           not null references library.game_data (id),
    name        varchar(200)  not null,
    source              bytea
);


drop view if exists library.v_platform;
create or replace view library.v_platform as
select platform from library.game_data group by platform;

drop view if exists library.v_release_date;
create or replace view library.v_release_date as
select release_date from library.game_data group by release_date;


GRANT ALL ON ALL TABLES IN SCHEMA library TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO "library-manager-user";