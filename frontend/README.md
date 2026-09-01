# JavaURL — Frontend (Web UI)

A dependency-free static web app (HTML + CSS + vanilla JS, no build step) for the JavaURL backend:

- Shorten any URL with an optional custom alias and expiry
- Paginated overview of all links with Active / Expired / Deleted status chips
- Copy-to-clipboard, open-in-new-tab, deactivate buttons
- **Live click counter** per link over STOMP WebSocket, with automatic polling fallback
- **QR code** generation and PNG download for each short link
- Client-side validation mirroring the backend rules; friendly error messages from the API envelope

## Run it

The backend must be running (default `http://localhost:8081`) — see [`../backend/README.md`](../backend/README.md).

Any static file server works. From this folder:

```bash
python3 -m http.server 3001        # 3000 is often taken by other dev servers
# or
npx serve -l 3001 .
```

Then open **http://localhost:3001**.

## How the API location is chosen

`js/config.js` auto-detects the target:

| UI served from            | Requests go to                    |
|---------------------------|-----------------------------------|
| port `8081` (same origin)  | same origin                       |
| anywhere else             | `<protocol>://<hostname>:8081`    |

To point elsewhere explicitly, define this before the scripts load:

```html
<script>window.JAVAURL_CONFIG_OVERRIDE = { apiBase: 'https://api.example.com', wsBase: 'wss://api.example.com' };</script>
```

## Files

```
index.html        page structure
css/styles.css    dark theme styling
js/config.js      API/WebSocket base URL resolution
js/api.js         REST client with normalised ApiError
js/ws.js          STOMP client (/ws/websocket transport, topic /topic/clicks/{code})
js/app.js         UI controller (form, table, pagination, stats panel)
```

Note: opening `index.html` directly via `file://` works in most browsers because the
backend sends permissive CORS headers, but serving over HTTP (as above) is more reliable.

**Docker:** `docker compose up -d --build` from the repository root also serves this UI
(nginx) at http://localhost:3001 alongside the containerized backend and PostgreSQL.