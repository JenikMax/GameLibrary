-- ============================================================
-- 03_user.sql — Таблица пользователей
-- Содержит: library_user (пользователи системы),
--           начальные учётные записи admin и guest.
-- ============================================================
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

COMMENT ON TABLE library.library_user IS 'Пользователи системы (администраторы и обычные пользователи)';
COMMENT ON COLUMN library.library_user.id IS 'Уникальный идентификатор пользователя';
COMMENT ON COLUMN library.library_user.create_ts IS 'Дата и время регистрации';
COMMENT ON COLUMN library.library_user.user_name IS 'Логин (имя пользователя)';
COMMENT ON COLUMN library.library_user.pass IS 'Хэш пароля (BCrypt)';
COMMENT ON COLUMN library.library_user.is_admin IS 'Флаг администратора (true = ROLE_ADMIN)';
COMMENT ON COLUMN library.library_user.is_active IS 'Флаг активности учётной записи';
COMMENT ON COLUMN library.library_user.avatar IS 'Аватар пользователя (bytea)';

-- Начальные учётные записи: admin (полные права) и guest (только просмотр)
INSERT INTO library.library_user (create_ts, user_name,pass,is_admin,is_active,avatar) VALUES (now(),'admin','$2a$10$M5DajV5kKWKpKDO8T./PnuvfB/Hz14lC.b6HiTQ5qqfpGwaCM9nly',true,true,null);
INSERT INTO library.library_user (create_ts, user_name,pass,is_admin,is_active,avatar) VALUES (now(),'guest','$2a$10$zOUmC6do.bEdibka1Z4AQuMbCLPtiAkzDn01BbmoV5mB5mVz8UH.q',false,true,null);

-- ============================================================
-- Права доступа
-- ============================================================
GRANT ALL ON ALL TABLES IN SCHEMA library TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA library TO "library-manager-user";
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO "library-manager-user";