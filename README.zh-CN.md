<h1 align="center">Radikall</h1>

<p align="center">
  <img src=".github/assets/logo3.png" alt="Radikall logo" width="260" />
</p>

<p align="center">
  适用于 Windows 和 Android 的日本广播客户端，可在全球任何地区收听全部Radiko电台。
</p>

<p align="center">
  <a href="https://baudstudio.com">baudstudio.com</a>
</p>

<p align="center">
  <a href="README.md">English</a> | <a href="README.zh-CN.md">简体中文</a> | <a href="README.zh-TW.md">繁體中文</a> | <a href="README.ja.md">日本語</a> | <a href="README.ko.md">한국어</a>
</p>

## 写在前面

为了更加方便地畅听“山下達郎の楽天カード サンデー・ソングブック”（山下达郎的乐天Card Sunday Songbook）节目。我开发了这个软件。夏天要到了，该听《Big Wave》专辑了。

顺便吐槽一下，Claude真抠门，额度转瞬即逝。

## 概览

Radikall 是一款跨平台广播客户端，涵盖Radiko官方提供的47个都道府县的所有日本电台，且没有位置锁和IP锁，可在世界上任何有网络的地方畅听。

提供流畅的电台浏览体验、正在播放详情、每周节目表、Windows 托盘播放控制，以及跨 Windows 和 Android 的多语言设置系统。

