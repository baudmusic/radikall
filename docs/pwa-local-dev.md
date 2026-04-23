# Radikall PWA Local Dev

## What This Adds

- `webProxy/`: a Ktor JVM server that handles Radiko auth, live session creation, and HLS proxying.
- `webApp/`: a React + Vite + TypeScript PWA frontend that talks only to the local proxy.
- `shared/`: still holds the existing Android/Desktop/iOS code and is reused by `webProxy` for station/program/auth logic.

## Local Windows Workflow

Run the proxy and frontend as two processes from `radiko-app/`.

### 1. Start the Ktor proxy

```powershell
.\gradlew.bat :webProxy:run
```

Requirement:

- `ffmpeg` must be available on `PATH`
- the current browser-friendly playback path uses `ffmpeg` inside `webProxy` to turn the proxied Radiko HLS stream into `audio/mpeg`

Default port:

- `http://127.0.0.1:8787`

Health check:

```powershell
curl http://127.0.0.1:8787/health
```

### 2. Install frontend dependencies

```powershell
cd .\webApp
npm install
```

### 3. Start the Vite dev server

```powershell
npm run dev
```

Default port:

- `http://127.0.0.1:5173`

Vite proxies `/api` and `/health` to `http://127.0.0.1:8787`, so the browser only needs the frontend origin.

Note:

- Dev mode no longer generates a development service worker.
- This avoids noisy `dev-dist` warnings during normal UI iteration.
- Use the production build path below when you want a real standalone / service worker check.

## Production Build Check

Build the PWA shell:

```powershell
cd .\webApp
npm run build
```

Then point `webProxy` at the built assets. By default it already looks for:

- `webApp/dist`

If you want to override the path, set:

- `RADIKALL_WEB_DIST`

## iPhone Local Test Path

Use an HTTPS development tunnel.

There are now two useful modes:

1. Fast UI iteration:
   - expose the Vite dev server
2. Real PWA / home screen validation:
   - build `webApp`
   - run `webProxy`
   - expose `webProxy` so iPhone gets the built `dist` plus the real service worker

Recommended flow:

1. Run `cmd /c npm run build` in `webApp/`.
2. Run `:webProxy:run` from the repo root.
3. Expose port `8787` through your HTTPS tunnel tool.
4. Open the tunnel URL in iPhone Safari.
5. Add the page to the home screen.
6. Launch from the home screen and test playback there.

Important:

- The iPhone should visit only the tunneled origin.
- Do not point Safari directly at a LAN IP if you want a realistic standalone/PWA test.
- For quick layout work, tunneling Vite is still fine.
- For standalone/PWA verification, tunnel `webProxy` after `webApp/dist` has been built.

## v1 Feature Expectations

Included in this PWA build:

- station browsing
- area switching
- search
- live playback
- now-playing details
- program website link
- recent songs
- weekly schedule
- language/theme/driving mode/startup area memory

Intentionally deferred in v1:

- alarm auto-start playback
- Web Push reminders
- sleep timer
- guaranteed background playback
- browser-direct Radiko access

## Troubleshooting

### `webProxy` starts but playback fails immediately

Check:

- `http://127.0.0.1:8787/health`
- `http://127.0.0.1:8787/api/bootstrap`
- `ffmpeg -version`
- the proxy console for auth or upstream fetch failures

### Frontend loads but station list is empty

Check:

- Vite is running on port `5173`
- `/api/stations` returns JSON in the browser devtools network tab
- `selectedAreaId` is valid, such as `JP13`

### iPhone opens Safari instead of standalone mode

Check:

- you launched from the home screen icon
- the Vite page is served over HTTPS through the tunnel
- the manifest and service worker loaded successfully

### Background playback is inconsistent

That is expected for v1. The current build treats background playback on iPhone PWA as best effort only.
