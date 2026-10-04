import { useEffect, useRef, useState, type ReactNode } from "react";
import { Link, Navigate, useNavigate } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CircleCheck, CreditCard, Lock, MapPin, Plus, Smartphone, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { checkoutApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { PaymentProviderName } from "@/api/types";
import { cn } from "@/lib/cn";
import { formatMoney } from "@/lib/format";
import { savePaymentSession } from "@/lib/payments";
import { qk, useAddresses, useCart } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { PageLoader, Skeleton } from "@/components/ui/Feedback";
import { ProductImage } from "@/components/ui/ProductImage";
import { AddressFormModal, AddressText } from "@/components/checkout/AddressForm";
import { OrderSummary } from "./CartPage";

type ProviderChoice = "AUTO" | PaymentProviderName;

const providers: { id: ProviderChoice; title: string; text: string; icon: typeof CreditCard }[] = [
  { id: "AUTO", title: "Recommended", text: "We route you to the fastest available gateway", icon: Sparkles },
  { id: "RAZORPAY", title: "UPI, cards & netbanking", text: "Pay with Razorpay", icon: Smartphone },
  { id: "STRIPE", title: "Credit / debit card", text: "Pay with Stripe", icon: CreditCard },
];

function Step({ n, title, children, done }: { n: number; title: string; children: ReactNode; done?: boolean }) {
  return (
    <Card>
      <div className="flex items-center gap-3 border-b border-slate-100 px-5 py-4">
        <span
          className={cn(
            "flex h-7 w-7 items-center justify-center rounded-full text-sm font-bold",
            done ? "bg-emerald-600 text-white" : "bg-brand-600 text-white",
          )}
        >
          {done ? <CircleCheck className="h-4 w-4" /> : n}
        </span>
        <h2 className="text-base font-semibold text-slate-900">{title}</h2>
      </div>
      <div className="p-5">{children}</div>
    </Card>
  );
}

export default function CheckoutPage() {
  useDocumentTitle("Checkout");
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { data: cart, isLoading: cartLoading } = useCart();
  const { data: addresses, isLoading: addrLoading } = useAddresses();
  const [addressId, setAddressId] = useState<number | null>(null);
  const [provider, setProvider] = useState<ProviderChoice>("AUTO");
  const [addressModal, setAddressModal] = useState(false);
  // One Idempotency-Key per checkout attempt; reused only to safely re-send after a network failure.
  const attemptKey = useRef<string | null>(null);

  useEffect(() => {
    if (addressId == null && addresses?.length) setAddressId(addresses[addresses.length - 1].addressId);
  }, [addresses, addressId]);

  useEffect(() => {
    attemptKey.current = null;
  }, [addressId, provider]);

  const placeOrder = useMutation({
    mutationFn: () => {
      attemptKey.current ??= crypto.randomUUID();
      return checkoutApi.checkout(attemptKey.current, addressId!, provider === "AUTO" ? undefined : provider);
    },
    onSuccess: (res) => {
      attemptKey.current = null;
      savePaymentSession(res);
      qc.invalidateQueries({ queryKey: ["orders"] });
      qc.invalidateQueries({ queryKey: qk.cart });
      navigate(`/orders/${res.orderId}/pay`, { replace: true });
    },
    onError: (err) => {
      // Keep the key only when the request may not have reached the server.
      if (!isAxiosError(err) || err.response) attemptKey.current = null;
      toast.error("Couldn't place your order", { description: getErrorMessage(err) });
      qc.invalidateQueries({ queryKey: qk.cart });
    },
  });

  if (cartLoading) return <PageLoader />;
  if (!cart || cart.products.length === 0) return <Navigate to="/cart" replace />;

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Checkout</h1>
        <span className="flex items-center gap-1.5 text-sm text-slate-500">
          <Lock className="h-4 w-4" /> Secure checkout
        </span>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
        <div className="space-y-5">
          <Step n={1} title="Delivery address" done={!!addressId}>
            {addrLoading ? (
              <Skeleton className="h-24 w-full" />
            ) : (
              <div className="grid gap-3 sm:grid-cols-2">
                {addresses?.map((a) => (
                  <label
                    key={a.addressId}
                    className={cn(
                      "flex cursor-pointer gap-3 rounded-xl border p-4 transition-all",
                      addressId === a.addressId
                        ? "border-brand-500 bg-brand-50/50 ring-4 ring-brand-500/10"
                        : "border-slate-200 hover:border-slate-300",
                    )}
                  >
                    <input
                      type="radio"
                      name="address"
                      className="mt-1 accent-brand-600"
                      checked={addressId === a.addressId}
                      onChange={() => setAddressId(a.addressId)}
                    />
                    <AddressText address={a} />
                  </label>
                ))}
                <button
                  type="button"
                  onClick={() => setAddressModal(true)}
                  className="flex min-h-24 items-center justify-center gap-2 rounded-xl border-2 border-dashed border-slate-300 p-4 text-sm font-semibold text-slate-600 transition-colors hover:border-brand-400 hover:text-brand-700"
                >
                  <Plus className="h-4 w-4" /> Add new address
                </button>
              </div>
            )}
            {!addrLoading && !addresses?.length && (
              <p className="mt-3 flex items-center gap-2 text-sm text-slate-500">
                <MapPin className="h-4 w-4" /> Add a delivery address to continue.
              </p>
            )}
          </Step>

          <Step n={2} title="Payment method" done>
            <div className="grid gap-3 sm:grid-cols-3">
              {providers.map(({ id, title, text, icon: Icon }) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => setProvider(id)}
                  className={cn(
                    "rounded-xl border p-4 text-left transition-all",
                    provider === id ? "border-brand-500 bg-brand-50/50 ring-4 ring-brand-500/10" : "border-slate-200 hover:border-slate-300",
                  )}
                >
                  <Icon className={cn("h-5 w-5", provider === id ? "text-brand-600" : "text-slate-500")} />
                  <p className="mt-2 text-sm font-semibold text-slate-900">{title}</p>
                  <p className="text-xs text-slate-500">{text}</p>
                </button>
              ))}
            </div>
            <p className="mt-3 text-xs text-slate-500">
              Items are reserved for 10 minutes once you place the order, so complete payment promptly.
            </p>
          </Step>

          <Step n={3} title={`Review items (${cart.products.length})`}>
            <ul className="divide-y divide-slate-100">
              {cart.products.map((p) => (
                <li key={p.productId} className="flex items-center gap-4 py-3 first:pt-0 last:pb-0">
                  <ProductImage src={p.image} alt={p.productName} className="h-14 w-14 shrink-0 rounded-lg border border-slate-100 p-1" />
                  <div className="min-w-0 flex-1">
                    <Link to={`/products/${p.productId}`} className="line-clamp-1 text-sm font-medium text-slate-800 hover:text-brand-700">
                      {p.productName}
                    </Link>
                    <p className="text-xs text-slate-500">Qty {p.quantity}</p>
                  </div>
                  <p className="text-sm font-semibold text-slate-900">{formatMoney(p.specialPriceMinorUnits * p.quantity, p.currency)}</p>
                </li>
              ))}
            </ul>
          </Step>
        </div>

        <div className="lg:sticky lg:top-40 lg:self-start">
          <OrderSummary cart={cart}>
            <Button size="lg" className="mt-5 w-full" disabled={!addressId} loading={placeOrder.isPending} onClick={() => placeOrder.mutate()}>
              <Lock className="h-4 w-4" /> Place order · {formatMoney(cart.finalPriceMinorUnits, cart.currency)}
            </Button>
            {!addressId && <p className="mt-2 text-center text-xs text-amber-700">Choose a delivery address first</p>}
            <p className="mt-3 text-center text-xs text-slate-500">
              By placing your order you agree to our terms of sale.
            </p>
          </OrderSummary>
        </div>
      </div>

      <AddressFormModal open={addressModal} onClose={() => setAddressModal(false)} onSaved={(a) => setAddressId(a.addressId)} />
    </div>
  );
}
