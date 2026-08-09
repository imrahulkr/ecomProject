import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'

import { buttonVariants } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useCategories } from '@/features/category/hooks'
import { ProductCard } from '@/features/product/ProductCard'
import { useProducts } from '@/features/product/hooks'

const CATEGORY_TILE_STYLES = [
  'from-violet-500/15 to-violet-500/5 text-violet-700 dark:text-violet-300',
  'from-sky-500/15 to-sky-500/5 text-sky-700 dark:text-sky-300',
  'from-amber-500/15 to-amber-500/5 text-amber-700 dark:text-amber-300',
  'from-rose-500/15 to-rose-500/5 text-rose-700 dark:text-rose-300',
  'from-emerald-500/15 to-emerald-500/5 text-emerald-700 dark:text-emerald-300',
  'from-indigo-500/15 to-indigo-500/5 text-indigo-700 dark:text-indigo-300',
]

export function HomePage() {
  const { data: categoriesData } = useCategories()
  const { data: productsData, isPending, isError } = useProducts({
    pageSize: 8,
    sortBy: 'productId',
    sortOrder: 'desc',
  })

  const categories = categoriesData?.content ?? []
  const products = productsData?.content ?? []

  return (
    <div className="flex flex-col gap-14">
      <section className="overflow-hidden rounded-3xl border border-border bg-gradient-to-br from-primary/10 via-background to-background px-6 py-16 text-center sm:px-12 sm:py-24">
        <h1 className="text-3xl font-semibold tracking-tight sm:text-5xl">
          Everything you need, from sellers you trust
        </h1>
        <p className="mx-auto mt-4 max-w-xl text-balance text-muted-foreground sm:text-lg">
          Browse thousands of products across every category, with fast checkout and
          secure payments.
        </p>
        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
          <Link to="/products" className={buttonVariants({ size: 'lg' })}>
            Shop all products
            <ArrowRight />
          </Link>
          <Link to="/sell" className={buttonVariants({ variant: 'outline', size: 'lg' })}>
            Become a seller
          </Link>
        </div>
      </section>

      {categories.length > 0 && (
        <section className="flex flex-col gap-5">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-semibold tracking-tight">Shop by category</h2>
          </div>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
            {categories.map((category, i) => (
              <Link
                key={category.categoryId}
                to={`/products?category=${encodeURIComponent(category.categoryName)}`}
                className={`flex aspect-square flex-col items-center justify-center gap-2 rounded-2xl border border-border bg-gradient-to-br p-4 text-center font-medium transition-transform hover:scale-[1.02] ${CATEGORY_TILE_STYLES[i % CATEGORY_TILE_STYLES.length]}`}
              >
                {category.categoryName}
              </Link>
            ))}
          </div>
        </section>
      )}

      <section className="flex flex-col gap-5">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-semibold tracking-tight">Other products</h2>
          <Link
            to="/products"
            className="flex items-center gap-1 text-sm font-medium text-primary hover:underline"
          >
            View all
            <ArrowRight className="size-3.5" />
          </Link>
        </div>

        {isPending && (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <Skeleton key={i} className="aspect-[3/4] w-full" />
            ))}
          </div>
        )}

        {isError && (
          <p className="text-sm text-destructive">Couldn't load products. Please try again.</p>
        )}

        {products.length > 0 && (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {products.map((product) => (
              <ProductCard key={product.productId} product={product} />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
