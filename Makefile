.PHONY: all build-backend build-frontend up down clean

all: build-backend build-frontend up

build-backend:
	mvn clean package -DskipTests

build-frontend:
	cd frontend && npm install && npm run build

up:
	docker-compose up --build -d

down:
	docker-compose down

clean:
	docker-compose down -v
	mvn clean
	rm -rf frontend/dist

logs:
	docker-compose logs -f

# Quick dev: run backend only (requires local DB + aria2)
dev-backend:
	mvn spring-boot:run

dev-frontend:
	cd frontend && npm run dev
