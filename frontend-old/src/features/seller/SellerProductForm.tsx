import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useCategories } from '@/features/category/hooks'
import type { ProductDTO } from '@/api/types'

const productSchema = z.object({
  productName: z.string().min(3, 'Product name must be at least 3 characters').max(255),
  description: z.string().optional(),
  quantity: z.coerce.number().int('Quantity must be a whole number').min(0, 'Quantity cannot be negative'),
  price: z.coerce.number().positive('Price must be greater than 0'),
  discount: z.coerce.number().min(0, 'Discount cannot be negative').max(100, 'Discount cannot exceed 100'),
  categoryId: z.coerce.number().int().positive('Select a category'),
})

export type ProductFormValues = z.infer<typeof productSchema>

export function SellerProductForm({
  defaultValues,
  onSubmit,
  onCancel,
  isSubmitting,
}: {
  defaultValues?: ProductDTO
  onSubmit: (values: ProductFormValues) => void
  onCancel: () => void
  isSubmitting: boolean
}) {
  const { data: categoriesData } = useCategories()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<z.input<typeof productSchema>, any, ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      productName: defaultValues?.productName ?? '',
      description: defaultValues?.description ?? '',
      quantity: defaultValues?.quantity ?? 0,
      price: defaultValues ? (defaultValues.priceMinorUnits ?? 0) / 100 : 0,
      discount: defaultValues?.discount ?? 0,
      categoryId: defaultValues?.categoryId,
    },
  })

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="flex flex-col gap-3 rounded-xl border border-border p-4"
    >
      <div className="flex flex-col gap-1">
        <label htmlFor="productName" className="text-sm font-medium">
          Product name
        </label>
        <Input id="productName" aria-invalid={!!errors.productName} {...register('productName')} />
        {errors.productName && (
          <p className="text-xs text-destructive">{errors.productName.message}</p>
        )}
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="description" className="text-sm font-medium">
          Description
        </label>
        <textarea
          id="description"
          className="min-h-20 rounded-lg border border-border bg-background px-2.5 py-1.5 text-sm"
          {...register('description')}
        />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="categoryId" className="text-sm font-medium">
          Category
        </label>
        <select
          id="categoryId"
          className="h-8 rounded-lg border border-border bg-background px-2.5 text-sm"
          aria-invalid={!!errors.categoryId}
          defaultValue={defaultValues?.categoryId ?? ''}
          {...register('categoryId')}
        >
          <option value="" disabled>
            Select a category
          </option>
          {categoriesData?.content?.map((c) => (
            <option key={c.categoryId} value={c.categoryId}>
              {c.categoryName}
            </option>
          ))}
        </select>
        {errors.categoryId && (
          <p className="text-xs text-destructive">{errors.categoryId.message}</p>
        )}
      </div>

      <div className="grid grid-cols-3 gap-3">
        <div className="flex flex-col gap-1">
          <label htmlFor="price" className="text-sm font-medium">
            Price (₹)
          </label>
          <Input
            id="price"
            type="number"
            step="0.01"
            min="0"
            aria-invalid={!!errors.price}
            {...register('price')}
          />
          {errors.price && <p className="text-xs text-destructive">{errors.price.message}</p>}
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="discount" className="text-sm font-medium">
            Discount (%)
          </label>
          <Input
            id="discount"
            type="number"
            step="0.01"
            min="0"
            max="100"
            aria-invalid={!!errors.discount}
            {...register('discount')}
          />
          {errors.discount && <p className="text-xs text-destructive">{errors.discount.message}</p>}
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="quantity" className="text-sm font-medium">
            Quantity
          </label>
          <Input
            id="quantity"
            type="number"
            step="1"
            min="0"
            aria-invalid={!!errors.quantity}
            {...register('quantity')}
          />
          {errors.quantity && <p className="text-xs text-destructive">{errors.quantity.message}</p>}
        </div>
      </div>

      <div className="flex gap-2 pt-1">
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Saving…' : 'Save product'}
        </Button>
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}
