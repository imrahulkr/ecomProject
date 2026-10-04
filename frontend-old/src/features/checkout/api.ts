import { api } from '@/lib/axios'
import type { CheckoutRequest, CheckoutResponse, RetryPaymentRequest } from '@/api/types'

export function checkout(payload: CheckoutRequest, idempotencyKey: string) {
  return api
    .post<CheckoutResponse>('/api/checkout', payload, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    .then((res) => res.data)
}

export function retryPayment(
  orderId: number,
  payload: RetryPaymentRequest,
  idempotencyKey: string
) {
  return api
    .post<CheckoutResponse>(`/api/checkout/${orderId}/retry-payment`, payload, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    .then((res) => res.data)
}
