import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // 遇到这些开头的请求，前端自动帮你转发给本地后端的 8080
      '/auth': 'http://localhost:8080',
      '/me': 'http://localhost:8080',
      '/entrances': 'http://localhost:8080',
      '/api': 'http://localhost:8080' 
    }
  }
})