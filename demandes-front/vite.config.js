import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Port fixe : il doit correspondre aux "Valid redirect URIs" et "Web origins" de Keycloak
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
})
