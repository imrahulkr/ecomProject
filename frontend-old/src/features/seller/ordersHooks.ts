import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { getSellerOrders, updateFulfillmentStatus } from '@/features/seller/ordersApi'
import { getErrorMessage } from '@/lib/errors'
import type { FulfillmentUpdateDTO } from '@/api/types'

const SELLER_ORDERS_KEY = ['seller-orders']

export function useSellerOrders(pageNumber: number) {
  return useQuery({
    queryKey: [...SELLER_ORDERS_KEY, pageNumber],
    queryFn: () => getSellerOrders(pageNumber),
  })
}

export function useUpdateFulfillmentStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      orderItemId,
      payload,
    }: {
      orderItemId: number
      payload: FulfillmentUpdateDTO
    }) => updateFulfillmentStatus(orderItemId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_ORDERS_KEY })
      toast.success('Fulfillment status updated')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not update fulfillment status')),
  })
}
