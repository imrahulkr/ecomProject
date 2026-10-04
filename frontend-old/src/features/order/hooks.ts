import { useQuery } from '@tanstack/react-query'

import { getOrder, getOrders } from '@/features/order/api'

const TERMINAL_STATUSES = new Set(['PAID', 'PAYMENT_FAILED', 'CANCELLED'])

export function useOrders(pageNumber: number) {
  return useQuery({
    queryKey: ['orders', pageNumber],
    queryFn: () => getOrders(pageNumber),
  })
}

/** Payment confirmation lands asynchronously via a provider webhook, so the order stays
 * PENDING_PAYMENT for a little while after checkout returns -- poll until it settles. */
export function useOrder(orderId: number | undefined) {
  return useQuery({
    queryKey: ['order', orderId],
    queryFn: () => getOrder(orderId!),
    enabled: !!orderId,
    refetchInterval: (query) => {
      const status = query.state.data?.orderStatus
      return status && TERMINAL_STATUSES.has(status) ? false : 2000
    },
  })
}
