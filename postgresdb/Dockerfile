FROM postgres:latest

COPY ddl/1_init.sql /docker-entrypoint-initdb.d/
COPY ddl/2_library.sql /docker-entrypoint-initdb.d/

RUN chmod 755 /docker-entrypoint-initdb.d/*.sql