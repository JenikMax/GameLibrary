# ============================================================
# Makefile — задачи сборки и запуска GameLibrary
# ============================================================

.PHONY: all build-backend build-frontend build-emulatorjs up down clean

# ─── Сборка всего и запуск ──────────────────────────────
all: build-backend build-frontend up

# Сборка backend (Maven, fat JAR)
build-backend:
	mvn clean package -DskipTests

# Сборка frontend (Vue SPA)
build-frontend:
	cd frontend && npm install && npm run build

# Сборка образа EmulatorJS (WASM-ядра из CDN, фиксация версии в emulatorjs/Dockerfile)
build-emulatorjs:
	docker-compose build emulatorjs

# Запуск всех сервисов в Docker
up:
	docker-compose up --build -d

# Остановка сервисов
down:
	docker-compose down

# Полная очистка (удалить тома, собранные артефакты)
clean:
	docker-compose down -v
	mvn clean
	rm -rf frontend/dist

# Логи всех контейнеров
logs:
	docker-compose logs -f

# ─── Быстрая разработка (без Docker) ───────────────────
# Запуск backend (требует локальный PostgreSQL)
dev-backend:
	mvn spring-boot:run

# Запуск frontend dev-сервера (:5173, прокси на :8080)
dev-frontend:
	cd frontend && npm run dev
