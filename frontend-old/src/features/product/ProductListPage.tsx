import { useSearchParams } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useCategories } from '@/features/category/hooks'
import { ProductCard } from '@/features/product/ProductCard'
import { useProducts } from '@/features/product/hooks'

const SORT_OPTIONS = [
  { value: 'productId:asc', label: 'Newest first', sortBy: 'productId', sortOrder: 'asc' as const },
  { value: 'productName:asc', label: 'Name: A to Z', sortBy: 'productName', sortOrder: 'asc' as const },
  {
    value: 'specialPriceMinorUnits:asc',
    label: 'Price: low to high',
    sortBy: 'specialPriceMinorUnits',
    sortOrder: 'asc' as const,
  },
  {
    value: 'specialPriceMinorUnits:desc',
    label: 'Price: high to low',
    sortBy: 'specialPriceMinorUnits',
    sortOrder: 'desc' as const,
  },
]

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  const keyword = searchParams.get('keyword') ?? ''
  const category = searchParams.get('category') ?? ''
  const pageNumber = Number(searchParams.get('pageNumber') ?? '0')
  const sortValue = searchParams.get('sort') ?? SORT_OPTIONS[0].value
  const sortOption = SORT_OPTIONS.find((o) => o.value === sortValue) ?? SORT_OPTIONS[0]

  const { data: categoriesData } = useCategories()
  const { data, isPending, isError } = useProducts({
    keyword: keyword || undefined,
    category: category || undefined,
    pageNumber,
    sortBy: sortOption.sortBy,
    sortOrder: sortOption.sortOrder,
  })

  function updateParams(updates: Record<string, string | undefined>) {
    const next = new URLSearchParams(searchParams)
    for (const [key, value] of Object.entries(updates)) {
      if (value) next.set(key, value)
      else next.delete(key)
    }
    setSearchParams(next)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center gap-3">
        <select
          className="h-8 rounded-lg border border-border bg-background px-2.5 text-sm"
          value={category}
          onChange={(e) => updateParams({ category: e.target.value || undefined, pageNumber: undefined })}
          aria-label="Filter by category"
        >
          <option value="">All categories</option>
          {categoriesData?.content?.map((c) => (
            <option key={c.categoryId} value={c.categoryName}>
              {c.categoryName}
            </option>
          ))}
        </select>

        <select
          className="h-8 rounded-lg border border-border bg-background px-2.5 text-sm"
          value={sortOption.value}
          onChange={(e) => updateParams({ sort: e.target.value, pageNumber: undefined })}
          aria-label="Sort products"
        >
          {SORT_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>

        {keyword && (
          <span className="text-sm text-muted-foreground">
            Results for “{keyword}”
          </span>
        )}
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

      {data && data.content?.length === 0 && (
        <p className="text-sm text-muted-foreground">No products found.</p>
      )}

      {data && (data.content?.length ?? 0) > 0 && (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {data.content!.map((product) => (
              <ProductCard key={product.productId} product={product} />
            ))}
          </div>

          <div className="flex items-center justify-center gap-3">
            <Button
              variant="outline"
              disabled={pageNumber === 0}
              onClick={() => updateParams({ pageNumber: String(pageNumber - 1) })}
            >
              Previous
            </Button>
            <span className="text-sm text-muted-foreground">
              Page {pageNumber + 1} of {Math.max(data.totalPages ?? 1, 1)}
            </span>
            <Button
              variant="outline"
              disabled={data.lastPage}
              onClick={() => updateParams({ pageNumber: String(pageNumber + 1) })}
            >
              Next
            </Button>
          </div>
        </>
      )}
    </div>
  )
}
