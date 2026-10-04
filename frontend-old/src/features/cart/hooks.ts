import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import {
  addToCart,
  decrementCartItem,
  getCart,
  incrementCartItem,
  removeFromCart,
} from '@/features/cart/api'
import type { CartDTO } from '@/api/types'
import { getErrorMessage } from '@/lib/errors'
import { useAuthStore } from '@/features/auth/store'

const CART_KEY = ['cart']

export function useCart() {
  const accessToken = useAuthStore((s) => s.accessToken)
  return useQuery({ queryKey: CART_KEY, queryFn: getCart, enabled: !!accessToken })
}

export function useAddToCart() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ productId, quantity }: { productId: number; quantity: number }) =>
      addToCart(productId, quantity),
    onSuccess: (cart) => {
      queryClient.setQueryData<CartDTO>(CART_KEY, cart)
      toast.success('Added to cart')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not add to cart')),
  })
}

function adjustQuantity(cart: CartDTO, productId: number, delta: number): CartDTO {
  const product = cart.products?.find((p) => p.productId === productId)
  if (!product) return cart
  const unitPrice = product.specialPriceMinorUnits ?? 0
  const newQuantity = (product.quantity ?? 0) + delta
  return {
    ...cart,
    totalPriceMinorUnits: (cart.totalPriceMinorUnits ?? 0) + unitPrice * delta,
    products: cart.products?.map((p) =>
      p.productId === productId ? { ...p, quantity: newQuantity } : p
    ),
  }
}

function useQuantityMutation(mutationFn: (productId: number) => Promise<CartDTO>, delta: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onMutate: async (productId: number) => {
      await queryClient.cancelQueries({ queryKey: CART_KEY })
      const previous = queryClient.getQueryData<CartDTO | null>(CART_KEY)
      if (previous) {
        queryClient.setQueryData<CartDTO>(CART_KEY, adjustQuantity(previous, productId, delta))
      }
      return { previous }
    },
    onError: (err, _productId, context) => {
      if (context) queryClient.setQueryData(CART_KEY, context.previous)
      toast.error(getErrorMessage(err, 'Could not update quantity'))
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: CART_KEY }),
  })
}

export function useIncrementCartItem() {
  return useQuantityMutation(incrementCartItem, 1)
}

export function useDecrementCartItem() {
  return useQuantityMutation(decrementCartItem, -1)
}

export function useRemoveFromCart() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ cartId, productId }: { cartId: number; productId: number }) =>
      removeFromCart(cartId, productId),
    onMutate: async ({ productId }) => {
      await queryClient.cancelQueries({ queryKey: CART_KEY })
      const previous = queryClient.getQueryData<CartDTO | null>(CART_KEY)
      if (previous) {
        const product = previous.products?.find((p) => p.productId === productId)
        const lineTotal = (product?.specialPriceMinorUnits ?? 0) * (product?.quantity ?? 0)
        queryClient.setQueryData<CartDTO>(CART_KEY, {
          ...previous,
          totalPriceMinorUnits: (previous.totalPriceMinorUnits ?? 0) - lineTotal,
          products: previous.products?.filter((p) => p.productId !== productId),
        })
      }
      return { previous }
    },
    onError: (err, _vars, context) => {
      if (context) queryClient.setQueryData(CART_KEY, context.previous)
      toast.error(getErrorMessage(err, 'Could not remove item'))
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: CART_KEY }),
  })
}
