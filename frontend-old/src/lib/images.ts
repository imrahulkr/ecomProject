const API_BASE = import.meta.env.VITE_API_URL as string

/**
 * Most product-image responses (list/detail GETs) already come back as a full URL
 * (ProductServiceImpl#constructImageUrl). The nested ProductDTO inside an OrderItemDTO is built
 * on a different path that skips that step, so it's a bare uploaded filename - resolve it the
 * same way the backend's image.base.url does.
 */
export function resolveProductImageUrl(image: string | undefined): string | undefined {
  if (!image) return undefined
  if (image.startsWith('http://') || image.startsWith('https://')) return image
  return `${API_BASE}/images/${image}`
}
