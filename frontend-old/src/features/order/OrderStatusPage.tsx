import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuthStore } from '@/features/auth/store'
import { ProviderPicker, type ProviderName } from '@/features/checkout/ProviderPicker'
import { RazorpayCheckout } from '@/features/checkout/RazorpayCheckout'
import { StripePaymentForm } from '@/features/checkout/StripePaymentForm'
import { useRetryPayment } from '@/features/checkout/hooks'
import { useOrder } from '@/features/order/hooks'
import { getErrorMessage } from '@/lib/errors'
import { formatMoney } from '@/lib/money'

export function OrderStatusPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const orderIdNum = orderId ? Number(orderId) : undefined
  const email = useAuthStore((s) => s.user?.email)
  const { data: order, isPending, isError, refetch } = useOrder(orderIdNum)
  const retryMutation = useRetryPayment()
  const [retryProvider, setRetryProvider] = useState<ProviderName | undefined>()

  function handleRetry() {
    if (!orderIdNum || !retryProvider) return
    retryMutation.mutate(
      { orderId: orderIdNum, payload: { provider: retryProvider }, idempotencyKey: crypto.randomUUID() },
      { onError: (err) => toast.error(getErrorMessage(err, 'Could not retry payment')) }
    )
  }

  function handlePaymentSuccess() {
    retryMutation.reset()
    refetch()
  }

  if (isPending) {
    return (
      <div className="mx-auto flex max-w-md flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    )
  }

  if (isError || !order) {
    return <p className="text-sm text-destructive">Couldn't load this order.</p>
  }

  const currency = order.currency ?? 'INR'
  const retryResponse = retryMutation.data

  return (
    <div className="mx-auto flex max-w-md flex-col gap-6">
      <h1 className="text-xl font-semibold">Order #{order.orderId}</h1>

      <div className="flex flex-col gap-2 rounded-xl border border-border p-4">
        {order.orderItems?.map((item) => (
          <div key={item.orderItemId} className="flex justify-between text-sm">
            <span>
              {item.product?.productName} × {item.quantity}
            </span>
            <span>
              {formatMoney(
                (item.orderedProductPriceMinorUnits ?? 0) * (item.quantity ?? 0),
                item.currency ?? currency
              )}
            </span>
          </div>
        ))}
        <div className="mt-2 flex justify-between border-t border-border pt-2 font-semibold">
          <span>Total</span>
          <span>{formatMoney(order.amountMinorUnits ?? 0, currency)}</span>
        </div>
      </div>

      {order.orderStatus === 'PAID' && (
        <div className="rounded-xl border border-primary/30 bg-primary/5 p-4 text-sm">
          Payment confirmed. Thanks for your order!
        </div>
      )}

      {order.orderStatus === 'PENDING_PAYMENT' && retryResponse && !retryResponse.paymentAttemptFailed && (
        <div className="flex flex-col gap-3 rounded-xl border border-border p-4">
          {retryResponse.provider === 'STRIPE' && retryResponse.clientSecret && (
            <StripePaymentForm
              clientSecret={retryResponse.clientSecret}
              onSuccess={handlePaymentSuccess}
              onError={(msg) => toast.error(msg)}
            />
          )}
          {retryResponse.provider === 'RAZORPAY' && retryResponse.clientPayload && (
            <RazorpayCheckout
              clientPayload={retryResponse.clientPayload}
              email={email ?? ''}
              onSuccess={handlePaymentSuccess}
              onError={(msg) => toast.error(msg)}
            />
          )}
        </div>
      )}

      {order.orderStatus === 'PENDING_PAYMENT' && (!retryResponse || retryResponse.paymentAttemptFailed) && (
        <div className="flex flex-col gap-3 rounded-xl border border-border p-4 text-sm">
          <p className="text-muted-foreground">Waiting for payment confirmation...</p>
          <p className="text-muted-foreground">
            If your payment didn't go through, you can try again below.
          </p>
          {retryResponse?.paymentAttemptFailed && (
            <p className="text-destructive">
              {retryResponse.failureReason ?? 'The retry attempt failed too.'}
            </p>
          )}
          <ProviderPicker
            value={retryProvider}
            onChange={setRetryProvider}
            exclude={retryResponse?.paymentAttemptFailed ? retryResponse.provider : undefined}
          />
          <Button disabled={!retryProvider || retryMutation.isPending} onClick={handleRetry}>
            {retryMutation.isPending ? 'Retrying...' : 'Retry payment'}
          </Button>
        </div>
      )}

      {(order.orderStatus === 'PAYMENT_FAILED' || order.orderStatus === 'CANCELLED') && (
        <div className="flex flex-col gap-3 rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm">
          <p className="text-destructive">This order's payment did not succeed.</p>
          <Link to="/checkout" className="text-primary underline-offset-4 hover:underline">
            Start a new checkout
          </Link>
        </div>
      )}
    </div>
  )
}
