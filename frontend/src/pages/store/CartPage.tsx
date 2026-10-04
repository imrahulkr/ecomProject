import { useState, type FormEvent, type ReactNode } from "react";
import { Link, useNavigate } from "react-router";
import { ArrowRight, Lock, ShoppingCart, Tag, Trash, X } from "lucide-react";
import { toast } from "sonner";
import { getErrorMessage } from "@/api/errors";
import { formatMoney } from "@/lib/format";
import { useCart, useCartActions } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Button, ButtonLink } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/Feedback";
import { Input } from "@/components/ui/Field";
import { ProductImage } from "@/components/ui/ProductImage";
import { QuantityStepper } from "@/components/ui/QuantityStepper";
import { ConfirmModal } from "@/components/ui/Modal";
import type { Cart } from "@/api/types";

export function OrderSummary({ cart, children }: { cart: Cart; children?: ReactNode }) {
  const itemCount = cart.products.reduce((s, p) => s + p.quantity, 0);
  const mrp = cart.products.reduce((s, p) => s + p.priceMinorUnits * p.quantity, 0);
  const productSavings = Math.max(0, mrp - cart.totalPriceMinorUnits);
  return (
    <Card className="p-5">
      <h2 className="text-base font-semibold text-slate-900">Order summary</h2>
      <dl className="mt-4 space-y-2.5 text-sm">
        <div className="flex justify-between text-slate-600">
          <dt>Price ({itemCount} item{itemCount === 1 ? "" : "s"})</dt>
          <dd>{formatMoney(mrp, cart.currency)}</dd>
        </div>
        {productSavings > 0 && (
          <div className="flex justify-between text-emerald-700">
            <dt>Product discounts</dt>
            <dd>−{formatMoney(productSavings, cart.currency)}</dd>
          </div>
        )}
        {cart.appliedCouponCode && cart.discountMinorUnits > 0 && (
          <div className="flex justify-between text-emerald-700">
            <dt>Coupon ({cart.appliedCouponCode})</dt>
            <dd>−{formatMoney(cart.discountMinorUnits, cart.currency)}</dd>
          </div>
        )}
        <div className="flex justify-between text-slate-600">
          <dt>Delivery</dt>
          {cart.shippingMinorUnits > 0 ? (
            <dd>{formatMoney(cart.shippingMinorUnits, cart.currency)}</dd>
          ) : (
            <dd className="font-medium text-emerald-700">Free</dd>
          )}
        </div>
        <div className="flex justify-between border-t border-dashed border-slate-200 pt-3 text-base font-bold text-slate-900">
          <dt>Total</dt>
          <dd>{formatMoney(cart.finalPriceMinorUnits, cart.currency)}</dd>
        </div>
      </dl>
      {productSavings + cart.discountMinorUnits > 0 && (
        <p className="mt-3 rounded-lg bg-emerald-50 px-3 py-2 text-center text-xs font-semibold text-emerald-700">
          You save {formatMoney(productSavings + cart.discountMinorUnits, cart.currency)} on this order
        </p>
      )}
      {children}
    </Card>
  );
}

function CouponBox({ cart }: { cart: Cart }) {
  const [code, setCode] = useState("");
  const { applyCoupon, removeCoupon } = useCartActions();

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!code.trim()) return;
    applyCoupon.mutate(code, {
      onSuccess: (res) => {
        toast.success(`Coupon ${res.couponCode} applied`, { description: `You saved ${formatMoney(res.discountMinorUnits, res.currency)}` });
        setCode("");
      },
      onError: (err) => toast.error(getErrorMessage(err)),
    });
  };

  return (
    <Card className="p-5">
      <p className="flex items-center gap-2 text-sm font-semibold text-slate-900">
        <Tag className="h-4 w-4 text-brand-600" /> Apply a coupon
      </p>
      {cart.appliedCouponCode ? (
        <div className="mt-3 flex items-center justify-between rounded-lg border border-dashed border-emerald-300 bg-emerald-50 px-3 py-2.5">
          <div>
            <p className="text-sm font-bold tracking-wide text-emerald-800">{cart.appliedCouponCode}</p>
            <p className="text-xs text-emerald-700">−{formatMoney(cart.discountMinorUnits, cart.currency)} applied</p>
          </div>
          <Button variant="ghost" size="icon-sm" onClick={() => removeCoupon.mutate()} loading={removeCoupon.isPending} aria-label="Remove coupon">
            <X className="h-4 w-4" />
          </Button>
        </div>
      ) : (
        <form onSubmit={submit} className="mt-3 flex gap-2">
          <Input value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="Enter code" className="uppercase tracking-wide" aria-label="Coupon code" />
          <Button type="submit" variant="secondary" loading={applyCoupon.isPending}>
            Apply
          </Button>
        </form>
      )}
    </Card>
  );
}

