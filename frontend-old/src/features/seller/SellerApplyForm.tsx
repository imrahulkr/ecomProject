import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

const applySchema = z.object({
  businessName: z.string().min(3, 'Business name must be at least 3 characters').max(255),
  businessDescription: z.string().max(2000).optional(),
})

export type ApplySellerFormValues = z.infer<typeof applySchema>

export function SellerApplyForm({
  onSubmit,
  isSubmitting,
}: {
  onSubmit: (values: ApplySellerFormValues) => void
  isSubmitting: boolean
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ApplySellerFormValues>({
    resolver: zodResolver(applySchema),
    defaultValues: { businessName: '', businessDescription: '' },
  })

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="flex flex-col gap-3 rounded-xl border border-border p-4"
    >
      <div className="flex flex-col gap-1">
        <label htmlFor="businessName" className="text-sm font-medium">
          Business name
        </label>
        <Input id="businessName" aria-invalid={!!errors.businessName} {...register('businessName')} />
        {errors.businessName && (
          <p className="text-xs text-destructive">{errors.businessName.message}</p>
        )}
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="businessDescription" className="text-sm font-medium">
          Description (optional)
        </label>
        <textarea
          id="businessDescription"
          className="min-h-24 rounded-lg border border-border bg-background px-2.5 py-1.5 text-sm"
          {...register('businessDescription')}
        />
        {errors.businessDescription && (
          <p className="text-xs text-destructive">{errors.businessDescription.message}</p>
        )}
      </div>

      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Submitting…' : 'Submit application'}
      </Button>
    </form>
  )
}
