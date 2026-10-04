import { useState } from "react";
import { Link, useSearchParams } from "react-router";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronDown, ClipboardList } from "lucide-react";
import { toast } from "sonner";
import { orderApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { Order, OrderStatus } from "@/api/types";
import { cn } from "@/lib/cn";
import { formatDateTime, formatMoney } from "@/lib/format";
import { Card, PageHeader } from "@/components/ui/Card";
import { FulfillmentBadge, OrderStatusBadge } from "@/components/ui/Badge";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/Feedback";
import { Pagination } from "@/components/ui/Pagination";
import { Segmented } from "@/components/ui/Table";
import { ProductImage } from "@/components/ui/ProductImage";
import { Select } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { ConfirmModal } from "@/components/ui/Modal";
import { FulfillmentActions } from "./FulfillmentActions";

const STATUSES: OrderStatus[] = ["PENDING_PAYMENT", "PAID", "PAYMENT_FAILED", "CANCELLED"];

// PAYMENT_FAILED is left out: the backend never writes it (failed payments stay PENDING_PAYMENT).
type StatusFilter = "ALL" | "PAID" | "PENDING_PAYMENT" | "CANCELLED";
const STATUS_FILTERS: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "PAID", label: "Paid" },
  { value: "PENDING_PAYMENT", label: "Awaiting payment" },
  { value: "CANCELLED", label: "Cancelled" },
];