export default function CartPage() {
  useDocumentTitle("Your cart");
  const navigate = useNavigate();
  const { data: cart, isLoading, error, refetch } = useCart();
  const { increment, decrement, remove, clear } = useCartActions();
  const [confirmClear, setConfirmClear] = useState(false);
  const busy = increment.isPending || decrement.isPending || remove.isPending;

  if (isLoading) {
    return (
      <div className="mx-auto grid max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:grid-cols-[1fr_360px]">
        <Skeleton className="h-96 rounded-xl" />
        <Skeleton className="h-72 rounded-xl" />
      </div>
    );
  }
  if (error) return <ErrorState className="py-24" error={error} onRetry={() => refetch()} />;

  if (!cart || cart.products.length === 0) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <Card>
          <EmptyState
            icon={<ShoppingCart />}
            title="Your cart is empty"
            description="Looks like you haven't added anything yet. Explore today's deals and find something you love."
            action={<ButtonLink to="/products">Start shopping</ButtonLink>}
          />
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Shopping cart</h1>
          <p className="text-sm text-slate-500">{cart.products.length} product{cart.products.length === 1 ? "" : "s"}</p>
        </div>
        <Button variant="ghost" size="sm" onClick={() => setConfirmClear(true)} className="text-slate-500 hover:text-rose-600">
          <Trash className="h-4 w-4" /> Clear cart
        </Button>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <Card className="h-fit divide-y divide-slate-100">
          {cart.products.map((p) => (
            <div key={p.productId} className="flex gap-4 p-4 sm:p-5">
              <Link to={`/products/${p.productId}`} className="shrink-0">
                <ProductImage src={p.image} alt={p.productName} className="h-24 w-24 rounded-lg border border-slate-100 p-2 sm:h-28 sm:w-28" />
              </Link>
              <div className="flex min-w-0 flex-1 flex-col">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <Link to={`/products/${p.productId}`} className="line-clamp-2 text-sm font-medium text-slate-900 hover:text-brand-700">
                      {p.productName}
                    </Link>
                    {p.sellerName && <p className="mt-0.5 text-xs text-slate-500">Sold by {p.sellerName}</p>}
                  </div>
                  <p className="shrink-0 text-base font-bold text-slate-900">{formatMoney(p.specialPriceMinorUnits * p.quantity, p.currency)}</p>
                </div>
                <div className="mt-1 flex items-baseline gap-2 text-xs">
                  <span className="font-medium text-slate-700">{formatMoney(p.specialPriceMinorUnits, p.currency)} each</span>
                  {p.discount > 0 && (
                    <>
                      <span className="text-slate-400 line-through">{formatMoney(p.priceMinorUnits, p.currency)}</span>
                      <span className="font-semibold text-emerald-600">{Math.round(p.discount)}% off</span>
                    </>
                  )}
                </div>
                <div className="mt-auto flex items-center justify-between pt-3">
                  <QuantityStepper
                    size="sm"
                    value={p.quantity}
                    disabled={busy}
                    onIncrement={() => increment.mutate(p.productId)}
                    onDecrement={() => (p.quantity <= 1 ? remove.mutate({ cartId: cart.cartId, productId: p.productId }) : decrement.mutate(p.productId))}
                  />
                  <button
                    onClick={() => remove.mutate({ cartId: cart.cartId, productId: p.productId })}
                    disabled={busy}
                    className="text-xs font-semibold text-slate-500 hover:text-rose-600 disabled:opacity-50"
                  >
                    Remove
                  </button>
                </div>
              </div>
            </div>
          ))}
        </Card>

        <div className="space-y-4 lg:sticky lg:top-40 lg:self-start">
          <CouponBox cart={cart} />
          <OrderSummary cart={cart}>
            <Button size="lg" className="mt-5 w-full" onClick={() => navigate("/checkout")}>
              Proceed to checkout <ArrowRight className="h-4 w-4" />
            </Button>
            <p className="mt-3 flex items-center justify-center gap-1.5 text-xs text-slate-500">
              <Lock className="h-3.5 w-3.5" /> Secure checkout with Stripe or Razorpay
            </p>
          </OrderSummary>
        </div>
      </div>

      <ConfirmModal
        open={confirmClear}
        onClose={() => setConfirmClear(false)}
        onConfirm={() => clear.mutate(undefined, { onSuccess: () => setConfirmClear(false) })}
        loading={clear.isPending}
        title="Clear your cart?"
        description="All products and any applied coupon will be removed."
        confirmLabel="Clear cart"
      />
    </div>
  );
}
