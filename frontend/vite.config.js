import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The dev server proxies API calls to the Spring Boot backend on :8080, so the
// browser only ever talks to the Vite origin (no CORS setup needed).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/orders': 'http://localhost:8080',
      '/orderbook': 'http://localhost:8080',
      '/trades': 'http://localhost:8080',
    },
  },
})
