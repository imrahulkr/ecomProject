import { useEffect, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CircleCheck, Clock, CreditCard, MapPin, Package, Star, Truck, Undo2 } from "lucide-react";
import { toast } from "sonner";
import { orderApi } from "@/api/endpoints";
import { getErrorMessage, getStatus } from "@/api/errors";
import type { Order, OrderItem } from "@/api/types";
import { cn } from "@/lib/cn";
import { formatDate, formatDateTime, formatMoney } from "@/lib/format";
import { qk, useAddress, useOrder } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Card, CardHeader } from "@/components/ui/Card";
import { FulfillmentBadge, OrderStatusBadge } from "@/components/ui/Badge";
import { Alert, EmptyState, ErrorState, PageLoader, Spinner } from "@/components/ui/Feedback";
import { Button, ButtonLink } from "@/components/ui/Button";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { FormField, Textarea } from "@/components/ui/Field";
import { RefundNote } from "@/components/dashboard/FulfillmentActions";
import { ProductImage } from "@/components/ui/ProductImage";
import { AddressText } from "@/components/checkout/AddressForm";

// Mirrors the backend's app.returns.window-days default; the backend enforces the real value.
const RETURN_WINDOW_DAYS = 7;

function canRequestReturn(item: OrderItem) {
  if (item.fulfillmentStatus !== "DELIVERED" || !item.deliveredAt) return false;
  const delivered = new Date(item.deliveredAt).getTime();
  return Date.now() - delivered < RETURN_WINDOW_DAYS * 24 * 60 * 60 * 1000;
}

function Tracker({ order }: { order: Order }) {
  const items = order.orderItems ?? [];
  const active = items.filter((i) => i.fulfillmentStatus !== "CANCELLED");
  const paid = order.orderStatus === "PAID";
  const shipped = paid && active.length > 0 && active.every((i) => i.fulfillmentStatus === "SHIPPED" || i.fulfillmentStatus === "DELIVERED");
  const delivered = paid && active.length > 0 && active.every((i) => i.fulfillmentStatus === "DELIVERED");
  const steps = [
    { label: "Order placed", done: true, icon: Package },
    { label: "Payment confirmed", done: paid, icon: CreditCard },
    { label: "Shipped", done: shipped, icon: Truck },
    { label: "Delivered", done: delivered, icon: CircleCheck },
  ];
  if (order.orderStatus === "CANCELLED" || order.orderStatus === "PAYMENT_FAILED") return null;
  return (
    <ol className="grid grid-cols-4 px-2 py-5 sm:px-5">
      {steps.map((s, i) => (
        <li key={s.label} className="relative flex flex-col items-center gap-1.5 text-center">
          {i > 0 && (
            <span className={cn("absolute right-1/2 top-4.5 h-0.5 w-full -translate-y-1/2", s.done ? "bg-emerald-600" : "bg-slate-200")} />
          )}
          <span
            className={cn(
              "relative z-10 flex h-9 w-9 items-center justify-center rounded-full border-2",
              s.done ? "border-emerald-600 bg-emerald-600 text-white" : "border-slate-200 bg-white text-slate-400",
            )}
          >
            <s.icon className="h-4 w-4" />
          </span>
          <span className={cn("text-xs font-medium", s.done ? "text-slate-900" : "text-slate-400")}>{s.label}</span>
        </li>
      ))}
    </ol>
  );
}

