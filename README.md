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

- **Backend (Java, no Docker)** → **Railway**
- **Frontend (static)** → **Vercel / Netlify / GitHub Pages**

> In-memory **H2** (the `dev` profile we use locally) works on Railway with **no database** — set `SPRING_PROFILES_ACTIVE=dev`. For persistence, use PostgreSQL by setting `POSTGRES_URL`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`.

### Fastest: Railway backend + Vercel frontend
1. In **Railway**, create a new project from GitHub and select this repo.
2. Set the Railway service **Root Directory** to `backend`.
3. Build command: `./mvnw package -DskipTests`
4. Start command: `java -jar target/JavaURL-0.0.1-SNAPSHOT.jar`
5. Add environment variables:

```text
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=${PORT}
APP_BASE_URL=https://YOUR-RAILWAY-DOMAIN.up.railway.app
```

6. Generate a public Railway domain, then update `APP_BASE_URL` to that exact URL.
7. Deploy `frontend/` to **Vercel** (or any static host).
8. In `frontend/index.html`, uncomment the override block and point it at your Railway backend URL:

```html
<script>
  window.JAVAURL_CONFIG_OVERRIDE = {
    apiBase: 'https://YOUR-RAILWAY-DOMAIN.up.railway.app',
    wsBase:  'wss://YOUR-RAILWAY-DOMAIN.up.railway.app'
  };
</script>
```

### Wait — you said H2 in-memory?
Correct: for local running we use `SPRING_PROFILES_ACTIVE=dev` → H2 in-memory (data resets on restart, perfect for a demo). On Railway, setting the same profile gives you a live API with **no database setup**. For real persistence, flip to Postgres (see `.env.example`).

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
