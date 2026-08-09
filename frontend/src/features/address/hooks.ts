import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { createAddress, deleteAddress, getAddresses, updateAddress } from '@/features/address/api'
import { getErrorMessage } from '@/lib/errors'
import type { AddressDTO } from '@/api/types'

const ADDRESSES_KEY = ['addresses']

export function useAddresses() {
  return useQuery({ queryKey: ADDRESSES_KEY, queryFn: getAddresses })
}

export function useCreateAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AddressDTO) => createAddress(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADDRESSES_KEY })
      toast.success('Address added')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not add address')),
  })
}

export function useUpdateAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ addressId, payload }: { addressId: number; payload: AddressDTO }) =>
      updateAddress(addressId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADDRESSES_KEY })
      toast.success('Address updated')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not update address')),
  })
}

export function useDeleteAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (addressId: number) => deleteAddress(addressId),
    onMutate: async (addressId) => {
      await queryClient.cancelQueries({ queryKey: ADDRESSES_KEY })
      const previous = queryClient.getQueryData<AddressDTO[]>(ADDRESSES_KEY)
      queryClient.setQueryData<AddressDTO[]>(ADDRESSES_KEY, (old) =>
        old?.filter((a) => a.addressId !== addressId)
      )
      return { previous }
    },
    onError: (err, _addressId, context) => {
      if (context) queryClient.setQueryData(ADDRESSES_KEY, context.previous)
      toast.error(getErrorMessage(err, 'Could not delete address'))
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ADDRESSES_KEY }),
  })
}
