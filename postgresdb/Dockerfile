FROM postgres:16.14

COPY ./postgresdb/ddl/1_init.sh /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/2_library.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/3_user.sql /docker-entrypoint-initdb.d/

RUN chmod 755 /docker-entrypoint-initdb.d/*.sh /docker-entrypoint-initdb.d/*.sql