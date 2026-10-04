import { useState } from "react";
import { Link } from "react-router";
import { ChevronRight, Package } from "lucide-react";
import { formatDate, formatMoney } from "@/lib/format";
import { useMyOrders } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Card, PageHeader } from "@/components/ui/Card";
import { OrderStatusBadge } from "@/components/ui/Badge";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/Feedback";
import { Pagination } from "@/components/ui/Pagination";
import { ProductImage } from "@/components/ui/ProductImage";
import { ButtonLink } from "@/components/ui/Button";

export default function OrdersPage() {
  useDocumentTitle("Your orders");
  const [page, setPage] = useState(0);
  const { data, isLoading, error, refetch } = useMyOrders({ pageNumber: page, pageSize: 10, sortBy: "orderId", sortOrder: "desc" });

  return (
    <div>
      <PageHeader title="Your orders" description="Track, pay for, or review your purchases." />
      {isLoading ? (
        <div className="space-y-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-32 rounded-xl" />
          ))}
        </div>
      ) : error ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : !data?.content.length ? (
        <Card>
          <EmptyState
            icon={<Package />}
            title="No orders yet"
            description="When you place an order, it'll show up here."
            action={<ButtonLink to="/products">Start shopping</ButtonLink>}
          />
        </Card>
      ) : (
        <div className="space-y-3">
          {data.content.map((o) => {
            const items = o.orderItems ?? [];
            return (
              <Link key={o.orderId} to={`/orders/${o.orderId}`} className="block">
                <Card className="transition-shadow hover:shadow-pop">
                  <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-3 text-sm">
                    <div className="flex flex-wrap gap-x-8 gap-y-1">
                      <div>
                        <p className="text-xs text-slate-500">Order</p>
                        <p className="font-semibold text-slate-900">#{o.orderId}</p>
                      </div>
                      <div>
                        <p className="text-xs text-slate-500">Placed</p>
                        <p className="font-medium text-slate-800">{formatDate(o.createdAt ?? o.orderDate)}</p>
                      </div>
                      <div>
                        <p className="text-xs text-slate-500">Total</p>
                        <p className="font-semibold text-slate-900">{formatMoney(o.amountMinorUnits, o.currency)}</p>
                      </div>
                    </div>
                    <OrderStatusBadge status={o.orderStatus} />
                  </div>
                  <div className="flex items-center gap-4 px-5 py-4">
                    <div className="flex -space-x-3">
                      {items.slice(0, 4).map((it) => (
                        <ProductImage
                          key={it.orderItemId}
                          src={it.product?.image}
                          alt={it.product?.productName ?? ""}
                          className="h-14 w-14 rounded-lg border-2 border-white bg-white p-1 shadow-card"
                        />
                      ))}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="line-clamp-1 text-sm font-medium text-slate-800">
                        {items.map((i) => i.product?.productName).filter(Boolean).join(", ") || "Order items"}
                      </p>
                      <p className="text-xs text-slate-500">
                        {items.reduce((s, i) => s + i.quantity, 0)} item(s)
                        {o.orderStatus === "PENDING_PAYMENT" && " · Payment pending"}
                      </p>
                    </div>
                    <ChevronRight className="h-5 w-5 text-slate-400" />
                  </div>
                </Card>
              </Link>
            );
          })}
          <Pagination className="pt-4" pageNumber={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
        </div>
      )}
    </div>
  );
}
