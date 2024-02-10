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

INSERT INTO library.game_genre (code, description) VALUES ('rpg','Ролевая');
INSERT INTO library.game_genre (code, description) VALUES ('action','Экшен');
INSERT INTO library.game_genre (code, description) VALUES ('moba','moba');
INSERT INTO library.game_genre (code, description) VALUES ('rts','RTS');
INSERT INTO library.game_genre (code, description) VALUES ('humour','Юмор');
INSERT INTO library.game_genre (code, description) VALUES ('economy','Экономика');
INSERT INTO library.game_genre (code, description) VALUES ('shooter','Шутер');
INSERT INTO library.game_genre (code, description) VALUES ('hockey','Хоккей');
INSERT INTO library.game_genre (code, description) VALUES ('fantasy','Фэнтези');
INSERT INTO library.game_genre (code, description) VALUES ('football','Футбол');
INSERT INTO library.game_genre (code, description) VALUES ('fighting','Файтинг');
INSERT INTO library.game_genre (code, description) VALUES ('free_to_play','Условно-бесплатная');
INSERT INTO library.game_genre (code, description) VALUES ('horror','Ужасы');
INSERT INTO library.game_genre (code, description) VALUES ('dance','Танцы');
INSERT INTO library.game_genre (code, description) VALUES ('tactics','Тактика');
INSERT INTO library.game_genre (code, description) VALUES ('construction','Строительство');
INSERT INTO library.game_genre (code, description) VALUES ('strategy','Стратегия');
INSERT INTO library.game_genre (code, description) VALUES ('steampunk','Стимпанк');
INSERT INTO library.game_genre (code, description) VALUES ('stealth','Стелс');
INSERT INTO library.game_genre (code, description) VALUES ('medieval','Средневековье');
INSERT INTO library.game_genre (code, description) VALUES ('sport','Спорт');
INSERT INTO library.game_genre (code, description) VALUES ('slasher','Слэшер');
INSERT INTO library.game_genre (code, description) VALUES ('simulators','Симулятор');
INSERT INTO library.game_genre (code, description) VALUES ('retro','Ретро');
INSERT INTO library.game_genre (code, description) VALUES ('turn_based','Пошаговая');
INSERT INTO library.game_genre (code, description) VALUES ('post_apocalyptic','Постапокалипсис');
INSERT INTO library.game_genre (code, description) VALUES ('dungeons','Подземелья');
INSERT INTO library.game_genre (code, description) VALUES ('platform','Платформер');
INSERT INTO library.game_genre (code, description) VALUES ('sandbox','Песочница');
INSERT INTO library.game_genre (code, description) VALUES ('open_world','Открытый мир');
INSERT INTO library.game_genre (code, description) VALUES ('third_person','От третьего лица');
INSERT INTO library.game_genre (code, description) VALUES ('first_person ','От первого лица');
INSERT INTO library.game_genre (code, description) VALUES ('sci_fi','Научная фантастика');
INSERT INTO library.game_genre (code, description) VALUES ('board_game','Настольная');
INSERT INTO library.game_genre (code, description) VALUES ('online','Мультиплеер');
INSERT INTO library.game_genre (code, description) VALUES ('music','Музыка');
INSERT INTO library.game_genre (code, description) VALUES ('moto','Мотоциклы');
INSERT INTO library.game_genre (code, description) VALUES ('manager','Менеджер');
INSERT INTO library.game_genre (code, description) VALUES ('logic','Логические');
INSERT INTO library.game_genre (code, description) VALUES ('space','Космос');
INSERT INTO library.game_genre (code, description) VALUES ('battle_royale','Королевская битва');
INSERT INTO library.game_genre (code, description) VALUES ('coop','Кооператив');
INSERT INTO library.game_genre (code, description) VALUES ('cyberpunk','Киберпанк');
INSERT INTO library.game_genre (code, description) VALUES ('quest','Квест');
INSERT INTO library.game_genre (code, description) VALUES ('card','Карточная');
INSERT INTO library.game_genre (code, description) VALUES ('indie','Инди');
INSERT INTO library.game_genre (code, description) VALUES ('zombie','Зомби');
INSERT INTO library.game_genre (code, description) VALUES ('other_simulators','Другой симулятор');
INSERT INTO library.game_genre (code, description) VALUES ('adult','Для взрослых 18+');
INSERT INTO library.game_genre (code, description) VALUES ('racing','Гонки');
INSERT INTO library.game_genre (code, description) VALUES ('_4X','Глобальная стратегия');
INSERT INTO library.game_genre (code, description) VALUES ('survival','Выживание');
INSERT INTO library.game_genre (code, description) VALUES ('vr','Виртуальная реальность');
INSERT INTO library.game_genre (code, description) VALUES ('top_down','Вид сверху');
INSERT INTO library.game_genre (code, description) VALUES ('western','Вестерн');
INSERT INTO library.game_genre (code, description) VALUES ('basketball','Баскетбол');
INSERT INTO library.game_genre (code, description) VALUES ('arcade','Аркада');
INSERT INTO library.game_genre (code, description) VALUES ('anime','Аниме');
INSERT INTO library.game_genre (code, description) VALUES ('adventure','Адвенчура');
INSERT INTO library.game_genre (code, description) VALUES ('auto','Автомобили');
INSERT INTO library.game_genre (code, description) VALUES ('flight','Авиация');
INSERT INTO library.game_genre (code, description) VALUES ('tower_defence','Tower defence');
INSERT INTO library.game_genre (code, description) VALUES ('rts','RTS');
INSERT INTO library.game_genre (code, description) VALUES ('mmo','MMO');
INSERT INTO library.game_genre (code, description) VALUES ('jrpg','jRpg');


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