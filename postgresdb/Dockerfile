FROM postgres:16.14

COPY ./postgresdb/ddl/01_init.sh /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/02_library.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/03_user.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/04_search.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/05_rating.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/06_favorite.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/07_comment.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/08_notification.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/09_collection.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/10_smart_collection.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/11_review.sql /docker-entrypoint-initdb.d/
COPY ./postgresdb/ddl/12_tags.sql /docker-entrypoint-initdb.d/

RUN chmod 755 /docker-entrypoint-initdb.d/*.sh /docker-entrypoint-initdb.d/*.sql