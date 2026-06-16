# GameLibrary — Agent Instructions

## Build & Run

```bash
mvn clean package -DskipTests              # builds target/game-library.jar
cd frontend && npm install && npm run build  # builds dist/ with Vue SPA
make all                                     # or docker-compose up --build -d
mvn spring-boot:run                          # run backend locally (needs PostgreSQL)
cd frontend && npm run dev                   # run frontend dev server (:5173, proxies to :8080)
```

No Maven wrapper — `mvn` must be on PATH. Java 1.8 target. No tests, no lint/format/typecheck config.

## Docker

`docker-compose.yml` starts four services: `frontend` (Nginx + Vue SPA, `:80`), `backend` (Spring Boot REST, `:8080`), `aria2` (torrent client, `:6800` RPC, `:6888` DHT), `postgresdb` (`:5432`).  
Optionally `ariang` web UI on `:6880`.  
DB lifecycle scripts in `postgresdb/`. Schema in `ddl/*.sql`, copied into `/docker-entrypoint-initdb.d/`.

## Architecture

- Spring Boot 2.7.1 → `jar` packaging, embedded Tomcat.
- Context path `/game-library` (`application.yml:server.servlet.context-path`).
- Frontend: separate Vue 3 + Vite + PrimeVue 4 project in `frontend/`, served via Nginx.
- API: JSON REST (`/game-library/api/**`) with JWT auth; Swagger UI at `/game-library/swagger-ui.html`.
- Legacy Thymeleaf views still work alongside REST (dual auth: form login + JWT).
- PostgreSQL schema `library`, managed via Commons DBCP + JPA (Hibernate 5.6).
- Auth: Spring Security, form login + JWT, BCrypt, `ROLE_ADMIN`/`ROLE_USER`.
- Torrent: aria2 via JSON-RPC (no embedded tracker, DHT/PEX only).
- Images: filesystem (`/gameLibrary/images/`) with DB bytea fallback.
- Scrapers: Steam, MobyGames, IGDB, Igromania, TheGameDB, Playground, WorldArt.
- Package root: `com.jenikmax.game.library`.

## Gotchas

- **Plaintext DB passwords** in `application.yml` and `postgresdb/ddl/1_init.sql`.
- `Game.java` is a decompiled `.class` → `@Entity` uses annotated getters, not fields. Keep this pattern.
- **Frontend must be built separately** before Docker (`npm run build` or `make build-frontend`). Dev server (`npm run dev`) proxies `/game-library/*` to `:8080`.
- **OkHttp 3.x API order**: `RequestBody.create(MediaType, String)` — MediaType first.
- **aria2 must be running** for torrent seeding / download management. In Docker it starts automatically; for local dev run `docker run p3terx/aria2-pro`.
- **Multi-stage Docker build** requires Docker 19.03+ and BuildKit.
- **Images on filesystem**: after DB→FS migration (`POST /api/admin/migrate-images` or `scripts/migrate-images.sh`), images are served from disk. If the file is missing, falls back to DB bytea.
