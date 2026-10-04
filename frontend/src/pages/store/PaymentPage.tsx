import { useEffect, useRef, useState, type FormEvent } from "react";
import { Navigate, useNavigate, useParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Elements, PaymentElement, useElements, useStripe } from "@stripe/react-stripe-js";
import { Clock, CreditCard, Lock, RefreshCw, Smartphone } from "lucide-react";
import { toast } from "sonner";
import { checkoutApi, orderApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { CheckoutResponse, Order, PaymentProviderName } from "@/api/types";
import { env } from "@/config/env";
import { cn } from "@/lib/cn";
import { formatMoney } from "@/lib/format";
import { clearPaymentSession, getStripe, loadPaymentSession, openRazorpay, savePaymentSession } from "@/lib/payments";
import { qk, useOrder } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { useAuthStore } from "@/store/auth";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Alert, ErrorState, PageLoader } from "@/components/ui/Feedback";
import { ConfirmModal } from "@/components/ui/Modal";
import { ProductImage } from "@/components/ui/ProductImage";

function StripeForm({ order, amount, currency, onPaid }: { order: Order; amount: number; currency: string; onPaid: () => void }) {
  const stripe = useStripe();
  const elements = useElements();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!stripe || !elements) return;
    setSubmitting(true);
    setError(null);
    const { error: stripeError } = await stripe.confirmPayment({
      elements,
      confirmParams: { return_url: `${window.location.origin}/orders/${order.orderId}?payment=processing` },
      redirect: "if_required",
    });
    setSubmitting(false);
    if (stripeError) setError(stripeError.message ?? "Your payment could not be completed.");
    else onPaid();
  };

  return (
    <form onSubmit={submit} className="space-y-5">
      <PaymentElement options={{ layout: "tabs" }} />
      {error && <Alert tone="danger">{error}</Alert>}
      <Button type="submit" size="lg" className="w-full" loading={submitting} disabled={!stripe}>
        <Lock className="h-4 w-4" /> Pay {formatMoney(amount, currency)}
      </Button>
    </form>
  );
}

function StripePanel({ session, order, onPaid }: { session: CheckoutResponse; order: Order; onPaid: () => void }) {
  const stripePromise = getStripe();
  if (!stripePromise || !session.clientSecret) {
    return (
      <Alert tone="warning" title="Card payments aren't configured">
        Set <code className="font-mono">VITE_STRIPE_PUBLISHABLE_KEY</code> in the frontend environment, or choose another payment method below.
      </Alert>
    );
  }
  return (
    <Elements
      key={session.clientSecret}
      stripe={stripePromise}
      options={{
        clientSecret: session.clientSecret,
        appearance: {
          theme: "stripe",
          variables: { colorPrimary: "#2547e8", borderRadius: "8px", fontFamily: "Inter, system-ui, sans-serif" },
        },
      }}
    >
      <StripeForm order={order} amount={session.amountMinorUnits} currency={session.currency} onPaid={onPaid} />
    </Elements>
  );
}

function RazorpayPanel({ session, order, onPaid }: { session: CheckoutResponse; order: Order; onPaid: () => void }) {
  const user = useAuthStore((s) => s.user);
  const [opening, setOpening] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const autoOpened = useRef(false);

  const open = async () => {
    setOpening(true);
    setError(null);
    try {
      await openRazorpay({
        payload: session.clientPayload ?? {},
        orderId: order.orderId,
        email: user?.email,
        name: user?.name,
        onPaid,
        onDismiss: () => setOpening(false),
        onFailed: (reason) => {
          setOpening(false);
          setError(reason);
        },
      });
    } catch (err) {
      setOpening(false);
      setError(getErrorMessage(err));
    }
  };

  useEffect(() => {
    if (autoOpened.current) return;
    autoOpened.current = true;
    void open();
  }, []);

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3 rounded-xl bg-slate-50 p-4">
        <Smartphone className="h-8 w-8 text-brand-600" />
        <div>
          <p className="text-sm font-semibold text-slate-900">Pay with UPI, cards, wallets or netbanking</p>
          <p className="text-xs text-slate-500">A secure Razorpay window will open to complete your payment.</p>
        </div>
      </div>
      {error && <Alert tone="danger">{error}</Alert>}
      <Button size="lg" className="w-full" loading={opening} onClick={open}>
        <Lock className="h-4 w-4" /> Pay {formatMoney(session.amountMinorUnits, session.currency)}
      </Button>
    </div>
  );
}

function ProviderPicker({ onPick, loading }: { onPick: (p: PaymentProviderName) => void; loading: PaymentProviderName | null }) {
  const options: { id: PaymentProviderName; title: string; text: string; icon: typeof CreditCard }[] = [
    { id: "RAZORPAY", title: "UPI, cards & netbanking", text: "Razorpay", icon: Smartphone },
    { id: "STRIPE", title: "Credit / debit card", text: "Stripe", icon: CreditCard },
  ];
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {options.map(({ id, title, text, icon: Icon }) => (
        <button
          key={id}
          disabled={!!loading}
          onClick={() => onPick(id)}
          className={cn(
            "flex items-center gap-3 rounded-xl border border-slate-200 p-4 text-left transition-all hover:border-brand-400 hover:bg-brand-50/40 disabled:opacity-60",
            loading === id && "border-brand-500 ring-4 ring-brand-500/10",
          )}
        >
          <Icon className="h-6 w-6 text-brand-600" />
          <div>
            <p className="text-sm font-semibold text-slate-900">{title}</p>
            <p className="text-xs text-slate-500">{loading === id ? "Preparing secure payment…" : `Pay with ${text}`}</p>
          </div>
        </button>
      ))}
    </div>
  );
}

