import { useSearchParams } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import {
  useCreateSellerProduct,
  useDeleteSellerProduct,
  useSellerProducts,
  useUpdateSellerProduct,
  useUploadSellerProductImage,
} from '@/features/seller/productsHooks'
import { SellerProductForm, type ProductFormValues } from '@/features/seller/SellerProductForm'
import { formatMoney } from '@/lib/money'
import type { ProductDTO } from '@/api/types'
import { useState } from 'react'

type FormState = { mode: 'create' } | { mode: 'edit'; product: ProductDTO } | null

export function SellerProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const pageNumber = Number(searchParams.get('pageNumber') ?? '0')
  const [formState, setFormState] = useState<FormState>(null)

  const { data, isPending, isError } = useSellerProducts({ pageNumber })
  const createMutation = useCreateSellerProduct()
  const updateMutation = useUpdateSellerProduct()
  const deleteMutation = useDeleteSellerProduct()
  const uploadImageMutation = useUploadSellerProductImage()

  function goToPage(next: number) {
    const params = new URLSearchParams(searchParams)
    params.set('pageNumber', String(next))
    setSearchParams(params)
  }

  function handleSubmit(values: ProductFormValues) {
    const payload = {
      productName: values.productName,
      description: values.description,
      quantity: values.quantity,
      priceMinorUnits: Math.round(values.price * 100),
      discount: values.discount,
      categoryId: values.categoryId,
    } as ProductDTO

    if (formState?.mode === 'edit') {
      updateMutation.mutate(
        { productId: formState.product.productId!, payload },
        { onSuccess: () => setFormState(null) }
      )
    } else {
      createMutation.mutate(
        { categoryId: values.categoryId, payload },
        { onSuccess: () => setFormState(null) }
      )
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">My products</h1>
        {!formState && <Button onClick={() => setFormState({ mode: 'create' })}>Add product</Button>}
      </div>

      {formState && (
        <SellerProductForm
          key={formState.mode === 'edit' ? formState.product.productId : 'create'}
          defaultValues={formState.mode === 'edit' ? formState.product : undefined}
          onSubmit={handleSubmit}
          onCancel={() => setFormState(null)}
          isSubmitting={createMutation.isPending || updateMutation.isPending}
        />
      )}

      {isPending && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Couldn't load your products. Please try again.</p>
      )}

      {data && data.content?.length === 0 && !formState && (
        <p className="text-sm text-muted-foreground">You haven't listed any products yet.</p>
      )}

      {data && (data.content?.length ?? 0) > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {data.content!.map((product) => (
              <div
                key={product.productId}
                className="flex flex-wrap items-center gap-4 rounded-xl border border-border p-4"
              >
                <img
                  src={product.image}
                  alt={product.productName}
                  className="size-16 rounded-lg border border-border object-cover"
                />
                <div className="flex flex-1 flex-col gap-1 text-sm">
                  <span className="font-medium">{product.productName}</span>
                  <span className="text-muted-foreground">
                    {formatMoney(product.specialPriceMinorUnits ?? 0, product.currency ?? 'INR')}
                    {(product.discount ?? 0) > 0 && (
                      <span className="ml-2 text-xs line-through">
                        {formatMoney(product.priceMinorUnits ?? 0, product.currency ?? 'INR')}
                      </span>
                    )}
                  </span>
                  <span className="text-muted-foreground">
                    Qty: {product.quantity} · {product.discount}% off
                  </span>
                </div>
                <Badge variant={(product.quantity ?? 0) > 0 ? 'default' : 'destructive'}>
                  {(product.quantity ?? 0) > 0 ? 'In stock' : 'Out of stock'}
                </Badge>
                <label
                  className={buttonVariants({ variant: 'outline', size: 'sm' }) + ' cursor-pointer'}
                >
                  Change image
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0]
                      if (file && product.productId) {
                        uploadImageMutation.mutate({ productId: product.productId, file })
                      }
                      e.target.value = ''
                    }}
                  />
                </label>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setFormState({ mode: 'edit', product })}
                >
                  Edit
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-destructive"
                  onClick={() => deleteMutation.mutate(product.productId!)}
                >
                  Delete
                </Button>
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
