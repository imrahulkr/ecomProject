import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import {
  getAdminOrders,
  updateOrderItemFulfillment,
  updateOrderStatus,
} from '@/features/admin/ordersApi'
import { getErrorMessage } from '@/lib/errors'
import type { FulfillmentUpdateDTO } from '@/api/types'

const ADMIN_ORDERS_KEY = ['admin-orders']

export function useAdminOrders(pageNumber: number) {
  return useQuery({
    queryKey: [...ADMIN_ORDERS_KEY, pageNumber],
    queryFn: () => getAdminOrders(pageNumber),
  })
}

export function useUpdateOrderStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ orderId, status }: { orderId: number; status: string }) =>
      updateOrderStatus(orderId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_ORDERS_KEY })
      toast.success('Order status updated')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not update order status')),
  })
}

export function useUpdateOrderItemFulfillment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      orderItemId,
      payload,
    }: {
      orderItemId: number
      payload: FulfillmentUpdateDTO
    }) => updateOrderItemFulfillment(orderItemId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_ORDERS_KEY })
      toast.success('Fulfillment status updated')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not update fulfillment status')),
  })
}