function AdminStatusOverride({ order, invalidateKey }: { order: Order; invalidateKey: readonly unknown[] }) {
  const qc = useQueryClient();
  const [target, setTarget] = useState<OrderStatus>(order.orderStatus);
  const [confirm, setConfirm] = useState(false);
  const save = useMutation({
    mutationFn: () => orderApi.adminSetStatus(order.orderId, target),
    onSuccess: () => {
      toast.success(`Order #${order.orderId} set to ${target}`);
      setConfirm(false);
      qc.invalidateQueries({ queryKey: invalidateKey });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  return (
    <div className="flex items-center gap-2">
      <Select value={target} onChange={(e) => setTarget(e.target.value as OrderStatus)} className="h-8 w-44 text-xs" aria-label="Order status">
        {STATUSES.map((s) => (
          <option key={s} value={s}>
            {s}
          </option>
        ))}
      </Select>
      <Button size="sm" variant="outline" disabled={target === order.orderStatus} onClick={() => setConfirm(true)}>
        Override
      </Button>
      <ConfirmModal
        open={confirm}
        onClose={() => setConfirm(false)}
        onConfirm={() => save.mutate()}
        loading={save.isPending}
        tone="primary"
        title="Override order status?"
        description="This bypasses the payment flow: no stock is released or confirmed and no emails are sent. Use only to correct records."
        confirmLabel="Override status"
      />
    </div>
  );
}

export function OrdersBoard({ scope }: { scope: "seller" | "admin" }) {
  const [page, setPage] = useState(0);
  const [expanded, setExpanded] = useState<number | null>(null);
  // Admin-only status filter, kept in the URL so dashboard cards can deep-link (?status=PAID).
  const [params, setParams] = useSearchParams();
  const rawStatus = params.get("status");
  const status: StatusFilter =
    scope === "admin" && STATUS_FILTERS.some((f) => f.value === rawStatus) ? (rawStatus as StatusFilter) : "ALL";
  const setStatus = (next: StatusFilter) => {
    setPage(0);
    setExpanded(null);
    setParams(next === "ALL" ? {} : { status: next }, { replace: true });
  };
  const q = { pageNumber: page, pageSize: 10, sortBy: "orderId", sortOrder: "desc" as const };
  const queryKey = ["manage-orders", scope, q, status] as const;
  const { data, isLoading, error, refetch } = useQuery({
    queryKey,
    queryFn: () =>
      scope === "seller" ? orderApi.sellerOrders(q) : orderApi.adminOrders(q, status === "ALL" ? undefined : status),
    placeholderData: keepPreviousData,
  });

  return (
    <div>
      <PageHeader
        title={scope === "seller" ? "Orders" : "All orders"}
        description={scope === "seller" ? "Orders containing your products. Ship paid items and add tracking." : "Every order on the marketplace."}
      />
      {scope === "admin" && (
        <div className="-mx-1 mb-4 overflow-x-auto px-1">
          <Segmented value={status} onChange={setStatus} options={STATUS_FILTERS} />
        </div>
      )}
      {isLoading ? (
        <div className="space-y-3">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-20 rounded-xl" />
          ))}
        </div>
      ) : error ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : !data?.content.length ? (
        <Card>
          {status === "ALL" ? (
            <EmptyState icon={<ClipboardList />} title="No orders yet" description="New orders will appear here as soon as customers check out." />
          ) : (
            <EmptyState
              icon={<ClipboardList />}
              title={`No ${STATUS_FILTERS.find((f) => f.value === status)?.label.toLowerCase()} orders`}
              description="Try another filter to see the rest."
            />
          )}
        </Card>
      ) : (
        <div className="space-y-3">
          {data.content.map((o) => {
            const open = expanded === o.orderId;
            const items = o.orderItems ?? [];
            const pendingItems = items.filter((i) => i.fulfillmentStatus === "PENDING").length;
            return (
              <Card key={o.orderId} className="overflow-hidden">
                <button
                  className="flex w-full flex-wrap items-center gap-x-6 gap-y-2 px-5 py-4 text-left hover:bg-slate-50/60"
                  onClick={() => setExpanded(open ? null : o.orderId)}
                  aria-expanded={open}
                >
                  <div className="min-w-24">
                    <p className="text-sm font-bold text-slate-900">#{o.orderId}</p>
                    <p className="text-xs text-slate-500">{formatDateTime(o.createdAt ?? o.orderDate)}</p>
                  </div>
                  <div className="min-w-40 flex-1">
                    <p className="truncate text-sm text-slate-700">{o.email}</p>
                    <p className="text-xs text-slate-500">
                      {items.length} line item{items.length === 1 ? "" : "s"}
                      {o.orderStatus === "PAID" && pendingItems > 0 && <span className="font-semibold text-amber-700"> · {pendingItems} to ship</span>}
                    </p>
                  </div>
                  <p className="text-sm font-semibold text-slate-900">{formatMoney(o.amountMinorUnits, o.currency)}</p>
                  <OrderStatusBadge status={o.orderStatus} />
                  <ChevronDown className={cn("h-4 w-4 text-slate-400 transition-transform", open && "rotate-180")} />
                </button>
                {open && (
                  <div className="border-t border-slate-100 bg-slate-50/40">
                    {scope === "admin" && (
                      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-3">
                        <p className="text-xs text-slate-500">
                          {o.couponCode ? `Coupon ${o.couponCode} (−${formatMoney(o.discountMinorUnits, o.currency)}) · ` : ""}Address #{o.addressId ?? "—"}
                        </p>
                        <AdminStatusOverride order={o} invalidateKey={["manage-orders", scope]} />
                      </div>
                    )}
                    <ul className="divide-y divide-slate-100">
                      {items.map((it) => (
                        <li key={it.orderItemId} className="flex flex-wrap items-center gap-4 px-5 py-3">
                          <ProductImage src={it.product?.image} alt={it.product?.productName ?? ""} className="h-12 w-12 shrink-0 rounded-lg border border-slate-100 p-1" />
                          <div className="min-w-48 flex-1">
                            {it.product ? (
                              <Link to={`/products/${it.product.productId}`} target="_blank" className="line-clamp-1 text-sm font-medium text-slate-900 hover:text-brand-700">
                                {it.product.productName}
                              </Link>
                            ) : (
                              <p className="text-sm font-medium">Product</p>
                            )}
                            <p className="text-xs text-slate-500">
                              Qty {it.quantity} · {formatMoney(it.orderedProductPriceMinorUnits * it.quantity, it.currency)}
                              {it.trackingNumber && (
                                <>
                                  {" "}· {it.carrier} <span className="font-mono">{it.trackingNumber}</span>
                                </>
                              )}
                            </p>
                          </div>
                          <FulfillmentBadge status={it.fulfillmentStatus} />
                          <FulfillmentActions item={it} orderStatus={o.orderStatus} scope={scope} invalidateKey={["manage-orders", scope]} />
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </Card>
            );
          })}
          <Pagination className="pt-4" pageNumber={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
        </div>
      )}
    </div>
  );
}
