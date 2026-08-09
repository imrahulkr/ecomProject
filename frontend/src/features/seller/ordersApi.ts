import { api } from '@/lib/axios'
import type { FulfillmentUpdateDTO, OrderItemDTO, OrderResponse } from '@/api/types'

export function getSellerOrders(pageNumber: number) {
  return api
    .get<OrderResponse>('/api/seller/orders', {
      params: { pageNumber, sortBy: 'orderId', sortOrder: 'desc' },
    })
    .then((res) => res.data)
}

export function updateFulfillmentStatus(orderItemId: number, payload: FulfillmentUpdateDTO) {
  return api
    .put<OrderItemDTO>(`/api/seller/order-items/${orderItemId}/fulfillment`, payload)
    .then((res) => res.data)
}
