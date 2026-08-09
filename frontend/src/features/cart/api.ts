import { api } from '@/lib/axios'
import type { CartDTO } from '@/api/types'

export async function getCart(): Promise<CartDTO | null> {
  try {
    const res = await api.get<CartDTO>('/api/carts/users/cart')
    return res.data
  } catch (err: unknown) {
    // No cart yet for this user -- treat as an empty cart rather than an error.
    if (isNoCartError(err)) return null
    throw err
  }
}

function isNoCartError(err: unknown) {
  if (typeof err !== 'object' || err === null || !('response' in err)) return false
  const response = (err as { response?: { status?: number } }).response
  return response?.status === 400
}

export function addToCart(productId: number, quantity: number) {
  return api
    .post<CartDTO>(`/api/carts/products/${productId}/quantity/${quantity}`)
    .then((res) => res.data)
}

export function incrementCartItem(productId: number) {
  return api
    .put<CartDTO>(`/api/cart/products/${productId}/quantity/increase`)
    .then((res) => res.data)
}

export function decrementCartItem(productId: number) {
  return api.put<CartDTO>(`/api/cart/products/${productId}/quantity/delete`).then((res) => res.data)
}

export function removeFromCart(cartId: number, productId: number) {
  return api.delete<string>(`/api/carts/${cartId}/product/${productId}`).then((res) => res.data)
}
