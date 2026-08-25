# Library Service

Java 17/Spring Boot REST API for registering borrowers and physical book copies, then borrowing and returning individual copies. PostgreSQL stores inventory and complete loan history; Flyway owns the schema.

## API documentation

With the application running:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>

The generated specification documents request and response schemas, validation errors, and the `400`, `404`, and `409` outcomes for all actions.

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/v1/borrowers` | Register a borrower (`201`) |
| `GET` | `/api/v1/borrowers` | List every borrower with complete loan history (`200`) |
| `POST` | `/api/v1/books` | Register one physical copy (`201`) |
| `GET` | `/api/v1/books` | List every copy and its availability (`200`) |
| `POST` | `/api/v1/loans` | Borrow a specific copy (`201`) |
| `POST` | `/api/v1/books/{bookId}/return` | Return the active loan (`200`) |

Errors have a stable JSON shape:

```json
{
  "timestamp": "2026-08-21T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/v1/borrowers",
  "fieldErrors": { "email": "must be a well-formed email address" }
}
```

## Run with Docker Compose

Requirements: Docker with Compose v2. From the repository root:

```bash
docker compose up --build --wait
curl --fail http://localhost:8080/actuator/health
```

Compose starts an isolated PostgreSQL 17 database, waits for it to become healthy, then starts the API. Flyway applies migrations automatically. To stop it:

```bash
docker compose down
```

To verify a **clean database**, remove the named data volume before restarting (this permanently removes local library data):

```bash
docker compose down --volumes
docker compose up --build --wait
```

Set `API_PORT` if port 8080 is occupied, for example `API_PORT=8081 docker compose up --build --wait`; use that port in the URLs below.

## Run locally with Maven

Requirements: Java 17, a running PostgreSQL database, and Docker if PostgreSQL-backed Testcontainers tests should execute. Start only the Compose database and then the application:

```bash
docker compose up -d --wait database
DB_URL=jdbc:postgresql://localhost:5432/library \
DB_USERNAME=library \
DB_PASSWORD=library \
sh mvnw spring-boot:run
```

The Maven wrapper is invoked through `sh` because it may not have executable permissions. Run tests with:

```bash
sh mvnw test
```

PostgreSQL integration tests use Testcontainers and skip when Docker is unavailable.

## Representative API flow

The commands below require `curl` and use `jq` only to capture generated opaque IDs.

```bash
BASE_URL=http://localhost:8080

BORROWER_ID=$(curl --fail --silent \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada Lovelace","email":"ada@example.com"}' \
  "$BASE_URL/api/v1/borrowers" | jq -r .id)

BOOK_ID=$(curl --fail --silent \
  -H 'Content-Type: application/json' \
  -d '{"isbn":"978-0-13-468599-1","title":"Effective Java","author":"Joshua Bloch"}' \
  "$BASE_URL/api/v1/books" | jq -r .id)

curl --fail --silent "$BASE_URL/api/v1/books" | jq

curl --fail --silent \
  -H 'Content-Type: application/json' \
  -d "{\"bookId\":\"$BOOK_ID\",\"borrowerId\":\"$BORROWER_ID\"}" \
  "$BASE_URL/api/v1/loans" | jq

curl --fail --silent -X POST \
  "$BASE_URL/api/v1/books/$BOOK_ID/return" | jq

curl --fail --silent "$BASE_URL/api/v1/borrowers" | jq
```

Listing books after borrowing shows `available: false`; listing after return shows `available: true`. Listing borrowers returns each borrower's active and returned loans in chronological order; borrowers with no loans have an empty `borrowHistory`. A second copy may be registered with the same ISBN and matching title/author and receives a different `id`.

## Configuration

Configuration is supplied through the environment; values shown are development defaults.

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/library` | JDBC connection URL |
| `DB_USERNAME` | `library` | Database user |
| `DB_PASSWORD` | `library` | Database password |
| `POSTGRES_DB` | `library` | Compose PostgreSQL database name |
| `POSTGRES_USER` | `library` | Compose PostgreSQL user and API `DB_USERNAME` |
| `POSTGRES_PASSWORD` | `library` | Compose PostgreSQL password and API `DB_PASSWORD` |
| `API_PORT` | `8080` | Host port published by Compose |

Do not use the development credentials in deployed environments. Secrets are not embedded in the image; inject them through the runtime environment or a secrets manager.

## Data and concurrency design

PostgreSQL was selected for durable ACID transactions, foreign keys, row-level locking, and partial unique indexes. An ISBN-unique `book_records` row stores title and author, while `book_copies` gives every physical copy its own UUID. API book responses flatten that model.

Each borrow/return transition is a short transaction that pessimistically locks the target copy. Every lifecycle remains in `loans` with `borrowed_at` and nullable `returned_at`. A PostgreSQL partial unique index on `book_copy_id WHERE returned_at IS NULL` independently guarantees at most one active loan, including concurrent races; conflicts are returned as `409`.

## Assumptions

- IDs are server-generated UUIDs and opaque to clients.
- Borrower names are trimmed; email must be syntactically valid but need not be unique.
- ISBN input is normalized by removing spaces/hyphens and validated as ISBN-10 or ISBN-13.
- One normalized ISBN identifies one trimmed title/author pair. Different metadata for an existing ISBN returns `409`.
- Every book registration creates a new physical copy, including registrations sharing an ISBN.
- Borrowing and returning take effect immediately; returning a copy without an active loan returns `409`.
- Only the active loan is closed, and returned loan rows remain as history.

## Twelve-Factor alignment

- **Codebase/dependencies:** one codebase with Maven-locked, declared dependencies.
- **Config:** database credentials and connection details come from environment variables.
- **Backing services:** PostgreSQL is attached through a URL and credentials rather than hard-coded infrastructure.
- **Build/release/run:** the multi-stage image separates compilation from the runtime image; Flyway versions schema changes.
- **Processes/disposability:** the API is stateless, runs as a non-root process, and has health checks and graceful container replacement.
- **Port binding/concurrency/logs:** HTTP is port-bound, instances can scale independently while database constraints enforce correctness, and Spring logs go to standard output.
- **Dev/prod parity:** local and Compose workflows both use PostgreSQL; Testcontainers exercises PostgreSQL-specific behavior.

## Intentional exclusions

This initial API has no authentication or authorization, borrower self-service, reservations, renewals, due dates, fines, borrower loan limits, pagination, or catalog search. CI/CD automation is also not included. These are deliberate scope choices, not implied API behavior.
