<h1 align="center">Radikall</h1>

<p align="center">
  <img src=".github/assets/logo3.png" alt="Radikall logo" width="260" />
</p>

<p align="center">
  A Windows + Android radio client for Japanese stations — listen to every Radiko station from anywhere in the world.
</p>

<p align="center">
  <a href="https://baudstudio.com">baudstudio.com</a>
</p>

<p align="center">
  <a href="README.md">English</a> | <a href="README.zh-CN.md">简体中文</a> | <a href="README.zh-TW.md">繁體中文</a> | <a href="README.ja.md">日本語</a> | <a href="README.ko.md">한국어</a>
</p>

## A Note Before You Start

I built this app primarily so I could listen to Tatsuro Yamashita's Rakuten Card Sunday Songbook (山下達郎の楽天カード サンデー・ソングブック) more conveniently. Summer is almost here — time to revisit the Big Wave album.

Also: Claude, you really are stingy with those usage limits.

## Overview

Radikall is a cross-platform radio client covering all Japanese stations across Radiko's official 47 prefectures, with full station browsing and playback support — listen from anywhere in the world with a network connection.

It features smooth station browsing, now-playing details, weekly schedules, tray playback controls on Windows, and a multi-language settings system across Windows and Android.

This project was made possible in part by the ideas, reverse-engineering path, and inspiration from [jackyzy823/rajiko](https://github.com/jackyzy823/rajiko). Without that repository, this native app route would have been much harder to reach.

## Before You Use It

If you just want to use the app instead of building it from source, read this section first.

### Windows

1. Download the latest Windows installer from your GitHub Releases page.
2. Install **VLC media player** first.
3. Launch Radikall after VLC is installed, because desktop playback currently depends on `libVLC` through `vlcj`.

Recommended environment:

- Windows 10 or Windows 11
- 64-bit system
- Stable internet connection

### Android

1. Download the latest **signed** Android APK from your GitHub Releases page.
2. On the phone, allow installation from unknown sources if Android asks.
3. Open the app and choose your area before playback.

Recommended environment:

- Android 7.0 or newer
- Stable Wi-Fi or mobile network

### Build From Source

If you want to build the project yourself, install these first:

- Git
- A full JDK 24 installation
- Android Studio with Android SDK 35
- Platform Tools / `adb`
- VLC media player on Windows for desktop playback testing

Then run:

```powershell
.\gradlew.bat :desktopApp:run
.\gradlew.bat :androidApp:installDebug
```

## Release Build

### Windows

```powershell
# Step 1: Build the distributable
.\gradlew.bat :desktopApp:createReleaseDistributable

# Step 2: Package the installer
& "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" tools\installer\radikall-setup.iss
```

Output:

- `desktopApp/build/compose/binaries/main-release/Radikall-Setup-0.1.0.exe`

### Android

```powershell
.\gradlew.bat :androidApp:assembleRelease
```

Output:

- `androidApp/build/outputs/apk/release/`

Note:

- This project currently builds **one universal Android APK**.
- It does **not** enable ABI splits or density splits, so you do not get many APKs.

## Technical Route

Radikall is built with a Kotlin Multiplatform architecture:

- `shared/`: shared business logic, Compose UI, theme system, localization, settings, station data, playback state, and schedule logic
- `androidApp/`: Android host app, `Media3 ExoPlayer`, lifecycle hooks, alarms, back navigation, and APK packaging
- `desktopApp/`: Compose Desktop shell, custom title bar, tray integration, Windows packaging, and desktop window behavior

Core technologies used in the repository:

- Kotlin 2.1.0
- Compose Multiplatform 1.7.3
- Material 3
- Ktor 3
- kotlinx.serialization
- kotlinx.datetime
- Coil 3
- Android `Media3 ExoPlayer`
- Desktop `vlcj` + local HLS proxy for playback
- JVM Preferences / Android persistent settings
- Local app-side multilingual system for Simplified Chinese, Traditional Chinese, English, Japanese, and Korean

## Design Language

The palette is deliberate, not accidental.

- `#D0104C` is **韓紅花 / karakurenai**
- `#005CAF` is **瑠璃 / RURI**
- `#113285` is **紺青 / konjyo**

**韓紅花 / karakurenai** carries the feeling of a vivid historical red bound to dye, fabric, ceremony, and inherited East Asian color naming traditions. During the Heian period, achieving this intensely saturated deep crimson required an enormous quantity of safflower (紅花) petals, making its cost extraordinary. It became a symbol of wealth and power among the highest nobility, and was once declared a "forbidden color" (禁色) by the Japanese imperial court — absolutely prohibited for commoners. In the Hyakunin Isshu, Ariwara no Narihira's iconic verse *"Chihayaburu / kamiyo mo kikazu / Tatsuta-gawa / karakurenai ni / mizu kukuru to wa"* borrows the luxuriance and intensity of karakurenai to describe the overwhelming sight of autumn leaves carpeting the surface of the Tatsuta River.

**瑠璃 / RURI** is counted among Buddhism's Seven Treasures (七宝). The Eastern Pure Land of Yakushi Nyorai — the Buddha of healing and longevity — is called the *Eastern Pure Lapis Lazuli World* (東方淨瑠璃世界). This color has therefore been bound throughout Japanese history to sanctity, purity, and mysterious religious power. Its raw materials could not be produced in ancient Japan; everything had to travel the arduous Silk Road to reach the islands. The Shōsōin treasury in Nara still preserves a lapis lazuli cup (瑠璃杯) from the era of Emperor Shōmu — in that time, it was not merely a color, but an embodiment of imagination toward the distant Western Regions and the Pure Land.

**紺青 / konjyo** brings the deep blue of textiles, pigment, night, sea, and distance. In early Japanese painting, konjyo was a traditional natural mineral pigment ground from azurite (藍銅鉱). Artisans crushed the ore and separated it through levigation; the coarsest, deepest layer yielded konjyo. It was used widely in the tomb murals of the Asuka and Nara periods — including the famous Takamatsu-zuka kofun — and in fine folding-screen paintings. In the late Edo period, a synthetic pigment from Europe, Prussian Blue, arrived in Japan and was also called "konjyo" or *bero-ai*. This new konjyo was affordable, vivid, and highly lightfast, breaking entirely through the constraints of traditional blue pigments. Katsushika Hokusai made extensive use of it to create *The Great Wave off Kanagawa*, which shocked the world and directly drove the full flourishing of ukiyo-e landscape prints (名所絵).

All three colors represent Japan's exchange with the wider world. I chose them for this app to say one simple thing: radio crosses the ocean more easily than people do.

## Features

- Windows + Android dual-platform app
- Native station browsing UI
- Responsive now-playing page
- Weekly schedule with current-program auto-focus
- Desktop tray playback controls
- Settings page with startup area, autoplay, sleep timer, alarm, Wi-Fi rules, theme mode, and desktop close behavior
- Driving mode
- Local multi-language UI

## Project Structure

```text
radiko-app/
|-- androidApp/
|-- desktopApp/
|-- shared/
|-- tools/
|-- .github/assets/
`-- README.md
```

## Acknowledgements

Special thanks to:

- [jackyzy823/rajiko](https://github.com/jackyzy823/rajiko)
- Codex
- Claude Code

## Follow

If you like this project, please follow my work and social platforms:

- [baudstudio.com](https://baudstudio.com)

## Disclaimer

Radikall is an unofficial third-party client and is not affiliated with, endorsed by, or connected to Radiko Co., Ltd. or its broadcasting partners in any way.

Radiko® is a registered trademark of Radiko Co., Ltd. All radio content, station logos, program data, and audio streams accessed through this application are the property of Radiko Co., Ltd. and their respective broadcasters. This application does not host, store, or redistribute any such content.

This project is intended solely for personal use and educational / technical research purposes. Commercial use of this software is not permitted.

This software accesses data through publicly observable network interfaces. Users are solely responsible for ensuring their usage complies with the laws, regulations, and terms of service applicable in their jurisdiction.

By using this software, you acknowledge that you do so at your own risk and that the developer(s) of this project assume no liability for any legal, technical, or other consequences arising from its use.

## Web App (Free Cloudflare Route)

The PWA / Home Screen Web App can now be deployed on Cloudflare Workers without paying for a separate always-on server.

- Public URL: [https://radikall-web.baudmusic.workers.dev](https://radikall-web.baudmusic.workers.dev)
- Public entry: a stable `workers.dev` URL
- Primary target: iPhone Safari + Home Screen Web App
- API + static files: served by the same Worker
- Playback: HLS-first route, no `ffmpeg` / `audio.mp3` fallback in the free production path

Quick start:

```bash
cd webApp
npm install
npm run build:cloudflare
npx wrangler login
npx wrangler secret put RADIKALL_SESSION_SECRET
npm run deploy:cloudflare
```

Deployment details:

- Worker config: [wrangler.jsonc](E:/YLY/RADIKO/radiko-app/wrangler.jsonc)
- Worker entry: [index.ts](E:/YLY/RADIKO/radiko-app/webApp/worker/src/index.ts)
- Static data generator: [generate-worker-data.mjs](E:/YLY/RADIKO/radiko-app/webApp/scripts/generate-worker-data.mjs)
- Full deployment guide: [cloudflare-workers-free-deploy.md](E:/YLY/RADIKO/radiko-app/docs/cloudflare-workers-free-deploy.md)

User distribution guidance:

- Give users [https://radikall-web.baudmusic.workers.dev](https://radikall-web.baudmusic.workers.dev) directly
- iPhone / iPad users should open it in Safari and add it to the Home Screen
- GitHub Releases should remain a changelog / backup entry, not the main Web install path
