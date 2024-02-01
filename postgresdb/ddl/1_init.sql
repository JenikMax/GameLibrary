CREATE USER "library-manager-user" WITH PASSWORD '2wq345tgfiNcbBBwee3';
CREATE DATABASE "game-library" WITH OWNER = "library-manager-user";
\c "game-library"
CREATE SCHEMA library AUTHORIZATION "library-manager-user";