# JavaURL — Full-Stack URL Shortener

A URL shortening platform split into two independent applications:

```
JavaURL/
├── backend/     Spring Boot 4.1.1 REST API + WebSocket server (Java 21, PostgreSQL/H2)
└── frontend/    Zero-build web UI (HTML/CSS/vanilla JS) for shortening links & live click stats
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

> For PostgreSQL instead, provide `POSTGRES_URL`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`,
> then start the backend *without* the `dev` profile.

**2. Start the web UI**

```bash
cd frontend
python3 serve.py 3001               # or: npx serve -l 3001 .
```

Open **http://localhost:3001**. The UI auto-detects the API at `http://localhost:8081`
(CORS is enabled on the backend); override targets via `window.JAVAURL_CONFIG_OVERRIDE`.

## ☁️ Deploy (free, demo)

The backend is a Spring Boot API + database; the frontend is plain static files. The simplest deploy path is:

- **Backend (Java/Docker)** → **Render**
- **Frontend (static)** → **Vercel / Netlify / GitHub Pages**

> In-memory **H2** (the `dev` profile we use locally) works on Render with **no database** — set `SPRING_PROFILES_ACTIVE=dev`. For persistence, use PostgreSQL by setting `POSTGRES_URL`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`.

### Fastest: Render backend + Vercel frontend
The repository includes a `render.yaml` Blueprint for the backend. In **Render**, choose
**New → Blueprint**, connect this repository, and apply the generated web service. The
Dockerfile builds the backend and Render injects its public `PORT` automatically.

For a zero-setup demo, add these environment variables to the Render service:

```text
SPRING_PROFILES_ACTIVE=dev
APP_BASE_URL=https://YOUR-BACKEND.onrender.com
```

Update `APP_BASE_URL` after Render assigns the service URL, then deploy `frontend/` to
**Vercel** (or any static host). In `frontend/index.html`, uncomment the override block
and point it at the Render backend:

```html
<script>
  window.JAVAURL_CONFIG_OVERRIDE = {
    apiBase: 'https://YOUR-BACKEND.onrender.com',
    wsBase:  'wss://YOUR-BACKEND.onrender.com'
  };
</script>
```

### Wait — you said H2 in-memory?
Correct: for local running we use `SPRING_PROFILES_ACTIVE=dev` → H2 in-memory (data resets on restart, perfect for a demo). On Render, setting the same profile gives you a live API with **no database setup**. For real persistence, provision Render PostgreSQL and set `POSTGRES_URL`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` (see `.env.example`).

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
