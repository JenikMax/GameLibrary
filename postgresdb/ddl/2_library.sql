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
    instruction       text
);

drop table if exists library.game_genre;
create table library.game_genre
(
    code        varchar(50)  not null,
    description varchar(200) not null,
    description_ru varchar(200) not null,
    constraint game_genre_pkey primary key (code)
);

COMMENT ON TABLE library.game_genre IS '';
COMMENT ON COLUMN library.game_genre.code IS '';
COMMENT ON COLUMN library.game_genre.description IS '';
COMMENT ON COLUMN library.game_genre.description IS '';

INSERT INTO library.game_genre (code, description, description_ru) VALUES ('rpg','RPG','Ролевая');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('action','Action','Экшен');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('moba','MOBA','MOBA');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('rts','RTS','RTS');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('humour','Humour','Юмор');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('economy','Economy','Экономика');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('shooter','Shooter','Шутер');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('hockey','Hockey','Хоккей');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('fantasy','Fantasy','Фэнтези');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('football','Football','Футбол');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('fighting','Fighting','Файтинг');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('free_to_play','Free-to-play','Условно-бесплатная');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('horror','Horror','Ужасы');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('dance','Dance','Танцы');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('tactics','Tactics','Тактика');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('construction','Construction','Строительство');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('strategy','Strategy','Стратегия');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('steampunk','Steampunk','Стимпанк');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('stealth','Stealth','Стелс');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('medieval','Medieval','Средневековье');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('sport','Sport','Спорт');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('slasher','Slasher','Слэшер');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('simulators','Simulators','Симулятор');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('retro','Retro','Ретро');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('turn_based','Turn based','Пошаговая');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('post_apocalyptic','Post apocalyptic','Постапокалипсис');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('dungeons','Dungeons','Подземелья');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('platform','Platform','Платформер');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('sandbox','Sandbox','Песочница');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('open_world','Open world','Открытый мир');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('third_person','Third person','От третьего лица');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('first_person','First person','От первого лица');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('sci_fi','Sci-Fi','Научная фантастика');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('board_game','Board game','Настольная');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('online','Online','Мультиплеер');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('music','Music','Музыка');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('moto','Moto','Мотоциклы');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('manager','Manager','Менеджер');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('logic','Logic','Логические');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('space','Space','Космос');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('battle_royale','Battle royale','Королевская битва');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('coop','Coop','Кооператив');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('cyberpunk','Cyberpunk','Киберпанк');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('quest','Quest','Квест');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('card','Card','Карточная');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('indie','Indie','Инди');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('zombie','Zombie','Зомби');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('other_simulators','Other simulators','Другой симулятор');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('adult','Adult','Для взрослых 18+');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('racing','Racing','Гонки');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('_4X','Global strategy','Глобальная стратегия');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('survival','Survival','Выживание');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('vr','Vr','Виртуальная реальность');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('top_down','Top down','Вид сверху');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('western','Western','Вестерн');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('basketball','Basketball','Баскетбол');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('arcade','Arcade','Аркада');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('anime','Anime','Аниме');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('adventure','Adventure','Адвенчура');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('auto','Auto','Автомобили');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('flight','Flight','Авиация');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('tower_defence','Tower defence','Tower defence');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('mmo','MMO','MMO');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('jrpg','jRpg','jRpg');

INSERT INTO library.game_genre (code, description, description_ru) VALUES ('beat_em_up','Beat-em-Up','Beat-em-Up');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('soulslike','Soulslike','Soulslike');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('fps','FPS','FPS');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('hack_and_slash','Hack And Slash','Hack And Slash');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('immersive_sim','Immersive Sim','Immersive Sim');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('point_click','Point and Click','Point and Click');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('roguelike','Roguelike','Roguelike');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('vizualnaia_novella','Visual novel','Визуальная новелла');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('interaktivnoe_kino','Interactive cinema','Интерактивное кино');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('istoriia','History','История');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('kki','Trading card game','Коллекционная карточная игра');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('metroidvaniia','Metroidvania','Метроидвания');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('pazzl','Puzzle','Паззл');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('other','xxxxxOther','яяяяДругое');

INSERT INTO library.game_genre (code, description, description_ru) VALUES ('meha','Meha','Меха');

INSERT INTO library.game_genre (code, description, description_ru) VALUES ('slesher','Slesher','Слешер');
INSERT INTO library.game_genre (code, description, description_ru) VALUES ('mistika','Mistika','Мистика');


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

create sequence game_poster_id_seq start 1;
drop table if exists library.game_poster;
create table library.game_poster
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

drop view if exists library.v_game_data;
create or replace view library.v_game_data as
select gd.id,
       gd.create_ts,
       gd.name,
       gd.directory_path,
       gd.platform,
       gd.release_date,
       post.id as poster_id
from library.game_data gd
left join library.game_poster post on gd.id = post.game_id;


GRANT ALL ON ALL TABLES IN SCHEMA library TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO "library-manager-user";