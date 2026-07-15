import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  base: '/game-library/',
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/game-library/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
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
    outDir: 'dist',
    assetsDir: 'assets'
  }
})
