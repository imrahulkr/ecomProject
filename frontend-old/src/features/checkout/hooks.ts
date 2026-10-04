import { useMutation, useQueryClient } from '@tanstack/react-query'

import { checkout, retryPayment } from '@/features/checkout/api'
import type { CheckoutRequest, RetryPaymentRequest } from '@/api/types'

export function useCheckout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      payload,
      idempotencyKey,
    }: {
      payload: CheckoutRequest
      idempotencyKey: string
    }) => checkout(payload, idempotencyKey),
    onSuccess: () => {
      // The cart itself is only cleared server-side once the PAID webhook lands, but the stock
      // reservation created here means the cart page's numbers can go stale immediately.
      queryClient.invalidateQueries({ queryKey: ['cart'] })
    },
  })
}

export function useRetryPayment() {
  return useMutation({
    mutationFn: ({
      orderId,
      payload,
      idempotencyKey,
    }: {
      orderId: number
      payload: RetryPaymentRequest
      idempotencyKey: string
    }) => retryPayment(orderId, payload, idempotencyKey),
  })
}