export default function OrderDetailPage() {
  const { orderId } = useParams();
  const id = Number(orderId);
  useDocumentTitle(`Order #${id}`);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [params, setParams] = useSearchParams();
  const processing = params.get("payment") === "processing";
  const [timedOut, setTimedOut] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [returnItem, setReturnItem] = useState<OrderItem | null>(null);
  const [returnReason, setReturnReason] = useState("");
  const { data: order, isLoading, error, refetch } = useOrder(id, { poll: processing && !timedOut });
  const { data: address, isError: addressMissing } = useAddress(order?.addressId);

  // Payment success arrives via the provider webhook (or sync-payment asking the provider), so poll for a while after returning from payment.
  useEffect(() => {
    if (!processing) return;
    const t = setTimeout(() => setTimedOut(true), 120_000);
    return () => clearTimeout(t);
  }, [processing]);

  const paidNow = processing && order?.orderStatus === "PAID";
  useEffect(() => {
    if (paidNow) qc.invalidateQueries({ queryKey: qk.cart });
  }, [paidNow, qc]);

  const cancel = useMutation({
    mutationFn: () => orderApi.cancel(id),
    onSuccess: () => {
      toast.success("Order cancelled");
      setConfirmCancel(false);
      qc.invalidateQueries({ queryKey: qk.order(id) });
      qc.invalidateQueries({ queryKey: ["orders"] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const requestReturn = useMutation({
    mutationFn: () => orderApi.requestReturn(id, returnItem!.orderItemId, returnReason.trim()),
    onSuccess: () => {
      toast.success("Return requested", { description: "We'll email you once the seller reviews it." });
      setReturnItem(null);
      setReturnReason("");
      qc.invalidateQueries({ queryKey: qk.order(id) });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return <PageLoader />;
  if (error || !order) {
    return getStatus(error) === 404 ? (
      <Card>
        <EmptyState icon={<Package />} title="Order not found" description="We couldn't find this order on your account." action={<ButtonLink to="/orders">Back to orders</ButtonLink>} />
      </Card>
    ) : (
      <ErrorState error={error} onRetry={() => refetch()} />
    );
  }

  const items = order.orderItems ?? [];
  const subtotal = items.reduce((s, i) => s + i.orderedProductPriceMinorUnits * i.quantity, 0);
  const refunded = items.reduce((s, i) => s + (i.refundStatus === "SUCCEEDED" ? i.refundedMinorUnits : 0), 0);

  return (
    <div className="space-y-5">
      <Link to="/orders" className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-slate-900">
        <ArrowLeft className="h-4 w-4" /> All orders
      </Link>

      {processing && order.orderStatus === "PENDING_PAYMENT" && !timedOut && (
        <Alert tone="info">
          <span className="flex items-center gap-2">
            <Spinner className="h-4 w-4" /> Confirming your payment with the bank — this usually takes a few seconds.
          </span>
        </Alert>
      )}
      {processing && order.orderStatus === "PENDING_PAYMENT" && timedOut && (
        <Alert tone="warning" title="Still waiting for confirmation">
          If money was deducted, your order will update automatically once the payment provider confirms it. You can safely leave this page.
        </Alert>
      )}
      {paidNow && (
        <Alert tone="success" title="Payment received — your order is confirmed!">
          We've emailed your receipt. You'll get updates as items ship.
          <button className="ml-2 font-semibold underline" onClick={() => setParams({})}>
            Dismiss
          </button>
        </Alert>
      )}

      <Card>
        <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-100 px-5 py-4">
          <div>
            <h1 className="text-xl font-bold text-slate-900">Order #{order.orderId}</h1>
            <p className="text-sm text-slate-500">Placed {formatDateTime(order.createdAt ?? order.orderDate)}</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <OrderStatusBadge status={order.orderStatus} />
            {order.orderStatus === "PENDING_PAYMENT" && (
              <>
                <Button size="sm" onClick={() => navigate(`/orders/${order.orderId}/pay`)}>
                  <CreditCard className="h-4 w-4" /> Pay now
                </Button>
                <Button size="sm" variant="outline" onClick={() => setConfirmCancel(true)}>
                  Cancel
                </Button>
              </>
            )}
          </div>
        </div>
        <Tracker order={order} />
        {order.orderStatus === "PENDING_PAYMENT" && !processing && (
          <div className="border-t border-slate-100 px-5 py-3 text-sm text-amber-800">
            <Clock className="mr-1.5 inline h-4 w-4" />
            Awaiting payment. Stock is held for 10 minutes after ordering; after that you'll need to cancel and order again.
          </div>
        )}
      </Card>

      <div className="grid gap-5 lg:grid-cols-[1fr_300px]">
        <Card>
          <CardHeader title={`Items (${items.length})`} />
          <ul className="divide-y divide-slate-100">
            {items.map((it) => (
              <li key={it.orderItemId} className="flex gap-4 p-5">
                <ProductImage src={it.product?.image} alt={it.product?.productName ?? ""} className="h-20 w-20 shrink-0 rounded-lg border border-slate-100 p-1.5" />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div className="min-w-0">
                      {it.product ? (
                        <Link to={`/products/${it.product.productId}`} className="line-clamp-2 text-sm font-medium text-slate-900 hover:text-brand-700">
                          {it.product.productName}
                        </Link>
                      ) : (
                        <p className="text-sm font-medium text-slate-900">Product</p>
                      )}
                      <p className="mt-0.5 text-xs text-slate-500">
                        Qty {it.quantity} · {formatMoney(it.orderedProductPriceMinorUnits, it.currency)} each
                      </p>
                    </div>
                    {order.orderStatus === "PAID" && <FulfillmentBadge status={it.fulfillmentStatus} />}
                  </div>
                  {it.trackingNumber && (
                    <p className="mt-2 text-xs text-slate-600">
                      <Truck className="mr-1 inline h-3.5 w-3.5" />
                      {it.carrier} · <span className="font-mono">{it.trackingNumber}</span>
                      {it.shippedAt && <span className="text-slate-400"> · shipped {formatDate(it.shippedAt)}</span>}
                    </p>
                  )}
                  {it.deliveredAt && <p className="mt-1 text-xs text-emerald-700">Delivered {formatDate(it.deliveredAt)}</p>}
                  {it.fulfillmentStatus === "RETURN_REQUESTED" && (
                    <p className="mt-1 text-xs text-amber-700">Return requested - the seller will review it shortly.</p>
                  )}
                  {it.fulfillmentStatus === "RETURN_REJECTED" && (
                    <p className="mt-1 text-xs text-rose-700">The seller declined this return. Contact support if you need help.</p>
                  )}
                  <div className="mt-1">
                    <RefundNote item={it} />
                  </div>
                  {canRequestReturn(it) && (
                    <button
                      type="button"
                      onClick={() => setReturnItem(it)}
                      className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-slate-600 hover:text-brand-700 hover:underline"
                    >
                      <Undo2 className="h-3.5 w-3.5" /> Return this item
                    </button>
                  )}
                  {it.fulfillmentStatus === "DELIVERED" && it.product && (
                    <Link to={`/products/${it.product.productId}#reviews`} className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-brand-700 hover:underline">
                      <Star className="h-3.5 w-3.5" /> Rate this product
                    </Link>
                  )}
                </div>
                <p className="shrink-0 text-sm font-semibold text-slate-900">{formatMoney(it.orderedProductPriceMinorUnits * it.quantity, it.currency)}</p>
              </li>
            ))}
          </ul>
        </Card>

        <div className="space-y-5">
          <Card className="p-5">
            <p className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-900">
              <MapPin className="h-4 w-4 text-brand-600" /> Delivery address
            </p>
            {address ? (
              <AddressText address={address} />
            ) : addressMissing ? (
              <p className="text-sm text-slate-500">This address was removed from your account.</p>
            ) : (
              <p className="text-sm text-slate-400">Loading…</p>
            )}
          </Card>
          <Card className="p-5">
            <p className="mb-3 text-sm font-semibold text-slate-900">Payment summary</p>
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between text-slate-600">
                <dt>Items</dt>
                <dd>{formatMoney(subtotal, order.currency)}</dd>
              </div>
              {order.discountMinorUnits > 0 && (
                <div className="flex justify-between text-emerald-700">
                  <dt>Coupon {order.couponCode}</dt>
                  <dd>−{formatMoney(order.discountMinorUnits, order.currency)}</dd>
                </div>
              )}
              <div className="flex justify-between text-slate-600">
                <dt>Delivery</dt>
                <dd>{order.shippingMinorUnits > 0 ? formatMoney(order.shippingMinorUnits, order.currency) : "Free"}</dd>
              </div>
              {refunded > 0 && (
                <div className="flex justify-between text-emerald-700">
                  <dt>Refunded</dt>
                  <dd>−{formatMoney(refunded, order.currency)}</dd>
                </div>
              )}
              <div className="flex justify-between border-t border-dashed border-slate-200 pt-2.5 text-base font-bold text-slate-900">
                <dt>Total</dt>
                <dd>{formatMoney(order.amountMinorUnits, order.currency)}</dd>
              </div>
            </dl>
          </Card>
        </div>
      </div>

      <ConfirmModal
        open={confirmCancel}
        onClose={() => setConfirmCancel(false)}
        onConfirm={() => cancel.mutate()}
        loading={cancel.isPending}
        title="Cancel this order?"
        description="Reserved items go back to stock. This can't be undone."
        confirmLabel="Cancel order"
      />
      <Modal
        open={!!returnItem}
        onClose={() => setReturnItem(null)}
        title="Return this item"
        description={`${returnItem?.product?.productName ?? "Item"} - once the seller approves, you're refunded to your original payment method.`}
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={() => setReturnItem(null)}>
              Keep item
            </Button>
            <Button loading={requestReturn.isPending} disabled={!returnReason.trim()} onClick={() => requestReturn.mutate()}>
              Request return
            </Button>
          </>
        }
      >
        <FormField label="Why are you returning it?">
          {(fid) => (
            <Textarea id={fid} rows={3} maxLength={1000} value={returnReason} onChange={(e) => setReturnReason(e.target.value)} placeholder="e.g. Arrived damaged, wrong size…" />
          )}
        </FormField>
      </Modal>
    </div>
  );
}
