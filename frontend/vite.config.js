// ============================================================
// vite.config.js — Конфигурация Vite для Vue 3 SPA
// ============================================================

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // Базовый путь SPA (соответствует context-path бэкенда)
  base: '/game-library/',
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // Прокси API-запросов на Spring Boot (:8080)
      '/game-library/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // Прокси статических ресурсов бэкенда (Thymeleaf)
      '/game-library/css': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/game-library/js': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/game-library/img': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',       // Директория для собранных файлов
    assetsDir: 'assets'   // Поддиректория для ассетов
  }
})
