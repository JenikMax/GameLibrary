\c "game-library"

create sequence library_user_id_seq start 1;
drop table if exists library.library_user;
create table library.library_user
(
    id                bigserial primary key,
    create_ts         timestamp without time zone,
    user_name         varchar(225),
    pass              varchar(225),
    is_admin          boolean default false,
    is_active         boolean default true,
    avatar            bytea
);

COMMENT ON TABLE library.library_user IS 'Таблица пользователей';
COMMENT ON COLUMN library.library_user.id IS 'Идентификатор';
COMMENT ON COLUMN library.library_user.create_ts IS 'Дата создания пользователя';
COMMENT ON COLUMN library.library_user.user_name IS 'Имя пользователя';
COMMENT ON COLUMN library.library_user.pass IS 'Пароль пользователя';
COMMENT ON COLUMN library.library_user.is_admin IS 'Признак администратора';
COMMENT ON COLUMN library.library_user.is_active IS 'Признак активности пользователя';
COMMENT ON COLUMN library.library_user.avatar IS 'Изображение пользователя';

INSERT INTO library.library_user (create_ts, user_name,pass,is_admin,is_active,avatar) VALUES (now(),'admin','$2a$10$M5DajV5kKWKpKDO8T./PnuvfB/Hz14lC.b6HiTQ5qqfpGwaCM9nly',true,true,null);
INSERT INTO library.library_user (create_ts, user_name,pass,is_admin,is_active,avatar) VALUES (now(),'guest','$2a$10$zOUmC6do.bEdibka1Z4AQuMbCLPtiAkzDn01BbmoV5mB5mVz8UH.q',false,true,null);

GRANT ALL ON ALL TABLES IN SCHEMA library TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO "library-manager-user";