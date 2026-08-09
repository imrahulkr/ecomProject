import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { AddressForm, type AddressFormValues } from '@/features/address/AddressForm'
import {
  useAddresses,
  useCreateAddress,
  useDeleteAddress,
  useUpdateAddress,
} from '@/features/address/hooks'
import type { AddressDTO } from '@/api/types'

type FormState = { mode: 'create' } | { mode: 'edit'; address: AddressDTO } | null

export function AddressBookPage() {
  const { data: addresses, isPending, isError } = useAddresses()
  const createAddress = useCreateAddress()
  const updateAddress = useUpdateAddress()
  const deleteAddress = useDeleteAddress()
  const [formState, setFormState] = useState<FormState>(null)

  function handleSubmit(values: AddressFormValues) {
    if (formState?.mode === 'edit') {
      updateAddress.mutate(
        { addressId: formState.address.addressId!, payload: values },
        { onSuccess: () => setFormState(null) }
      )
    } else {
      createAddress.mutate(values, { onSuccess: () => setFormState(null) })
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Addresses</h1>
        {!formState && (
          <Button onClick={() => setFormState({ mode: 'create' })}>Add address</Button>
        )}
      </div>

      {formState && (
        <AddressForm
          key={formState.mode === 'edit' ? formState.address.addressId : 'create'}
          defaultValues={formState.mode === 'edit' ? formState.address : undefined}
          onSubmit={handleSubmit}
          onCancel={() => setFormState(null)}
          isSubmitting={createAddress.isPending || updateAddress.isPending}
        />
      )}

      {isPending && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 2 }).map((_, i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Couldn't load your addresses. Please try again.</p>
      )}

      {addresses && addresses.length === 0 && !formState && (
        <p className="text-sm text-muted-foreground">You haven't added any addresses yet.</p>
      )}

      <div className="flex flex-col gap-3">
        {addresses?.map((address) => (
          <div
            key={address.addressId}
            className="flex items-start justify-between gap-4 rounded-xl border border-border p-4"
          >
            <div className="text-sm">
              <p className="font-medium">{address.buildingName}, {address.street}</p>
              <p className="text-muted-foreground">
                {address.city}, {address.state}, {address.country} - {address.pincode}
              </p>
            </div>
            <div className="flex shrink-0 gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setFormState({ mode: 'edit', address })}
              >
                Edit
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="text-destructive"
                onClick={() => deleteAddress.mutate(address.addressId!)}
              >
                Delete
              </Button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
