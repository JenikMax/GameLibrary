#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER "library-manager-user" WITH PASSWORD '${DB_PASSWORD}';
    CREATE DATABASE "game-library" WITH OWNER = "library-manager-user";
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "game-library" <<-EOSQL
    CREATE SCHEMA library AUTHORIZATION "library-manager-user";
EOSQL
