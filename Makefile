.DEFAULT_GOAL := help

# ==============================================================================
# Configuration Variables (Overridable from CLI, e.g. `make dev API_PORT=8081`)
# ==============================================================================
MVN ?= ./mvnw
API_PORT ?= 8080
DB_PORT ?= 5432
DB_NAME ?= library
DB_USERNAME ?= library
DB_PASSWORD ?= library
DB_URL ?= jdbc:postgresql://localhost:$(DB_PORT)/$(DB_NAME)

.PHONY: help \
        dev-compose compose-up compose-up-d compose-down compose-down-v compose-logs compose-ps \
        dev dev-with-db \
        db-up db-down db-restart db-reset db-shell \
        build package clean test docker-build \
        health open-docs sample-flow

# ==============================================================================
# Help / Self-Documentation
# ==============================================================================
help: ## Show available commands and their descriptions
	@echo "\nUsage: make [target] [VARIABLE=value]\n"
	@echo "Available targets:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""

# ==============================================================================
# 1. Docker Compose Dev Server (Full Stack in Containers)
# ==============================================================================
dev-compose: compose-down compose-up ## Run dev server in Docker Compose with restart/teardown (stops previous containers first, then rebuilds & starts)

compose-up: ## Start all services (API + DB) with Docker Compose and stream logs
	API_PORT=$(API_PORT) DB_PORT=$(DB_PORT) docker compose up --build

compose-up-d: ## Start all services with Docker Compose in detached (background) mode
	API_PORT=$(API_PORT) DB_PORT=$(DB_PORT) docker compose up --build -d

compose-down: ## Stop and remove all Docker Compose containers and networks
	docker compose down --remove-orphans

compose-down-v: ## Stop Docker Compose and remove all persistent database volumes (fresh start)
	docker compose down --volumes --remove-orphans

compose-logs: ## Follow logs from all Docker Compose services
	docker compose logs -f

compose-ps: ## View status of running Docker Compose services
	docker compose ps

# ==============================================================================
# 2. Local Dev Server (Without Docker Compose for the Application)
# ==============================================================================
dev: ## Run dev server locally with Maven Spring Boot plugin (requires running PostgreSQL)
	DB_URL="$(DB_URL)" DB_USERNAME="$(DB_USERNAME)" DB_PASSWORD="$(DB_PASSWORD)" sh $(MVN) spring-boot:run

dev-with-db: db-up dev ## Start only PostgreSQL container in background, then run dev server locally

# ==============================================================================
# Database Helper Commands
# ==============================================================================
db-up: ## Start only the PostgreSQL database container in background and wait until healthy
	DB_PORT=$(DB_PORT) docker compose up -d --wait database

db-down: ## Stop the PostgreSQL database container
	docker compose stop database

db-restart: db-down db-up ## Restart the PostgreSQL database container

db-reset: ## Reset the PostgreSQL database volume for a clean state
	docker compose stop database
	docker compose rm -f -v database
	docker volume rm -f library-service_postgres-data 2>/dev/null || true
	DB_PORT=$(DB_PORT) docker compose up -d --wait database

db-shell: ## Open interactive psql shell inside the database container
	docker compose exec -it database psql -U $(DB_USERNAME) -d $(DB_NAME)

# ==============================================================================
# Build, Package & Test Commands
# ==============================================================================
build: ## Compile and build the application JAR (skips tests for speed)
	sh $(MVN) clean package -DskipTests

package: ## Run all tests and package the application JAR
	sh $(MVN) clean package

test: ## Run unit and integration tests
	sh $(MVN) test

clean: ## Clean Maven target build artifacts
	sh $(MVN) clean

docker-build: ## Build standalone Docker image directly with docker build
	docker build -t library-service:latest .

# ==============================================================================
# Verification & Utilities
# ==============================================================================
health: ## Check Actuator health endpoint of the running server
	curl -i http://localhost:$(API_PORT)/actuator/health

open-docs: ## Open Swagger UI documentation in your default browser
	@open http://localhost:$(API_PORT)/swagger-ui.html 2>/dev/null || xdg-open http://localhost:$(API_PORT)/swagger-ui.html 2>/dev/null || echo "Swagger UI: http://localhost:$(API_PORT)/swagger-ui.html"

sample-flow: ## Execute sample API request flow (register borrower, add book, borrow, return)
	@echo "=== Registering Borrower ==="
	@BORROWER_ID=$$(curl --fail --silent -H 'Content-Type: application/json' -d '{"name":"Ada Lovelace","email":"ada@example.com"}' "http://localhost:$(API_PORT)/api/v1/borrowers" | grep -o '"id":"[^"]*' | cut -d'"' -f4); \
	echo "Created Borrower ID: $$BORROWER_ID"; \
	echo "\n=== Registering Book ==="; \
	BOOK_ID=$$(curl --fail --silent -H 'Content-Type: application/json' -d '{"isbn":"978-0-13-468599-1","title":"Effective Java","author":"Joshua Bloch"}' "http://localhost:$(API_PORT)/api/v1/books" | grep -o '"id":"[^"]*' | cut -d'"' -f4); \
	echo "Created Book ID: $$BOOK_ID"; \
	echo "\n=== Borrowing Book ==="; \
	curl --fail --silent -H 'Content-Type: application/json' -d "{\"bookId\":\"$$BOOK_ID\",\"borrowerId\":\"$$BORROWER_ID\"}" "http://localhost:$(API_PORT)/api/v1/loans"; \
	echo "\n\n=== Returning Book ==="; \
	curl --fail --silent -X POST "http://localhost:$(API_PORT)/api/v1/books/$$BOOK_ID/return"; \
	echo "\n\n=== Listing Borrowers History ==="; \
	curl --fail --silent "http://localhost:$(API_PORT)/api/v1/borrowers"; \
	echo "\n\nDone!"
