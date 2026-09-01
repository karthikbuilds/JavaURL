# JavaURL — Full-Stack URL Shortener

A URL shortening platform split into two independent applications:

```
JavaURL/
├── backend/     Spring Boot 4.1.1 REST API + WebSocket server (Java 21, PostgreSQL/H2)
├── frontend/    Zero-build web UI (HTML/CSS/vanilla JS) for shortening links & live click stats
└── docker-compose.yml   Local PostgreSQL for the backend
```

| App      | Location    | Port | Docs                                        |
|----------|-------------|------|---------------------------------------------|
| Backend  | `backend/`  | 8080 | [`backend/README.md`](backend/README.md)    |
| Frontend | `frontend/` | 3001 | [`frontend/README.md`](frontend/README.md)  |

## Quickstart (two terminals)

**1. Start the API**

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # in-memory H2, zero setup
```

> For PostgreSQL instead: from the repository root run `docker compose up -d`,
> then start the backend *without* the dev profile.

**2. Start the web UI**

```bash
cd frontend
python3 -m http.server 3001        # or: npx serve -l 3001 .
```

Open **http://localhost:3001**. The UI auto-detects the API at `http://localhost:8080`
(CORS is enabled on the backend); override targets via `window.JAVAURL_CONFIG_OVERRIDE`.

### Alternative — whole stack in Docker

With Docker Desktop running, one command starts PostgreSQL + API + UI:

```bash
docker compose up -d --build       # first build downloads dependencies, be patient
```

Same URLs as above (`http://localhost:3001` / `http://localhost:8080`); data persists in a
named volume. Stop everything with `docker compose down` (add `-v` to wipe the database).

## What you get

- Shorten URLs with generated Base62 codes or custom aliases, optional expiry
- One-click copy, **QR code download**, redirect testing, deactivation (`410 Gone` afterwards)
- Paginated link overview with status chips (Active / Expired / Deleted)
- **Live click counters** pushed over STOMP WebSocket (`/topic/clicks/{code}`),
  with automatic polling fallback when WebSocket is unavailable
- Per-click analytics detail (referrer, user agent, origin IP) via `/api/v1/urls/{code}/clicks`
- Resilient performance: atomic click counters, Caffeine redirect cache, per-client rate limiting
- Health/metrics via Actuator, continuous integration via GitHub Actions

## Tests & builds

```bash
cd backend && ./mvnw test      # 27 tests incl. full HTTP integration suite (H2)
```

The frontend has no build step — it runs as plain static files.
