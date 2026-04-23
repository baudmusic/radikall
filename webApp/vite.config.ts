import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["apple-touch-icon.png", "favicon-64.png", "icon-192.png", "icon-512.png", "logo3.png"],
      manifest: {
        name: "Radikall Web",
        short_name: "Radikall",
        description: "A standalone-friendly web radio client for Radiko stations.",
        theme_color: "#d0104c",
        background_color: "#f7f4ed",
        display: "standalone",
        orientation: "portrait",
        start_url: "/",
        icons: [
          {
            src: "/icon-192.png",
            sizes: "192x192",
            type: "image/png",
            purpose: "any"
          },
          {
            src: "/icon-512.png",
            sizes: "512x512",
            type: "image/png",
            purpose: "any maskable"
          }
        ]
      },
      workbox: {
        cleanupOutdatedCaches: true,
        clientsClaim: true,
        globPatterns: ["**/*.{js,css,html,svg,png,webmanifest}"],
        skipWaiting: true,
        runtimeCaching: [
          {
            urlPattern: ({ url }) => url.pathname === "/api/bootstrap" || url.pathname === "/api/stations",
            handler: "NetworkFirst",
            options: {
              cacheName: "radikall-api-cache",
              expiration: {
                maxEntries: 24,
                maxAgeSeconds: 300
              }
            }
          },
          {
            urlPattern: ({ url }) => url.pathname.startsWith("/api/stations/"),
            handler: "NetworkOnly"
          },
          {
            urlPattern: ({ url }) => url.pathname.startsWith("/api/playback"),
            handler: "NetworkOnly"
          }
        ]
      }
    })
  ],
  server: {
    host: "0.0.0.0",
    port: 5173,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8787",
        changeOrigin: false
      },
      "/health": {
        target: "http://127.0.0.1:8787",
        changeOrigin: false
      }
    }
  }
});
