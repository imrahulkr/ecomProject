import { api } from '@/lib/axios'
import type { OrderDTO, OrderResponse } from '@/api/types'

export function getOrder(orderId: number) {
  return api.get<OrderDTO>(`/api/orders/${orderId}`).then((res) => res.data)
}

export function getOrders(pageNumber: number) {
  return api
    .get<OrderResponse>('/api/orders', {
      params: { pageNumber, sortBy: 'orderId', sortOrder: 'desc' },
    })
    .then((res) => res.data)
}
