import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { formatMoney } from '@/lib/money'
import type { ProductDTO } from '@/api/types'

export function ProductCard({ product }: { product: ProductDTO }) {
  const hasDiscount = (product.discount ?? 0) > 0

  return (
    <Link to={`/products/${product.productId}`}>
      <Card className="h-full transition-shadow hover:shadow-md">
        <CardHeader className="px-4">
          <div className="aspect-square w-full overflow-hidden rounded-lg bg-muted">
            <img
              src={product.image}
              alt={product.productName}
              className="h-full w-full object-cover"
              onError={(e) => {
                e.currentTarget.style.visibility = 'hidden'
              }}
            />
          </div>
        </CardHeader>
        <CardContent className="flex flex-col gap-1">
          <CardTitle className="line-clamp-2">{product.productName}</CardTitle>
          {product.quantity === 0 && (
            <Badge variant="destructive" className="w-fit">
              Out of stock
            </Badge>
          )}
        </CardContent>
        <CardFooter className="flex items-baseline gap-2">
          <span className="font-semibold">
            {formatMoney(product.specialPriceMinorUnits ?? 0, product.currency ?? 'INR')}
          </span>
          {hasDiscount && (
            <>
              <span className="text-sm text-muted-foreground line-through">
                {formatMoney(product.priceMinorUnits ?? 0, product.currency ?? 'INR')}
              </span>
              <Badge variant="secondary">{product.discount}% off</Badge>
            </>
          )}
        </CardFooter>
      </Card>
    </Link>
  )
}
