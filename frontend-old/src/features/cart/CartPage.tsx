import { Link } from 'react-router-dom'

import { Button, buttonVariants } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import {
  useCart,
  useDecrementCartItem,
  useIncrementCartItem,
  useRemoveFromCart,
} from '@/features/cart/hooks'
import { formatMoney } from '@/lib/money'

export function CartPage() {
  const { data: cart, isPending, isError } = useCart()
  const increment = useIncrementCartItem()
  const decrement = useDecrementCartItem()
  const remove = useRemoveFromCart()

  if (isPending) {
    return (
      <div className="flex flex-col gap-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-24 w-full" />
        ))}
      </div>
    )
  }

  if (isError) {
    return <p className="text-sm text-destructive">Couldn't load your cart. Please try again.</p>
  }

  const items = cart?.products ?? []

  if (items.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-16 text-center">
        <p className="text-muted-foreground">Your cart is empty.</p>
        <Link to="/" className="text-sm text-primary underline-offset-4 hover:underline">
          Browse products
        </Link>
      </div>
    )
  }

  const currency = cart?.currency ?? 'INR'

  function handleDecrease(productId: number, quantity: number) {
    if (quantity <= 1) {
      remove.mutate({ cartId: cart!.cartId!, productId })
    } else {
      decrement.mutate(productId)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Your Cart</h1>

      <div className="flex flex-col gap-3">
        {items.map((item) => (
          <div
            key={item.productId}
            className="flex items-center gap-4 rounded-xl border border-border p-3"
          >
            <div className="size-16 shrink-0 overflow-hidden rounded-lg bg-muted">
              <img
                src={item.image}
                alt={item.productName}
                className="h-full w-full object-cover"
                onError={(e) => {
                  e.currentTarget.style.visibility = 'hidden'
                }}
              />
            </div>

            <div className="flex-1">
              <Link
                to={`/products/${item.productId}`}
                className="font-medium hover:underline"
              >
                {item.productName}
              </Link>
              <p className="text-sm text-muted-foreground">
                {formatMoney(item.specialPriceMinorUnits ?? 0, item.currency ?? currency)} each
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="icon-sm"
                aria-label="Decrease quantity"
                onClick={() => handleDecrease(item.productId!, item.quantity ?? 1)}
              >
                −
              </Button>
              <span className="w-6 text-center text-sm">{item.quantity}</span>
              <Button
                variant="outline"
                size="icon-sm"
                aria-label="Increase quantity"
                onClick={() => increment.mutate(item.productId!)}
              >
                +
              </Button>
            </div>

            <div className="w-24 text-right font-medium">
              {formatMoney((item.specialPriceMinorUnits ?? 0) * (item.quantity ?? 0), item.currency ?? currency)}
            </div>

            <Button
              variant="ghost"
              size="sm"
              className="text-destructive"
              onClick={() => remove.mutate({ cartId: cart!.cartId!, productId: item.productId! })}
            >
              Remove
            </Button>
          </div>
        ))}
      </div>

      <div className="flex items-center justify-between border-t border-border pt-4">
        <span className="text-lg font-semibold">Total</span>
        <span className="text-lg font-semibold">
          {formatMoney(cart?.totalPriceMinorUnits ?? 0, currency)}
        </span>
      </div>

      <Link to="/checkout" className={buttonVariants({ size: 'lg' })}>
        Proceed to checkout
      </Link>
    </div>
  )
}
