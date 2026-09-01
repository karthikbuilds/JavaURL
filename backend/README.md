# JavaURL — Backend (REST API + WebSocket)

Spring Boot **4.1.1** / Java 21 service that shortens URLs, redirects visitors,
counts clicks and broadcasts live analytics.

- Persistence: Spring Data JPA / Hibernate — PostgreSQL (default) or H2 (`dev` profile)
- Realtime: STOMP over WebSocket (`/ws`, SockJS fallback)
- Security: stateless, all endpoints public, CORS `*`
- Tests: JUnit 5 + Mockito + MockMvc against H2 — no external services needed

## Run

```bash
# zero-setup (in-memory H2):
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# PostgreSQL (start just the database from the repository root):
#   docker compose up -d postgres
./mvnw spring-boot:run
```

Environment overrides: `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `APP_BASE_URL`.

## API Reference

Base URL: `http://localhost:8080`

| Method   | Path                    | Description                                    |
|----------|-------------------------|------------------------------------------------|
| `POST`   | `/api/v1/urls`          | Create a short URL → `201`                     |
| `GET`    | `/api/v1/urls`          | List all short URLs, paged                     |
| `GET`    | `/api/v1/urls/{code}`   | Metadata + click stats                         |
| `DELETE` | `/api/v1/urls/{code}`   | Deactivate (idempotent) → `204`                |
| `GET`    | `/{code}`               | Redirect to destination (`302`; `410` if gone) |
| `GET`    | `/ws`                   | STOMP endpoint (SockJS fallback)               |

### Create a short URL

```bash
curl -i -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"longUrl": "https://spring.io/projects/spring-boot", "customAlias": "boot", "expiresInDays": 7}'
```

```json
{
  "id": 1,
  "shortCode": "boot",
  "shortUrl": "http://localhost:8080/boot",
  "longUrl": "https://spring.io/projects/spring-boot",
  "createdAt": "2026-08-26T00:00:00Z",
  "expiresAt": "2026-09-02T00:00:00Z",
  "active": true,
  "clickCount": 0
}
```

Request fields:

| Field           | Type    | Required | Notes                                              |
|-----------------|---------|----------|----------------------------------------------------|
| `longUrl`       | string  | yes      | Absolute http(s) URL, max 2048 chars               |
| `customAlias`   | string  | no       | 3–64 chars `[A-Za-z0-9_-]`, unique, not reserved   |
| `expiresInDays` | int     | no       | Relative expiry; mutually exclusive w/ `expiresAt` |
| `expiresAt`     | instant | no       | ISO-8601 timestamp in the future                   |

Errors share one envelope: `{"status", "error", "message", "timestamp", "path", "fieldErrors"?}` —
duplicate alias → `409`, unknown code → `404`, expired/deactivated → `410`, validation → `400`.

### Live click analytics

Connect to `/ws` and subscribe to `/topic/clicks/{code}`. Every redirect publishes:

```json
{ "shortCode": "boot", "totalClicks": 42, "clickedAt": "2026-08-26T12:00:00Z" }
```

## Configuration (`application.properties`)

| Property                    | Default                 | Purpose                            |
|-----------------------------|-------------------------|------------------------------------|
| `app.base-url`              | `http://localhost:8080` | Base URL used in returned links    |
| `app.code-length`           | `7`                     | Random code length (3–32)          |
| `app.redirect-status`       | `302`                   | Redirect status (301/302/307/308)  |
| `app.cleanup.enabled`       | `true`                  | Scheduled expiry maintenance       |
| `app.cleanup.retention-days`| `30`                    | Hard-delete window after expiry    |
| `app.cleanup.interval-ms`   | `3600000`               | Cleanup interval                   |

## Test & package

```bash
./mvnw test        # unit + integration tests (H2)
./mvnw package     # executable jar in target/
```

## Layout

```
src/main/java/com/karthik/JavaURL/
├── analytics/    ClickEvent + WebSocket publisher
├── config/       AppProperties, SecurityConfig, WebSocketConfig
├── url/          Entity, repository, service, controllers, validation, errors (+ dto/)
└── util/         Base62Codec
```