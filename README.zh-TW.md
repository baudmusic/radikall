<h1 align="center">Radikall</h1>

<p align="center">
  <img src=".github/assets/logo3.png" alt="Radikall logo" width="260" />
</p>

<p align="center">
  適用於 Windows 和 Android 的日本廣播客戶端，可在全球任何地區收聽全部 Radiko 電台。
</p>

<p align="center">
  <a href="https://baudstudio.com">baudstudio.com</a>
</p>

<p align="center">
  <a href="README.md">English</a> | <a href="README.zh-CN.md">简体中文</a> | <a href="README.zh-TW.md">繁體中文</a> | <a href="README.ja.md">日本語</a> | <a href="README.ko.md">한국어</a>
</p>

## 寫在前面

為了更加方便地暢聽「山下達郎の楽天カード サンデー・ソングブック」（山下達郎的樂天 Card Sunday Songbook）節目，我開發了這個軟體。夏天快到了，該聽《Big Wave》專輯了。

順帶抱怨一下，Claude 真小氣，額度轉瞬即逝。

## 概覽

Radikall 是一款跨平台廣播客戶端，涵蓋 Radiko 官方提供的 47 個都道府縣的所有日本電台，且沒有位置鎖和 IP 鎖，可在世界上任何有網路的地方暢聽。

提供流暢的電台瀏覽體驗、正在播放詳情、每週節目表、Windows 系統匣播放控制，以及跨 Windows 和 Android 的多語言設定系統。

