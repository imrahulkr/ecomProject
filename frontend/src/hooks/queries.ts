import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  addressApi,
  cartApi,
  catalogApi,
  checkoutApi,
  orderApi,
  reviewApi,
  wishlistApi,
} from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { Cart, PageQuery, ProductQuery } from "@/api/types";
import { useAuthStore } from "@/store/auth";

export const qk = {
  categories: ["categories"] as const,
  products: (q: ProductQuery) => ["products", q] as const,
  productsByCategory: (id: number, q: PageQuery) => ["products", "category", id, q] as const,
  product: (id: number) => ["product", id] as const,
  reviews: (productId: number, q: PageQuery) => ["reviews", productId, q] as const,
  reviewSummary: (productId: number) => ["reviews", productId, "summary"] as const,
  myReviews: (q: PageQuery) => ["my-reviews", q] as const,
  cart: ["cart"] as const,
  wishlist: ["wishlist"] as const,
  addresses: ["addresses"] as const,
  address: (id: number) => ["addresses", id] as const,
  orders: (q: PageQuery) => ["orders", q] as const,
  order: (id: number) => ["order", id] as const,
};

const useIsAuthed = () => useAuthStore((s) => s.status === "authenticated");

// ---------------------------------------------------------------- catalog
export function useCategories() {
  return useQuery({
    queryKey: qk.categories,
    queryFn: () => catalogApi.categories(),
    staleTime: 5 * 60_000,
    select: (page) => page.content,
  });
}

export function useProducts(q: ProductQuery) {
  return useQuery({ queryKey: qk.products(q), queryFn: () => catalogApi.products(q), placeholderData: keepPreviousData });
}

export function useProductsByCategory(categoryId: number | null | undefined, q: PageQuery) {
  return useQuery({
    queryKey: qk.productsByCategory(categoryId ?? 0, q),
    queryFn: () => catalogApi.productsByCategory(categoryId!, q),
    enabled: !!categoryId,
  });
}

export function useProduct(id: number) {
  return useQuery({ queryKey: qk.product(id), queryFn: () => catalogApi.product(id), enabled: Number.isFinite(id) });
}

// ---------------------------------------------------------------- reviews
export function useReviews(productId: number, q: PageQuery) {
  return useQuery({
    queryKey: qk.reviews(productId, q),
    queryFn: () => reviewApi.forProduct(productId, q),
    placeholderData: keepPreviousData,
    enabled: productId > 0,
  });
}

export function useReviewSummary(productId: number) {
  return useQuery({ queryKey: qk.reviewSummary(productId), queryFn: () => reviewApi.summary(productId) });
}

// ---------------------------------------------------------------- cart
export function useCart() {
  const authed = useIsAuthed();
  return useQuery({ queryKey: qk.cart, queryFn: cartApi.get, enabled: authed, staleTime: 30_000 });
}

export function useCartCount() {
  const { data } = useCart();
  return data?.products.reduce((sum, p) => sum + (p.quantity ?? 0), 0) ?? 0;
}

/**
 * The backend computes a coupon's discount once, when applied, and never recomputes it on quantity
 * changes. Re-apply after every mutation so the stored discount matches the new subtotal.
 */
async function refreshCouponIfAny(cart: Cart | null) {
  if (!cart?.appliedCouponCode) return;
  try {
    await cartApi.applyCoupon(cart.appliedCouponCode);
  } catch (err) {
    await cartApi.removeCoupon().catch(() => undefined);
    toast.warning(`Coupon ${cart.appliedCouponCode} was removed`, { description: getErrorMessage(err) });
  }
}

export function useCartActions() {
  const qc = useQueryClient();

  const settle = async () => {
    const cart = await cartApi.get();
    await refreshCouponIfAny(cart);
    await qc.invalidateQueries({ queryKey: qk.cart });
  };

  const onError = (err: unknown) => toast.error(getErrorMessage(err));

  const add = useMutation({
    mutationFn: ({ productId, quantity }: { productId: number; quantity: number }) => cartApi.add(productId, quantity),
    onSuccess: settle,
    onError,
  });
  const increment = useMutation({ mutationFn: cartApi.increment, onSuccess: settle, onError });
  const decrement = useMutation({ mutationFn: cartApi.decrement, onSuccess: settle, onError });
  const remove = useMutation({
    mutationFn: ({ cartId, productId }: { cartId: number; productId: number }) => cartApi.remove(cartId, productId),
    onSuccess: settle,
    onError,
  });
  const clear = useMutation({
    mutationFn: cartApi.clear,
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.cart }),
    onError,
  });
  const applyCoupon = useMutation({
    mutationFn: cartApi.applyCoupon,
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.cart }),
  });
  const removeCoupon = useMutation({
    mutationFn: cartApi.removeCoupon,
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.cart }),
    onError,
  });

  return { add, increment, decrement, remove, clear, applyCoupon, removeCoupon };
}

// ---------------------------------------------------------------- wishlist
export function useWishlist() {
  const authed = useIsAuthed();
  return useQuery({ queryKey: qk.wishlist, queryFn: wishlistApi.list, enabled: authed, staleTime: 60_000 });
}

export function useWishlistToggle() {
  const qc = useQueryClient();
  const { data } = useWishlist();
  const ids = new Set((data ?? []).map((p) => p.productId));

  const mutation = useMutation({
    mutationFn: async (productId: number) => {
      if (ids.has(productId)) await wishlistApi.remove(productId);
      else await wishlistApi.add(productId);
      return !ids.has(productId);
    },
    onSuccess: (added) => {
      toast.success(added ? "Saved to wishlist" : "Removed from wishlist");
      return qc.invalidateQueries({ queryKey: qk.wishlist });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return { isWishlisted: (id: number) => ids.has(id), toggle: mutation.mutate, pendingId: mutation.variables, isPending: mutation.isPending };
}

// ---------------------------------------------------------------- addresses & orders
export function useAddresses() {
  const authed = useIsAuthed();
  return useQuery({ queryKey: qk.addresses, queryFn: addressApi.list, enabled: authed });
}

export function useAddress(id: number | null | undefined) {
  return useQuery({
    queryKey: qk.address(id ?? 0),
    queryFn: () => addressApi.get(id!),
    enabled: !!id,
    retry: false,
  });
}

export function useMyOrders(q: PageQuery) {
  return useQuery({ queryKey: qk.orders(q), queryFn: () => orderApi.mine(q), placeholderData: keepPreviousData });
}

export function useOrder(id: number, opts: { poll?: boolean } = {}) {
  return useQuery({
    queryKey: qk.order(id),
    // While waiting on a payment, go through sync-payment so the backend checks with the
    // provider instead of relying only on the webhook having arrived.
    queryFn: () => (opts.poll ? checkoutApi.syncPayment(id) : orderApi.get(id)),
    enabled: Number.isFinite(id),
    refetchInterval: (query) =>
      opts.poll && query.state.data?.orderStatus === "PENDING_PAYMENT" ? 3000 : false,
  });
}
