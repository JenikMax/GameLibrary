FROM postgres:latest

COPY ./postgresdb/ddl/1_init.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/2_library.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/3_user.sql /docker-entrypoint-initdb.d/

RUN chmod 755 /docker-entrypoint-initdb.d/*.sql