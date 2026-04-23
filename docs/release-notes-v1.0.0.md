# Radikall 1.0.0

Web App:
[https://radikall-web.baudmusic.workers.dev](https://radikall-web.baudmusic.workers.dev)

This release makes the Web App publicly available through Cloudflare Workers, so users can open one stable URL and install it to the Home Screen without going through the App Store.

Highlights:

- Public PWA entry on Cloudflare Workers
- iPhone Safari + Add to Home Screen flow
- Same-worker API and static asset delivery
- HLS-first playback route for the free production path
- Web UI aligned more closely with the Android app structure and behavior

How to use:

- iPhone / iPad: open the URL in Safari, then use Share -> Add to Home Screen
- Android: open the same URL in Chrome, then install or add to the Home Screen
- Desktop: open the same URL directly in the browser

Notes:

- GitHub Releases now serve as release notes, backup entry, and native package index
- The Web version should be shared through the public URL above, not as a downloaded archive
