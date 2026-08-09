import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAddresses } from '@/features/address/hooks'
import { useAuthStore } from '@/features/auth/store'
import { useCart } from '@/features/cart/hooks'
import { ProviderPicker, type ProviderName } from '@/features/checkout/ProviderPicker'
import { RazorpayCheckout } from '@/features/checkout/RazorpayCheckout'
import { StripePaymentForm } from '@/features/checkout/StripePaymentForm'
import { useCheckout, useRetryPayment } from '@/features/checkout/hooks'
import type { CheckoutResponse } from '@/api/types'
import { getErrorMessage } from '@/lib/errors'
import { formatMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

export function CheckoutPage() {
  const navigate = useNavigate()
  const email = useAuthStore((s) => s.user?.email)
  const { data: addresses, isPending: addressesPending } = useAddresses()
  const { data: cart, isPending: cartPending } = useCart()
  const checkoutMutation = useCheckout()
  const retryMutation = useRetryPayment()

  const [addressId, setAddressId] = useState<number | undefined>()
  const [provider, setProvider] = useState<ProviderName | undefined>()
  const [retryProvider, setRetryProvider] = useState<ProviderName | undefined>()
  const [response, setResponse] = useState<CheckoutResponse | null>(null)

  useEffect(() => {
    if (addressId === undefined && addresses && addresses.length > 0) {
      setAddressId(addresses[0].addressId)
    }
  }, [addresses, addressId])

  function handlePlaceOrder() {
    if (!addressId) return
    checkoutMutation.mutate(
      { payload: { addressId, provider }, idempotencyKey: crypto.randomUUID() },
      {
        onSuccess: setResponse,
        onError: (err) => toast.error(getErrorMessage(err, 'Could not place your order')),
      }
    )
  }

  function handleRetry() {
    if (!response?.orderId || !retryProvider) return
    retryMutation.mutate(
      {
        orderId: response.orderId,
        payload: { provider: retryProvider },
        idempotencyKey: crypto.randomUUID(),
      },
      {
        onSuccess: setResponse,
        onError: (err) => toast.error(getErrorMessage(err, 'Could not retry payment')),
      }
    )
  }

  function handlePaymentSuccess() {
    navigate(`/orders/${response!.orderId}`)
  }

  function handlePaymentError(message: string) {
    toast.error(message)
  }

  if (addressesPending || cartPending) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (!cart || !cart.products || cart.products.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-16 text-center">
        <p className="text-muted-foreground">Your cart is empty.</p>
        <Link to="/" className="text-sm text-primary underline-offset-4 hover:underline">
          Browse products
        </Link>
      </div>
    )
  }

  if (!addresses || addresses.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-16 text-center">
        <p className="text-muted-foreground">Add a delivery address before checking out.</p>
        <Link to="/addresses" className="text-sm text-primary underline-offset-4 hover:underline">
          Add an address
        </Link>
      </div>
    )
  }

  const currency = cart.currency ?? 'INR'

  // Payment step: the checkout call succeeded and the provider needs client-side action.
  if (response && !response.paymentAttemptFailed) {
    return (
      <div className="mx-auto flex max-w-md flex-col gap-6">
        <h1 className="text-xl font-semibold">Complete payment</h1>
        <div className="flex items-center justify-between rounded-xl border border-border p-4">
          <span className="text-sm text-muted-foreground">Amount due</span>
          <span className="font-semibold">
            {formatMoney(response.amountMinorUnits ?? 0, response.currency ?? currency)}
          </span>
        </div>

        {response.provider === 'STRIPE' && response.clientSecret && (
          <StripePaymentForm
            clientSecret={response.clientSecret}
            onSuccess={handlePaymentSuccess}
            onError={handlePaymentError}
          />
        )}

        {response.provider === 'RAZORPAY' && response.clientPayload && (
          <RazorpayCheckout
            clientPayload={response.clientPayload}
            email={email ?? ''}
            onSuccess={handlePaymentSuccess}
            onError={handlePaymentError}
          />
        )}
      </div>
    )
  }

  // Payment step: the provider call itself failed synchronously -- let the user retry with
  // another provider. The order and stock reservation are still intact.
  if (response && response.paymentAttemptFailed) {
    return (
      <div className="mx-auto flex max-w-md flex-col gap-6">
        <h1 className="text-xl font-semibold">Payment could not be started</h1>
        <p className="text-sm text-destructive">
          {response.failureReason ?? 'Something went wrong starting the payment.'}
        </p>
        <ProviderPicker value={retryProvider} onChange={setRetryProvider} exclude={response.provider} />
        <Button size="lg" disabled={!retryProvider || retryMutation.isPending} onClick={handleRetry}>
          {retryMutation.isPending ? 'Retrying...' : 'Try again'}
        </Button>
      </div>
    )
  }

  // Initial step: pick address + provider, review summary, place the order.
  return (
    <div className="flex flex-col gap-8">
      <h1 className="text-xl font-semibold">Checkout</h1>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-medium text-muted-foreground">Delivery address</h2>
        <div className="flex flex-col gap-2" role="radiogroup" aria-label="Delivery address">
          {addresses.map((address) => (
            <button
              key={address.addressId}
              type="button"
              role="radio"
              aria-checked={addressId === address.addressId}
              onClick={() => setAddressId(address.addressId)}
              className={cn(
                'rounded-xl border p-4 text-left text-sm transition-colors',
                addressId === address.addressId
                  ? 'border-primary bg-primary/5'
                  : 'border-border hover:bg-muted'
              )}
            >
              <p className="font-medium">
                {address.buildingName}, {address.street}
              </p>
              <p className="text-muted-foreground">
                {address.city}, {address.state}, {address.country} - {address.pincode}
              </p>
            </button>
          ))}
        </div>
        <Link to="/addresses" className="text-sm text-primary underline-offset-4 hover:underline">
          Manage addresses
        </Link>
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-medium text-muted-foreground">Order summary</h2>
        <div className="flex flex-col gap-2 rounded-xl border border-border p-4">
          {cart.products.map((item) => (
            <div key={item.productId} className="flex justify-between text-sm">
              <span>
                {item.productName} × {item.quantity}
              </span>
              <span>
                {formatMoney(
                  (item.specialPriceMinorUnits ?? 0) * (item.quantity ?? 0),
                  item.currency ?? currency
                )}
              </span>
            </div>
          ))}
          <div className="mt-2 flex justify-between border-t border-border pt-2 font-semibold">
            <span>Total</span>
            <span>{formatMoney(cart.totalPriceMinorUnits ?? 0, currency)}</span>
          </div>
        </div>
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-medium text-muted-foreground">Payment method</h2>
        <ProviderPicker value={provider} onChange={setProvider} allowAuto />
      </section>

      <Button
        size="lg"
        disabled={!addressId || checkoutMutation.isPending}
        onClick={handlePlaceOrder}
      >
        {checkoutMutation.isPending ? 'Placing order...' : 'Place order'}
      </Button>
    </div>
  )
}