export default function PaymentPage() {
  const { orderId } = useParams();
  const id = Number(orderId);
  useDocumentTitle(`Pay for order #${id}`);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { data: order, isLoading, error, refetch } = useOrder(id);
  const [session, setSession] = useState<CheckoutResponse | null>(() => loadPaymentSession(id));
  const [switching, setSwitching] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);

  const retry = useMutation({
    mutationFn: (p: PaymentProviderName) => checkoutApi.retryPayment(crypto.randomUUID(), id, p),
    onSuccess: (res) => {
      savePaymentSession(res);
      setSession(res);
      setSwitching(false);
      if (res.paymentAttemptFailed) toast.error(res.failureReason ?? "Payment could not be started");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const cancel = useMutation({
    mutationFn: () => orderApi.cancel(id),
    onSuccess: () => {
      clearPaymentSession(id);
      toast.success("Order cancelled");
      qc.invalidateQueries({ queryKey: qk.order(id) });
      qc.invalidateQueries({ queryKey: ["orders"] });
      navigate(`/orders/${id}`, { replace: true });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return <PageLoader label="Preparing your payment…" />;
  if (error || !order) return <ErrorState className="py-24" error={error} onRetry={() => refetch()} />;
  if (order.orderStatus !== "PENDING_PAYMENT") return <Navigate to={`/orders/${id}`} replace />;

  const onPaid = () => {
    clearPaymentSession(id);
    qc.invalidateQueries({ queryKey: qk.order(id) });
    navigate(`/orders/${id}?payment=processing`, { replace: true });
  };

  const ready = session && !session.paymentAttemptFailed && !switching;
  const items = order.orderItems ?? [];

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
      <div className="mb-6">
        <p className="text-sm font-medium text-brand-600">Order #{order.orderId}</p>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Complete your payment</h1>
        <p className="mt-1 flex items-center gap-1.5 text-sm text-slate-500">
          <Clock className="h-4 w-4" /> Your items are reserved for 10 minutes from when you placed the order.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <Card className="p-6">
          {ready && session.provider === "STRIPE" && <StripePanel session={session} order={order} onPaid={onPaid} />}
          {ready && session.provider === "RAZORPAY" && <RazorpayPanel session={session} order={order} onPaid={onPaid} />}
          {!ready && (
            <div className="space-y-4">
              {session?.paymentAttemptFailed && !switching && (
                <Alert tone="danger" title="We couldn't start your payment">
                  {session.failureReason}
                </Alert>
              )}
              <p className="text-sm font-semibold text-slate-900">Choose how you'd like to pay</p>
              <ProviderPicker onPick={(p) => retry.mutate(p)} loading={retry.isPending ? (retry.variables ?? null) : null} />
            </div>
          )}

          <div className="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
            {ready ? (
              <Button variant="link" onClick={() => setSwitching(true)}>
                <RefreshCw className="h-3.5 w-3.5" /> Use a different payment method
              </Button>
            ) : (
              <span />
            )}
            <Button variant="ghost" size="sm" className="text-slate-500 hover:text-rose-600" onClick={() => setConfirmCancel(true)}>
              Cancel order
            </Button>
          </div>
        </Card>

        <Card className="h-fit p-5">
          <h2 className="text-sm font-semibold text-slate-900">Order summary</h2>
          <ul className="mt-3 space-y-3">
            {items.map((it) => (
              <li key={it.orderItemId} className="flex items-center gap-3">
                <ProductImage src={it.product?.image} alt={it.product?.productName ?? "Product"} className="h-12 w-12 shrink-0 rounded-lg border border-slate-100 p-1" />
                <div className="min-w-0 flex-1">
                  <p className="line-clamp-1 text-sm text-slate-800">{it.product?.productName ?? "Product"}</p>
                  <p className="text-xs text-slate-500">Qty {it.quantity}</p>
                </div>
                <p className="text-sm font-medium">{formatMoney(it.orderedProductPriceMinorUnits * it.quantity, it.currency)}</p>
              </li>
            ))}
          </ul>
          {order.discountMinorUnits > 0 && (
            <div className="mt-4 flex justify-between text-sm text-emerald-700">
              <span>Coupon {order.couponCode}</span>
              <span>−{formatMoney(order.discountMinorUnits, order.currency)}</span>
            </div>
          )}
          {order.shippingMinorUnits > 0 && (
            <div className="mt-2 flex justify-between text-sm text-slate-600">
              <span>Delivery</span>
              <span>{formatMoney(order.shippingMinorUnits, order.currency)}</span>
            </div>
          )}
          <div className="mt-4 flex justify-between border-t border-dashed border-slate-200 pt-3 text-base font-bold">
            <span>Total</span>
            <span>{formatMoney(order.amountMinorUnits, order.currency)}</span>
          </div>
          {!env.stripePublishableKey && (
            <p className="mt-4 text-xs text-slate-400">Card payments require a Stripe publishable key.</p>
          )}
        </Card>
      </div>

      <ConfirmModal
        open={confirmCancel}
        onClose={() => setConfirmCancel(false)}
        onConfirm={() => cancel.mutate()}
        loading={cancel.isPending}
        title="Cancel this order?"
        description="Reserved items will be released back to stock. This can't be undone."
        confirmLabel="Cancel order"
      />
    </div>
  );
}
