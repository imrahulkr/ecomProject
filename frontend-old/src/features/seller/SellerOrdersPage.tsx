import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { useSellerOrders, useUpdateFulfillmentStatus } from '@/features/seller/ordersHooks'
import { resolveProductImageUrl } from '@/lib/images'
import { formatMoney } from '@/lib/money'
import type { OrderItemDTO } from '@/api/types'

const STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  DELIVERED: 'default',
  SHIPPED: 'secondary',
  PENDING: 'outline',
  CANCELLED: 'destructive',
}

function FulfillmentRow({ item }: { item: OrderItemDTO }) {
  const [trackingNumber, setTrackingNumber] = useState('')
  const [carrier, setCarrier] = useState('')
  const updateMutation = useUpdateFulfillmentStatus()

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
        <Badge variant={STATUS_VARIANT[status ?? ''] ?? 'outline'}>{status}</Badge>
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
            onClick={() =>
              updateMutation.mutate({ orderItemId, payload: { status: 'CANCELLED' } })
            }
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
            onClick={() =>
              updateMutation.mutate({ orderItemId, payload: { status: 'DELIVERED' } })
            }
          >
            Mark delivered
          </Button>
        </div>
      )}
    </div>
  )
}

export function SellerOrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const pageNumber = Number(searchParams.get('pageNumber') ?? '0')
  const { data, isPending, isError } = useSellerOrders(pageNumber)

  function goToPage(next: number) {
    const params = new URLSearchParams(searchParams)
    params.set('pageNumber', String(next))
    setSearchParams(params)
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Orders to fulfill</h1>

      {isPending && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
      )}

      {isError && <p className="text-sm text-destructive">Couldn't load orders. Please try again.</p>}

      {data && data.content?.length === 0 && (
        <p className="text-sm text-muted-foreground">No orders yet.</p>
      )}

      {data && (data.content?.length ?? 0) > 0 && (
        <>
          <div className="flex flex-col gap-4">
            {data.content!.map((order) => (
              <div key={order.orderId} className="rounded-xl border border-border p-4">
                <div className="mb-2 flex items-center justify-between text-sm">
                  <span className="font-medium">Order #{order.orderId}</span>
                  <span className="text-muted-foreground">{order.orderDate}</span>
                </div>
                {order.orderItems?.map((item) => (
                  <FulfillmentRow key={item.orderItemId} item={item} />
                ))}
              </div>
            ))}
          </div>

          <div className="flex items-center justify-center gap-3">
            <Button
              variant="outline"
              disabled={pageNumber === 0}
              onClick={() => goToPage(pageNumber - 1)}
            >
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
