import { Link, useSearchParams } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useOrders } from '@/features/order/hooks'
import { formatMoney } from '@/lib/money'
import type { OrderDTO } from '@/api/types'

const STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  PAID: 'default',
  PENDING_PAYMENT: 'secondary',
  PAYMENT_FAILED: 'destructive',
  CANCELLED: 'outline',
}

const STATUS_LABEL: Record<string, string> = {
  PAID: 'Paid',
  PENDING_PAYMENT: 'Pending payment',
  PAYMENT_FAILED: 'Payment failed',
  CANCELLED: 'Cancelled',
}

function StatusBadge({ status }: { status: OrderDTO['orderStatus'] }) {
  if (!status) return null
  return (
    <Badge variant={STATUS_VARIANT[status] ?? 'outline'}>{STATUS_LABEL[status] ?? status}</Badge>
  )
}

export function OrderListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const pageNumber = Number(searchParams.get('pageNumber') ?? '0')
  const { data, isPending, isError } = useOrders(pageNumber)

  function goToPage(next: number) {
    const params = new URLSearchParams(searchParams)
    params.set('pageNumber', String(next))
    setSearchParams(params)
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Your orders</h1>

      {isPending && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Couldn't load your orders. Please try again.</p>
      )}

      {data && data.content?.length === 0 && (
        <p className="text-sm text-muted-foreground">You haven't placed any orders yet.</p>
      )}

      {data && (data.content?.length ?? 0) > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {data.content!.map((order) => (
              <Link
                key={order.orderId}
                to={`/orders/${order.orderId}`}
                className="flex items-center justify-between gap-4 rounded-xl border border-border p-4 transition-colors hover:bg-accent"
              >
                <div className="flex flex-col gap-1 text-sm">
                  <span className="font-medium">Order #{order.orderId}</span>
                  <span className="text-muted-foreground">{order.orderDate}</span>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-sm font-medium">
                    {formatMoney(order.amountMinorUnits ?? 0, order.currency ?? 'INR')}
                  </span>
                  <StatusBadge status={order.orderStatus} />
                </div>
              </Link>
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
