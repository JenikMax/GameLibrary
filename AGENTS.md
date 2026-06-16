# GameLibrary — Agent Instructions

## Build & Run

```bash
mvn clean package                          # builds target/game-library.war
mvn spring-boot:run                        # run locally (needs PostgreSQL on localhost:5432)
```

Use `application-alone.yml` overrides for local dev. Activate via `--spring.profiles.active=alone`.  
No Maven wrapper — `mvn` must be on PATH. Java 1.8 target. No tests, no lint/format/typecheck config.

## Docker

`docker-compose.yml` starts three services: `deluge` (torrent client), `postgresdb`, `application` (Tomcat `:8080`, tracker `:9010`).  
DB lifecycle scripts in `postgresdb/`. Schema in `ddl/*.sql`, copied into `/docker-entrypoint-initdb.d/`.

## Architecture

- Spring Boot 2.7.1 → `war` packaging, deploys to external Tomcat via `ServletInitializer`.
- Context path `/game-library` (`application.yml:server.servlet.context-path`).
- PostgreSQL schema `library`, managed via Commons DBCP + JPA (Hibernate 5.6).
- Thymeleaf templates in `src/main/resources/templates/` (names omit `.html`), i18n in `msg/`.
- Auth: Spring Security, form login, BCrypt, `ROLE_ADMIN`/`ROLE_USER`.
- Torrent: embedded tracker (`ttorrent-core`) + Deluge integration.
- Scrapers: Steam, MobyGames, IGDB, Igromania, TheGameDB, Playground, WorldArt.
- Package root: `com.jenikmax.game.library`.

## Gotchas

- **Plaintext DB passwords** in `application.yml` and `postgresdb/ddl/1_init.sql`.
- `Game.java` is a decompiled `.class` → `@Entity` uses annotated getters, not fields. Keep this pattern.
- `application-alone.yml` uses Windows paths (`D:/Work/...`) — swap for Linux outside Docker.
