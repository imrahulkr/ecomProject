import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAddToCart } from '@/features/cart/hooks'
import { formatMoney } from '@/lib/money'
import { useProduct } from '@/features/product/hooks'
import { useAuthStore } from '@/features/auth/store'

export function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>()
  const { data: product, isPending, isError } = useProduct(productId)
  const [quantity, setQuantity] = useState(1)
  const addToCart = useAddToCart()
  const accessToken = useAuthStore((s) => s.accessToken)
  const navigate = useNavigate()
  const location = useLocation()

  if (isPending) {
    return (
      <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
        <Skeleton className="aspect-square w-full" />
        <div className="flex flex-col gap-3">
          <Skeleton className="h-8 w-3/4" />
          <Skeleton className="h-5 w-1/3" />
          <Skeleton className="h-24 w-full" />
        </div>
      </div>
    )
  }

  if (isError || !product) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <p className="text-sm text-muted-foreground">Product not found.</p>
        <Link to="/" className="text-sm text-primary underline-offset-4 hover:underline">
          Back to products
        </Link>
      </div>
    )
  }

  const hasDiscount = (product.discount ?? 0) > 0
  const inStock = (product.quantity ?? 0) > 0

  function handleAddToCart() {
    if (!accessToken) {
      navigate('/login', { state: { from: location } })
      return
    }
    addToCart.mutate({ productId: product!.productId!, quantity })
  }

  return (
    <div className="flex flex-col gap-6">
      <Link to="/" className="w-fit text-sm text-muted-foreground hover:text-foreground">
        ← Back to products
      </Link>

      <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
        <div className="aspect-square w-full overflow-hidden rounded-xl bg-muted">
          <img
            src={product.image}
            alt={product.productName}
            className="h-full w-full object-cover"
            onError={(e) => {
              e.currentTarget.style.visibility = 'hidden'
            }}
          />
        </div>

        <div className="flex flex-col gap-4">
          <div>
            <h1 className="text-2xl font-semibold">{product.productName}</h1>
            {product.sellerName && (
              <p className="text-sm text-muted-foreground">Sold by {product.sellerName}</p>
            )}
          </div>

          <div className="flex items-baseline gap-3">
            <span className="text-2xl font-semibold">
              {formatMoney(product.specialPriceMinorUnits ?? 0, product.currency ?? 'INR')}
            </span>
            {hasDiscount && (
              <>
                <span className="text-muted-foreground line-through">
                  {formatMoney(product.priceMinorUnits ?? 0, product.currency ?? 'INR')}
                </span>
                <Badge variant="secondary">{product.discount}% off</Badge>
              </>
            )}
          </div>

          <Badge variant={inStock ? 'outline' : 'destructive'} className="w-fit">
            {inStock ? `${product.quantity} in stock` : 'Out of stock'}
          </Badge>

          {product.description && (
            <p className="text-sm text-muted-foreground">{product.description}</p>
          )}

          {inStock && (
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="icon-sm"
                  aria-label="Decrease quantity"
                  disabled={quantity <= 1}
                  onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                >
                  −
                </Button>
                <span className="w-6 text-center text-sm">{quantity}</span>
                <Button
                  variant="outline"
                  size="icon-sm"
                  aria-label="Increase quantity"
                  disabled={quantity >= (product.quantity ?? 1)}
                  onClick={() => setQuantity((q) => Math.min(product.quantity ?? 1, q + 1))}
                >
                  +
                </Button>
              </div>
              <Button onClick={handleAddToCart} disabled={addToCart.isPending}>
                {addToCart.isPending ? 'Adding…' : 'Add to cart'}
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