本项目的实现得益于 [jackyzy823/rajiko](https://github.com/jackyzy823/rajiko) 提供的思路、逆向工程路径和灵感。没有那个仓库，这条原生应用路线将难以走通。

## 使用前须知

如果你只是想使用 App 而非从源码构建，请先阅读本节。

### Windows

1. 从 GitHub Releases 页面下载最新的 Windows 安装包。
2. 先安装 **VLC 媒体播放器**。
3. 安装 VLC 后再启动 Radikall，因为桌面端播放依赖通过 `vlcj` 调用的 `libVLC`。

推荐环境：

- Windows 10 或 Windows 11
- 64 位系统
- 稳定的网络连接

### Android

1. 从 GitHub Releases 页面下载最新的已签名 Android APK。
2. 如果 Android 提示，需要允许安装来自未知来源的应用。
3. 打开 App，在播放前选择你的地区。

推荐环境：

- Android 7.0 或更高版本
- 稳定的 Wi-Fi 或移动网络

### iOS

1. 用 Safari 打开 [https://radikall-web.baudmusic.workers.dev](https://radikall-web.baudmusic.workers.dev)。
2. 保存到桌面 / 主屏幕后即可使用。

### macOS

1. 用 Safari 打开 [https://radikall-web.baudmusic.workers.dev](https://radikall-web.baudmusic.workers.dev)。
2. 保存到桌面后即可使用。

### 从源码构建

如果你想自行构建项目，请先安装以下工具：

- Git
- 完整的 JDK 24 安装
- Node.js LTS 与 npm
- 带有 Android SDK 35 的 Android Studio
- Platform Tools / `adb`
- Windows 上用于桌面播放测试的 VLC 媒体播放器

然后运行：

```powershell
.\gradlew.bat :desktopApp:run
.\gradlew.bat :androidApp:installDebug
```

### Web App 源码部署

```bash
cd webApp
npm install
npm run build:cloudflare
npx wrangler login
npx wrangler secret put RADIKALL_SESSION_SECRET
npm run deploy:cloudflare
```

## 发布构建

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

输出目录：

- `androidApp/build/outputs/apk/release/`

注意：

- 本项目目前构建**一个通用 Android APK**，未启用 ABI 拆分或分辨率拆分。

## 技术路线

Radikall 采用 Kotlin Multiplatform 架构：

- `shared/`：共享业务逻辑、Compose UI、主题系统、本地化、设置、电台数据、播放状态和节目表逻辑
- `androidApp/`：Android 宿主应用、`Media3 ExoPlayer`、生命周期钩子、闹钟、返回导航和 APK 打包
- `desktopApp/`：Compose Desktop 外壳、自定义标题栏、托盘集成、Windows 打包和桌面窗口行为

仓库使用的核心技术：

- Kotlin 2.1.0
- Compose Multiplatform 1.7.3
- Material 3
- Ktor 3
- kotlinx.serialization
- kotlinx.datetime
- Coil 3
- Android `Media3 ExoPlayer`
- 桌面端 `vlcj` + 本地 HLS 代理用于播放
- JVM Preferences / Android 持久化设置
- 本地应用侧多语言系统，支持简体中文、繁体中文、英语、日语和韩语

## 设计语言

这套配色是刻意为之，而非偶然。

- `#D0104C` 是**韓紅花 / karakurenai**
- `#005CAF` 是**瑠璃 / RURI**
- `#113285` 是**紺青 / konjyo**

**韓紅花 / karakurenai** 承载着历史性的鲜红色感，与染料、织物、礼仪和东亚传统色名相连。在平安时代，要染出这种极度饱和的深红色，需要消耗极其庞大的红花（Safflower）花瓣，成本极其高昂。因此，它成为了顶级贵族财富与权力的象征，曾一度被日本朝廷列为平民绝对禁止使用的“禁色”。《百人一首》中在原业平的绝命名句“神代未闻今日见，龙田川水染唐红”（千早振る神代もきかず龍田川 からくれなゐに水くくるとは），正是借用“韩红花”的奢华与浓烈，来形容满河红叶铺满水面的极致视觉冲击。

**瑠璃 / RURI** 瑠璃在佛教中被列为“七宝”之一。药师如来（医治疾病、消灾延寿的佛）所在的东方净土就叫做“东方净瑠璃世界”。因此，这种颜色在日本历史上始终与神圣、清净、无垢以及神秘的宗教力量绑定。这种色彩曾经由中国、日本、韩国之间的文化通道传播。古代日本无法出产这种颜色的原料，皆需通过丝绸之路历经艰险传入。奈良正仓院至今仍完好保存着圣武天皇时期的“瑠璃杯”，在当时，这不仅是一种颜色，更是代表着对遥远西域和极乐净土的想象。

**紺青 / konjyo** 带来织物、颜料、夜晚、大海和远方的深蓝。在早期的日本画中，紺青是一种传统的天然矿物颜料，由“蓝铜矿”（Azurite）研磨而成。匠人将矿石粉碎后，利用水飞法分离，颗粒最粗、颜色最深沉的那一层即为紺青。它被广泛用于飞鸟、奈良时代的古坟壁画（如著名的高松塚古坟）和高级屏风画中。到了江户时代后期，欧洲发明的合成颜料“普鲁士蓝”（Prussian Blue）传入日本，也被日本人称为“紺青”或“维罗林蓝”（Bero-ai）。这种新型紺青价格亲民、发色鲜艳且不易褪色，彻底打破了传统蓝色颜料的限制。葛饰北斋正是大量使用了这种新式“紺青”，创作出了震惊世界的《神奈川冲浪里》，直接推动了浮世绘风景画（名所绘）的全面繁荣。

这三种颜色都代表着“日本与世界的交流”，我在APP中使用这三种颜色，目的就是传达一件简单的事：广播跨越海洋，比人更容易。

## 功能特性

- Windows + Android 双平台应用
- 原生电台浏览界面
- 响应式正在播放页面
- 带当前节目自动聚焦的每周节目表
- 桌面托盘播放控制
- 设置页面（启动地区、自动播放、睡眠定时器、闹钟、Wi-Fi 规则、主题模式、桌面关闭行为）
- 驾驶模式
- 本地多语言界面

## 项目结构

```text
radiko-app/
|-- androidApp/
|-- desktopApp/
|-- shared/
|-- tools/
|-- .github/assets/
`-- README.md
```

## 致谢

特别感谢：

- [jackyzy823/rajiko](https://github.com/jackyzy823/rajiko)
- Codex
- Claude Code

## 关注

如果你喜欢这个项目，欢迎关注我的作品和社交平台：

- [baudstudio.com](https://baudstudio.com)

## 免责声明

Radikall 是一款非官方第三方客户端应用，与 Radiko Co., Ltd. 及其广播合作伙伴没有任何隶属、认可或关联关系。

Radiko® 是 Radiko Co., Ltd. 的注册商标。通过本应用访问的所有广播内容、电台标志、节目数据及音频流均归 Radiko Co., Ltd. 及各广播权利人所有。本应用不托管、缓存、存储或再分发任何上述内容。

本项目仅供个人使用及技术学习研究目的。不得将本软件用于任何商业目的。

本软件通过公开可观察的网络接口获取数据。用户须自行确认其使用行为符合所在地区适用的法律法规及服务条款。

使用本软件即表示您已阅读并理解本声明，并同意自行承担相关风险。开发者不对因使用或滥用本软件而产生的任何法律、技术或其他后果承担任何责任。
