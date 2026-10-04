import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { fileURLToPath, URL } from "node:url";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const backend = env.VITE_BACKEND_ORIGIN || "http://localhost:8080";

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
    },
    // Only imported from lazy routes (PaymentPage), so the dep scanner misses them at startup and
    // re-optimizes mid-session, which 504s the old chunk ("Failed to fetch dynamically imported module").
    optimizeDeps: {
      include: ["@stripe/react-stripe-js", "@stripe/stripe-js"],
    },
    server: {
      // Must match FRONTEND_URL on the backend (CORS + email/OAuth redirect links).
      port: 5176,
      strictPort: true,
      // Same-origin API calls in dev: the refresh-token cookie and CORS just work.
      proxy: {
        "/api": { target: backend, changeOrigin: true },
        "/images": { target: backend, changeOrigin: true },
      },
    },
  };
});
