import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import {
  useAdminOrders,
  useUpdateOrderItemFulfillment,
  useUpdateOrderStatus,
} from '@/features/admin/ordersHooks'
import { resolveProductImageUrl } from '@/lib/images'
import { formatMoney } from '@/lib/money'
import type { OrderDTO, OrderItemDTO } from '@/api/types'

const ORDER_STATUSES = ['PENDING_PAYMENT', 'PAID', 'PAYMENT_FAILED', 'CANCELLED']

const ORDER_STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  PAID: 'default',
  PENDING_PAYMENT: 'secondary',
  PAYMENT_FAILED: 'destructive',
  CANCELLED: 'outline',
}

const FULFILLMENT_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  DELIVERED: 'default',
  SHIPPED: 'secondary',
  PENDING: 'outline',
  CANCELLED: 'destructive',
}

function FulfillmentRow({ item }: { item: OrderItemDTO }) {
  const [trackingNumber, setTrackingNumber] = useState('')
  const [carrier, setCarrier] = useState('')
  const updateMutation = useUpdateOrderItemFulfillment()

  const orderItemId = Number(item.orderItemId)
  const status = item.fulfillmentStatus

  return (
    <div className="flex flex-col gap-2 border-t border-border py-3 first:border-t-0 first:pt-0">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <img
            src={resolveProductImageUrl(item.product?.image)}
            alt={item.product?.productName}
            className="size-12 rounded-lg border border-border object-cover"
          />
          <div className="text-sm">
            <p className="font-medium">{item.product?.productName}</p>
            <p className="text-muted-foreground">
              Qty {item.quantity} ·{' '}
              {formatMoney(item.orderedProductPriceMinorUnits ?? 0, item.currency ?? 'INR')}
            </p>
          </div>
        </div>
        <Badge variant={FULFILLMENT_VARIANT[status ?? ''] ?? 'outline'}>{status}</Badge>
      </div>

      {status === 'PENDING' && (
        <div className="flex flex-wrap items-end gap-2">
          <div className="flex flex-col gap-1">
            <label className="text-xs text-muted-foreground">Tracking number</label>
            <Input
              value={trackingNumber}
              onChange={(e) => setTrackingNumber(e.target.value)}
              className="h-8 w-40"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-xs text-muted-foreground">Carrier</label>
            <Input value={carrier} onChange={(e) => setCarrier(e.target.value)} className="h-8 w-32" />
          </div>
          <Button
            size="sm"
            disabled={!trackingNumber.trim() || !carrier.trim() || updateMutation.isPending}
            onClick={() =>
              updateMutation.mutate({
                orderItemId,
                payload: { status: 'SHIPPED', trackingNumber, carrier },
              })
            }
          >
            Mark shipped
          </Button>
          <Button
            size="sm"
            variant="outline"
            disabled={updateMutation.isPending}
            onClick={() => updateMutation.mutate({ orderItemId, payload: { status: 'CANCELLED' } })}
          >
            Cancel item
          </Button>
        </div>
      )}

      {status === 'SHIPPED' && (
        <div className="flex items-center justify-between gap-2">
          <p className="text-xs text-muted-foreground">
            {item.carrier} · {item.trackingNumber}
          </p>
          <Button
            size="sm"
            disabled={updateMutation.isPending}
            onClick={() => updateMutation.mutate({ orderItemId, payload: { status: 'DELIVERED' } })}
          >
            Mark delivered
          </Button>
        </div>
      )}
    </div>
  )
}

function OrderCard({ order }: { order: OrderDTO }) {
  const [status, setStatus] = useState(order.orderStatus ?? 'PENDING_PAYMENT')
  const updateStatusMutation = useUpdateOrderStatus()

  return (
    <div className="rounded-xl border border-border p-4">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <div className="text-sm">
          <p className="font-medium">Order #{order.orderId}</p>
          <p className="text-muted-foreground">
            {order.email} · {order.orderDate}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium">
            {formatMoney(order.amountMinorUnits ?? 0, order.currency ?? 'INR')}
          </span>
          <Badge variant={ORDER_STATUS_VARIANT[order.orderStatus ?? ''] ?? 'outline'}>
            {order.orderStatus}
          </Badge>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2 rounded-lg bg-muted/50 p-2">
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="h-8 rounded-lg border border-border bg-background px-2.5 text-sm"
        >
          {ORDER_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <Button
          size="sm"
          variant="outline"
          disabled={status === order.orderStatus || updateStatusMutation.isPending || !order.orderId}
          onClick={() => order.orderId && updateStatusMutation.mutate({ orderId: order.orderId, status })}
        >
          Override status
        </Button>
      </div>

      {order.orderItems?.map((item) => <FulfillmentRow key={item.orderItemId} item={item} />)}
    </div>
  )
}

export function AdminOrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const pageNumber = Number(searchParams.get('pageNumber') ?? '0')
  const { data, isPending, isError } = useAdminOrders(pageNumber)

  function goToPage(next: number) {
    const params = new URLSearchParams(searchParams)
    params.set('pageNumber', String(next))
    setSearchParams(params)
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">All orders</h1>

      {isPending && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-32 w-full" />
          ))}
        </div>
      )}

      {isError && <p className="text-sm text-destructive">Couldn't load orders. Please try again.</p>}

      {data && (data.content?.length ?? 0) === 0 && (
        <p className="text-sm text-muted-foreground">No orders yet.</p>
      )}

      {data && (data.content?.length ?? 0) > 0 && (
        <>
          <div className="flex flex-col gap-4">
            {data.content!.map((order) => (
              <OrderCard key={order.orderId} order={order} />
            ))}
          </div>

          <div className="flex items-center justify-center gap-3">
            <Button variant="outline" disabled={pageNumber === 0} onClick={() => goToPage(pageNumber - 1)}>
              Previous
            </Button>
            <span className="text-sm text-muted-foreground">
              Page {pageNumber + 1} of {Math.max(data.totalPages ?? 1, 1)}
            </span>
            <Button variant="outline" disabled={data.lastPage} onClick={() => goToPage(pageNumber + 1)}>
              Next
            </Button>
          </div>
        </>
      )}
    </div>
  )
}
