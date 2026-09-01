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
| Backend  | `backend/`  | 8081 | [`backend/README.md`](backend/README.md)    |
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
python3 serve.py 3001               # or: npx serve -l 3001 .
```

Open **http://localhost:3001**. The UI auto-detects the API at `http://localhost:8081`
(CORS is enabled on the backend); override targets via `window.JAVAURL_CONFIG_OVERRIDE`.

### Alternative — whole stack in Docker

With Docker Desktop running, one command starts PostgreSQL + API + UI:

```bash
docker compose up -d --build       # first build downloads dependencies, be patient
```

Same URLs as above (`http://localhost:3001` / `http://localhost:8081`); data persists in a
named volume. Stop everything with `docker compose down` (add `-v` to wipe the database).

## ☁️ Deploy (free, demo)

The backend is a Spring Boot API + database; the frontend is plain static files. Two small pieces, two ways to host:

- **Frontend (static)** → **Vercel / Netlify / GitHub Pages** (free, zero config)
- **Backend (API + DB)** → **Render / Railway / Fly** (has free tiers)

> In-memory **H2** (the `dev` profile we use locally) works on Render with **no database** — put `SPRING_PROFILES_ACTIVE=dev`. For persistence, attach a Postgres and Set `POSTGRES_URL` / `POSTGRES_USER` / `POSTGRES_PASSWORD`.

### Fastest: Render-only (skip Vercel)
1. Import `render.yaml` into **Render** (New → Blueprint) — it creates the backend Web Service (H2, no DB).
2. Set `APP_BASE_URL` to the live URL Render assigns (e.g. `https://javaurl-backend.onrender.com`) — otherwise all short links point at `localhost`.
3. Serve the frontend as a **Render Static Site** (root `frontend`, no build, publish `.`) **or** push `frontend/` to **Vercel**.
4. In `frontend/index.html`, uncomment the override block and point it at that backend URL:

```html
<script>
  window.JAVAURL_CONFIG_OVERRIDE = {
    apiBase: 'https://YOUR-BACKEND.onrender.com',
    wsBase:  'wss://YOUR-BACKEND.onrender.com'
  };
</script>
```

### Wait — you said H2 in-memory?
Correct: for local running we use `SPRING_PROFILES_ACTIVE=dev` → H2 in-memory (data resets on restart, perfect for a demo). On Render, setting the same profile gives you a live API with **no database setup**. For real persistence, flip to Postgres (see `.env.example`.

### Full env reference
See **[`.env.example`](.env.example)** — it lists every variable (ports, DB choice, `APP_BASE_URL`, tuning).

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
