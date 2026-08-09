import { api } from '@/lib/axios'
import type { FulfillmentUpdateDTO, OrderDTO, OrderItemDTO, OrderResponse } from '@/api/types'

export function getAdminOrders(pageNumber: number) {
  return api
    .get<OrderResponse>('/api/admin/orders', {
      params: { pageNumber, sortBy: 'orderId', sortOrder: 'desc' },
    })
    .then((res) => res.data)
}

export function updateOrderStatus(orderId: number, status: string) {
  return api
    .put<OrderDTO>(`/api/admin/orders/${orderId}/status`, { status })
    .then((res) => res.data)
}

export function updateOrderItemFulfillment(orderItemId: number, payload: FulfillmentUpdateDTO) {
  return api
    .put<OrderItemDTO>(`/api/admin/order-items/${orderItemId}/fulfillment`, payload)
    .then((res) => res.data)
}
