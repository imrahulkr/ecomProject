const trimSlash = (s: string) => s.replace(/\/+$/, "");

const apiBaseUrl = trimSlash(import.meta.env.VITE_API_BASE_URL ?? "");

export const env = {
  apiBaseUrl,
  backendOrigin: trimSlash(import.meta.env.VITE_BACKEND_ORIGIN || "http://localhost:8080"),
  imageBaseUrl: trimSlash(import.meta.env.VITE_IMAGE_BASE_URL || `${apiBaseUrl}/images`),
  stripePublishableKey: (import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY as string | undefined) || "",
};
