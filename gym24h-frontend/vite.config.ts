import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    allowedHosts: ['tiger-stature-produce.ngrok-free.dev'],
    proxy: {
      '/auth': 'http://localhost:8080',
      '/me': 'http://localhost:8080',
      '/admin/users': 'http://localhost:8080',
      '/admin/entrances': 'http://localhost:8080',
      '/admin/subscriptions': 'http://localhost:8080',
      '/entrances': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
    },
  },
})