本專案的實現得益於 [jackyzy823/rajiko](https://github.com/jackyzy823/rajiko) 提供的思路、逆向工程路徑和靈感。沒有那個倉庫，這條原生應用路線將難以走通。

## 使用前須知

如果你只是想使用 App 而非從原始碼建置，請先閱讀本節。

### Windows

1. 從 GitHub Releases 頁面下載最新的 Windows 安裝程式。
2. 先安裝 **VLC 媒體播放器**。
3. 安裝 VLC 後再啟動 Radikall，因為桌面端播放依賴透過 `vlcj` 呼叫的 `libVLC`。

推薦環境：

- Windows 10 或 Windows 11
- 64 位元系統
- 穩定的網路連線

### Android

1. 從 GitHub Releases 頁面下載最新的已簽署 Android APK。
2. 若 Android 提示，需要允許安裝來自未知來源的應用程式。
3. 開啟 App，在播放前選擇你的地區。

推薦環境：

- Android 7.0 或更高版本
- 穩定的 Wi-Fi 或行動網路

### iOS

1. 用 Safari 開啟 [https://radikall-web.baudmusic.workers.dev](https://radikall-web.baudmusic.workers.dev)。
2. 儲存到桌面 / 主畫面後即可使用。

### macOS

1. 用 Safari 開啟 [https://radikall-web.baudmusic.workers.dev](https://radikall-web.baudmusic.workers.dev)。
2. 儲存到桌面後即可使用。

### 從原始碼建置

如果你想自行建置專案，請先安裝以下工具：

- Git
- 完整的 JDK 24 安裝
- Node.js LTS 與 npm
- 附有 Android SDK 35 的 Android Studio
- Platform Tools / `adb`
- Windows 上用於桌面播放測試的 VLC 媒體播放器

然後執行：

```powershell
.\gradlew.bat :desktopApp:run
.\gradlew.bat :androidApp:installDebug
```

### Web App 原始碼部署

```bash
cd webApp
npm install
npm run build:cloudflare
npx wrangler login
npx wrangler secret put RADIKALL_SESSION_SECRET
npm run deploy:cloudflare
```

## 發布建置

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

輸出目錄：

- `androidApp/build/outputs/apk/release/`

注意：

- 本專案目前建置**一個通用 Android APK**，未啟用 ABI 拆分或解析度拆分。

## 技術路線

Radikall 採用 Kotlin Multiplatform 架構：

- `shared/`：共用業務邏輯、Compose UI、主題系統、在地化、設定、電台資料、播放狀態和節目表邏輯
- `androidApp/`：Android 宿主應用、`Media3 ExoPlayer`、生命週期掛鉤、鬧鐘、返回導航和 APK 打包
- `desktopApp/`：Compose Desktop 外殼、自訂標題列、系統匣整合、Windows 打包和桌面視窗行為

倉庫使用的核心技術：

- Kotlin 2.1.0
- Compose Multiplatform 1.7.3
- Material 3
- Ktor 3
- kotlinx.serialization
- kotlinx.datetime
- Coil 3
- Android `Media3 ExoPlayer`
- 桌面端 `vlcj` + 本地 HLS 代理用於播放
- JVM Preferences / Android 持久化設定
- 本地應用側多語言系統，支援簡體中文、繁體中文、英語、日語和韓語

## 設計語言

這套配色是刻意為之，而非偶然。

- `#D0104C` 是**韓紅花 / karakurenai**
- `#005CAF` 是**瑠璃 / RURI**
- `#113285` 是**紺青 / konjyo**

**韓紅花 / karakurenai** 承載著歷史性的鮮紅色感，與染料、織物、禮儀和東亞傳統色名相連。在平安時代，要染出這種極度飽和的深紅色，需要消耗極其龐大的紅花（Safflower）花瓣，成本極其高昂。因此，它成為了頂級貴族財富與權力的象徵，曾一度被日本朝廷列為平民絕對禁止使用的「禁色」。《百人一首》中在原業平的絕命名句「神代未聞今日見，龍田川水染唐紅」（千早振る神代もきかず龍田川 からくれなゐに水くくるとは），正是借用「韓紅花」的奢華與濃烈，來形容滿河紅葉鋪滿水面的極致視覺衝擊。

**瑠璃 / RURI** 瑠璃在佛教中被列為「七寶」之一。藥師如來（醫治疾病、消災延壽的佛）所在的東方淨土就叫做「東方淨瑠璃世界」。因此，這種顏色在日本歷史上始終與神聖、清淨、無垢以及神秘的宗教力量綁定。這種色彩曾經由中國、日本、韓國之間的文化通道傳播。古代日本無法出產這種顏色的原料，皆需透過絲綢之路歷經艱險傳入。奈良正倉院至今仍完好保存著聖武天皇時期的「瑠璃杯」，在當時，這不僅是一種顏色，更是代表著對遙遠西域和極樂淨土的想像。

**紺青 / konjyo** 帶來織物、顏料、夜晚、大海和遠方的深藍。在早期的日本畫中，紺青是一種傳統的天然礦物顏料，由「藍銅礦」（Azurite）研磨而成。匠人將礦石粉碎後，利用水飛法分離，顆粒最粗、顏色最深沉的那一層即為紺青。它被廣泛用於飛鳥、奈良時代的古墳壁畫（如著名的高松塚古墳）和高級屏風畫中。到了江戶時代後期，歐洲發明的合成顏料「普魯士藍」（Prussian Blue）傳入日本，也被日本人稱為「紺青」或「ベロ藍」（Bero-ai）。這種新型紺青價格親民、發色鮮艷且不易褪色，徹底打破了傳統藍色顏料的限制。葛飾北齋正是大量使用了這種新式「紺青」，創作出了震驚世界的《神奈川沖浪裏》，直接推動了浮世繪風景畫（名所繪）的全面繁榮。

這三種顏色都代表著「日本與世界的交流」，我在 App 中使用這三種顏色，目的就是傳達一件簡單的事：廣播跨越海洋，比人更容易。

## 功能特性

- Windows + Android 雙平台應用
- 原生電台瀏覽介面
- 響應式正在播放頁面
- 附當前節目自動聚焦的每週節目表
- 桌面系統匣播放控制
- 設定頁面（啟動地區、自動播放、睡眠計時器、鬧鐘、Wi-Fi 規則、主題模式、桌面關閉行為）
- 駕駛模式
- 本地多語言介面

## 專案結構

```text
radiko-app/
|-- androidApp/
|-- desktopApp/
|-- shared/
|-- tools/
|-- .github/assets/
`-- README.md
```

## 致謝

特別感謝：

- [jackyzy823/rajiko](https://github.com/jackyzy823/rajiko)
- Codex
- Claude Code

## 關注

如果你喜歡這個專案，歡迎關注我的作品和社交平台：

- [baudstudio.com](https://baudstudio.com)

## 免責聲明

Radikall 是一款非官方第三方客戶端應用程式，與 Radiko Co., Ltd. 及其廣播合作夥伴沒有任何隸屬、認可或關聯關係。

Radiko® 是 Radiko Co., Ltd. 的注冊商標。透過本應用程式存取的所有廣播內容、電台標誌、節目資料及音訊串流均歸 Radiko Co., Ltd. 及各廣播權利人所有。本應用程式不託管、快取、儲存或再散布任何上述內容。

本專案僅供個人使用及技術學習研究目的。不得將本軟體用於任何商業目的。

本軟體透過公開可觀察的網路介面獲取資料。使用者須自行確認其使用行為符合所在地區適用的法律法規及服務條款。

使用本軟體即表示您已閱讀並理解本聲明，並同意自行承擔相關風險。開發者不對因使用或濫用本軟體而產生的任何法律、技術或其他後果承擔任何責任。
