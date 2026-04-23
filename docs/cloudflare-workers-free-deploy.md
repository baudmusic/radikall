# Radikall Web 免费上线到 Cloudflare Workers

这条线路是给 `PWA / 主屏 Web App` 用的免费发布方案。

它的公开入口会是：

- `https://<your-worker-name>.<your-subdomain>.workers.dev`

只要 `Worker 名称` 和你的 `workers.dev` 子域名不变，这个地址就不会因为日常重新部署而自动变化。

## 1. 你需要准备什么

- 一个 Cloudflare 账号
- 本仓库最新代码
- Node.js LTS
- npm
- Wrangler CLI（已经写入 `webApp/package.json` 的 devDependencies）

## 2. 首次登录 Cloudflare

先进入 `webApp` 目录：

```bash
cd webApp
npx wrangler login
```

浏览器会打开 Cloudflare 授权页面。登录并确认即可。

## 3. 生成静态目录数据并构建前端

进入 `webApp` 目录：

```bash
cd webApp
npm install
npm run build:cloudflare
```

这一步会依次完成：

1. 从现有 Kotlin 源文件生成 Worker 用的静态地区 / 电台目录 JSON
2. 构建 `webApp/dist`
3. 对 Worker 代码做 TypeScript 类型检查

## 4. 配置 Worker 密钥

保持在 `webApp` 目录，写入播放 session 加密密钥：

```bash
npx wrangler secret put RADIKALL_SESSION_SECRET
```

建议填一个足够长的随机字符串，例如：

```text
radikall-web-session-2026-please-replace-with-your-own-long-random-secret
```

本地调试可以复制：

- `.dev.vars.example`

为：

- `.dev.vars`

然后写入同名变量。

## 5. 本地调试 Worker

保持在 `webApp` 目录：

```bash
npm run dev:cloudflare
```

可检查：

- `/health`
- `/api/bootstrap`
- `/`

如果需要让 iPhone 局域网访问，可在 `npm run dev:cloudflare` 的基础上再自行配 HTTPS 隧道。

## 6. 正式部署

保持在 `webApp` 目录：

```bash
npm run deploy:cloudflare
```

部署成功后，Wrangler 会输出一个固定的 `workers.dev` 地址。

你后续发给用户的主入口就是这个地址。

## 7. 给用户的最简说明

### iPhone / iPad

1. 用 Safari 打开你的 `workers.dev` 地址
2. 按页面顶部安装提示操作
3. 选择“添加到主屏幕”
4. 从主屏图标打开

### Android

1. 用 Chrome 打开同一个地址
2. 如果浏览器弹出安装按钮，直接安装
3. 如果没有弹出，就按页面顶部提示手动“添加到主屏幕”

## 8. GitHub Release 应该怎么写

GitHub Release 继续保留，但用途改成：

- 更新日志
- 备用网址
- Windows / Android 原生包下载说明

不要再让 Web 用户去下载压缩包。

建议在 Release 顶部固定写：

```text
Web App:
https://<your-worker-name>.<your-subdomain>.workers.dev
```

## 9. 这条免费路线的边界

这条路线的优先目标是：

- iPhone Safari
- 主屏 Web App

它不再包含：

- `audio.mp3` 转码 fallback
- `ffmpeg` 依赖
- 原生 iOS 包装

也就是说：

- iPhone Safari / 主屏 Web App 是第一优先级
- 其他桌面浏览器和 Android 浏览器属于 `best effort`

## 10. 当前配置文件

- Wrangler 配置：[wrangler.jsonc](E:/YLY/RADIKO/radiko-app/wrangler.jsonc)
- Worker 入口：[index.ts](E:/YLY/RADIKO/radiko-app/webApp/worker/src/index.ts)
- 静态目录生成脚本：[generate-worker-data.mjs](E:/YLY/RADIKO/radiko-app/webApp/scripts/generate-worker-data.mjs)
