import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import type { AddressDTO } from '@/api/types'

const addressSchema = z.object({
  street: z.string().min(5, 'Street must be at least 5 characters').max(50),
  buildingName: z.string().min(4, 'Building name must be at least 4 characters'),
  city: z.string().min(4, 'City must be at least 4 characters'),
  state: z.string().min(3, 'State must be at least 3 characters'),
  country: z.string().min(3, 'Country must be at least 3 characters'),
  pincode: z.string().min(6, 'Pincode must be at least 6 characters'),
})

export type AddressFormValues = z.infer<typeof addressSchema>

export function AddressForm({
  defaultValues,
  onSubmit,
  onCancel,
  isSubmitting,
}: {
  defaultValues?: AddressDTO
  onSubmit: (values: AddressFormValues) => void
  onCancel: () => void
  isSubmitting: boolean
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AddressFormValues>({
    resolver: zodResolver(addressSchema),
    defaultValues: {
      street: defaultValues?.street ?? '',
      buildingName: defaultValues?.buildingName ?? '',
      city: defaultValues?.city ?? '',
      state: defaultValues?.state ?? '',
      country: defaultValues?.country ?? '',
      pincode: defaultValues?.pincode ?? '',
    },
  })

  const fields: { name: keyof AddressFormValues; label: string }[] = [
    { name: 'street', label: 'Street' },
    { name: 'buildingName', label: 'Building name' },
    { name: 'city', label: 'City' },
    { name: 'state', label: 'State' },
    { name: 'country', label: 'Country' },
    { name: 'pincode', label: 'Pincode' },
  ]

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="flex flex-col gap-3 rounded-xl border border-border p-4"
    >
      {fields.map(({ name, label }) => (
        <div key={name} className="flex flex-col gap-1">
          <label htmlFor={name} className="text-sm font-medium">
            {label}
          </label>
          <Input id={name} aria-invalid={!!errors[name]} {...register(name)} />
          {errors[name] && (
            <p className="text-xs text-destructive">{errors[name]?.message}</p>
          )}
        </div>
      ))}

      <div className="flex gap-2 pt-1">
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Saving…' : 'Save address'}
        </Button>
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}
