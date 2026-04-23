# Radikall 1.0.0

## 中文

Web App：
[https://radikall-web.baudmusic.workers.dev](https://radikall-web.baudmusic.workers.dev)

这个版本把 Web App 正式公开到了 Cloudflare Workers。现在用户可以直接打开一个稳定网址，并安装到主屏幕使用，不需要经过 App Store。

更新亮点：

- Web 版正式提供公网入口
- 支持 iPhone Safari 添加到主屏幕
- API 与静态资源由同一个 Worker 提供
- 免费生产链路改为 HLS-first，不再依赖 `ffmpeg` / `audio.mp3` fallback
- Web UI 进一步向 Android 版的结构和交互对齐

使用方式：

- iPhone / iPad：用 Safari 打开这个网址，然后使用“分享 -> 添加到主屏幕”
- Android：用 Chrome 打开同一个网址，然后安装或添加到主屏幕
- 桌面端：直接在浏览器中打开同一个网址即可

说明：

- GitHub Releases 现在主要作为更新日志、备用入口和原生包索引
- Web 版应该通过上面的公网地址分发，而不是让用户下载压缩包

## English

Web App:
[https://radikall-web.baudmusic.workers.dev](https://radikall-web.baudmusic.workers.dev)

This release makes the Web App publicly available through Cloudflare Workers. Users can now open one stable URL and install it to the Home Screen without going through the App Store.

Highlights:

- Public Web App entry on Cloudflare Workers
- iPhone Safari + Add to Home Screen flow
- Same-worker API and static asset delivery
- HLS-first playback path for the free production route, with no `ffmpeg` / `audio.mp3` fallback
- Web UI brought closer to the Android app structure and interaction model

How to use:

- iPhone / iPad: open the URL in Safari, then use Share -> Add to Home Screen
- Android: open the same URL in Chrome, then install or add it to the Home Screen
- Desktop: open the same URL directly in the browser

Notes:

- GitHub Releases now mainly serve as release notes, backup entry, and native package index
- The Web version should be shared through the public URL above, not as a downloaded archive
