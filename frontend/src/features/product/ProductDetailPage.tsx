import { Link, useParams } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { formatMoney } from '@/lib/money'
import { useProduct } from '@/features/product/hooks'

export function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>()
  const { data: product, isPending, isError } = useProduct(productId)

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

          <Badge variant={product.quantity && product.quantity > 0 ? 'outline' : 'destructive'} className="w-fit">
            {product.quantity && product.quantity > 0 ? `${product.quantity} in stock` : 'Out of stock'}
          </Badge>

          {product.description && (
            <p className="text-sm text-muted-foreground">{product.description}</p>
          )}
        </div>
      </div>
    </div>
  )
}
