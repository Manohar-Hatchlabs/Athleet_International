import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server proxies /api to the backend so the refresh cookie stays same-origin (SameSite=Strict).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: false } },
  },
  build: { sourcemap: false },
});
