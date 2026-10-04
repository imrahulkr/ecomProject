import { useState } from "react";
import { Link } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Boxes, ClipboardList, MessageSquare, MessageSquareReply, Package, Plus, TriangleAlert, Wallet } from "lucide-react";
import { toast } from "sonner";
import { orderApi, productAdminApi, reviewApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { Review } from "@/api/types";
import { formatDate, formatMoney } from "@/lib/format";
import { useAuthStore } from "@/store/auth";
import { useReviews } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Card, CardHeader, PageHeader } from "@/components/ui/Card";
import { Button, ButtonLink } from "@/components/ui/Button";
import { OrderStatusBadge } from "@/components/ui/Badge";
import { EmptyState, Skeleton } from "@/components/ui/Feedback";
import { Select, Textarea } from "@/components/ui/Field";
import { Stars } from "@/components/ui/Stars";
import { Pagination } from "@/components/ui/Pagination";
import { ProductImage } from "@/components/ui/ProductImage";
import { StatCard } from "@/components/dashboard/StatCard";
import { ProductsTable } from "@/components/dashboard/ProductsTable";
import { ProductForm } from "@/components/dashboard/ProductForm";
import { OrdersBoard } from "@/components/dashboard/OrdersBoard";

export function SellerDashboardPage() {
  useDocumentTitle("Seller dashboard");
  const user = useAuthStore((s) => s.user);
  // No seller analytics endpoint exists, so the overview is derived from the seller's own lists.
  const products = useQuery({
    queryKey: ["manage-products", "seller", "overview"],
    queryFn: () => productAdminApi.list("seller", { pageSize: 200, sortBy: "quantity", sortOrder: "asc" }),
  });
  const orders = useQuery({
    queryKey: ["manage-orders", "seller", "overview"],
    queryFn: () => orderApi.sellerOrders({ pageSize: 50, sortBy: "orderId", sortOrder: "desc" }),
  });

  const paidOrders = orders.data?.content.filter((o) => o.orderStatus === "PAID") ?? [];
  const revenue = paidOrders.reduce(
    (s, o) => s + (o.orderItems ?? []).filter((i) => i.fulfillmentStatus !== "CANCELLED").reduce((t, i) => t + i.orderedProductPriceMinorUnits * i.quantity, 0),
    0,
  );
  const toShip = paidOrders.reduce((s, o) => s + (o.orderItems ?? []).filter((i) => i.fulfillmentStatus === "PENDING").length, 0);
  const lowStock = products.data?.content.filter((p) => p.quantity <= 5) ?? [];
  const loading = products.isLoading || orders.isLoading;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Seller dashboard"
        title={`Welcome back, ${user?.name?.split(" ")[0] || user?.username}`}
        description="Here's what's happening with your store."
        action={
          <ButtonLink to="/seller/products/new">
            <Plus className="h-4 w-4" /> Add product
          </ButtonLink>
        }
      />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Revenue (recent paid orders)" value={formatMoney(revenue)} icon={<Wallet />} tone="emerald" loading={loading} />
        <StatCard label="Items to ship" value={toShip} icon={<ClipboardList />} tone="amber" loading={loading} hint="Paid items awaiting shipment" />
        <StatCard label="Products listed" value={products.data?.totalElements ?? 0} icon={<Package />} loading={loading} />
        <StatCard label="Low stock" value={lowStock.length} icon={<TriangleAlert />} tone="rose" loading={loading} hint="5 units or fewer" />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader
            title="Recent orders"
            action={
              <Link to="/seller/orders" className="flex items-center gap-1 text-sm font-semibold text-brand-700 hover:underline">
                All orders <ArrowRight className="h-4 w-4" />
              </Link>
            }
          />
          {orders.isLoading ? (
            <div className="space-y-2 p-5">
              <Skeleton className="h-10" />
              <Skeleton className="h-10" />
            </div>
          ) : !orders.data?.content.length ? (
            <EmptyState icon={<ClipboardList />} title="No orders yet" />
          ) : (
            <ul className="divide-y divide-slate-100">
              {orders.data.content.slice(0, 6).map((o) => (
                <li key={o.orderId} className="flex items-center justify-between gap-3 px-5 py-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-900">#{o.orderId}</p>
                    <p className="text-xs text-slate-500">{formatDate(o.createdAt ?? o.orderDate)}</p>
                  </div>
                  <OrderStatusBadge status={o.orderStatus} />
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardHeader
            title="Needs restocking"
            action={
              <Link to="/seller/products" className="flex items-center gap-1 text-sm font-semibold text-brand-700 hover:underline">
                Products <ArrowRight className="h-4 w-4" />
              </Link>
            }
          />
          {products.isLoading ? (
            <div className="space-y-2 p-5">
              <Skeleton className="h-10" />
              <Skeleton className="h-10" />
            </div>
          ) : !lowStock.length ? (
            <EmptyState icon={<Boxes />} title="Stock looks healthy" description="No products are running low." />
          ) : (
            <ul className="divide-y divide-slate-100">
              {lowStock.slice(0, 6).map((p) => (
                <li key={p.productId} className="flex items-center gap-3 px-5 py-3">
                  <ProductImage src={p.image} alt={p.productName} className="h-10 w-10 rounded-lg border border-slate-100 p-1" />
                  <p className="line-clamp-1 flex-1 text-sm text-slate-800">{p.productName}</p>
                  <span className={p.quantity === 0 ? "text-sm font-semibold text-rose-600" : "text-sm font-semibold text-amber-700"}>
                    {p.quantity === 0 ? "Out" : `${p.quantity} left`}
                  </span>
                  <Link to={`/seller/products/${p.productId}/edit`} className="text-xs font-semibold text-brand-700 hover:underline">
                    Restock
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}

export function SellerProductsPage() {
  useDocumentTitle("Your products");
  return <ProductsTable scope="seller" />;
}

export function SellerProductFormPage() {
  useDocumentTitle("Product");
  return <ProductForm scope="seller" />;
}

export function SellerOrdersPage() {
  useDocumentTitle("Orders");
  return <OrdersBoard scope="seller" />;
}

function ReplyBox({ review, productId }: { review: Review; productId: number }) {
  const qc = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [text, setText] = useState(review.sellerReply ?? "");
  const invalidate = () => qc.invalidateQueries({ queryKey: ["reviews", productId] });

  const save = useMutation({
    mutationFn: () => reviewApi.reply(review.reviewId, text.trim()),
    onSuccess: () => {
      toast.success("Reply published");
      setEditing(false);
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const remove = useMutation({
    mutationFn: () => reviewApi.removeReply(review.reviewId),
    onSuccess: () => {
      toast.success("Reply removed");
      setText("");
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (!editing && review.sellerReply) {
    return (
      <div className="mt-3 rounded-lg border-l-2 border-brand-400 bg-slate-50 px-4 py-3">
        <p className="text-xs font-semibold text-slate-700">Your reply · {formatDate(review.sellerRepliedAt)}</p>
        <p className="mt-1 text-sm text-slate-600">{review.sellerReply}</p>
        <div className="mt-2 flex gap-2">
          <Button size="sm" variant="ghost" onClick={() => setEditing(true)}>
            Edit
          </Button>
          <Button size="sm" variant="ghost" className="hover:text-rose-600" loading={remove.isPending} onClick={() => remove.mutate()}>
            Delete
          </Button>
        </div>
      </div>
    );
  }
  if (!editing) {
    return (
      <Button size="sm" variant="outline" className="mt-3" onClick={() => setEditing(true)}>
        <MessageSquareReply className="h-3.5 w-3.5" /> Reply publicly
      </Button>
    );
  }
  return (
    <div className="mt-3 space-y-2">
      <Textarea rows={3} value={text} onChange={(e) => setText(e.target.value)} maxLength={2000} placeholder="Thank the customer or address their concern…" />
      <div className="flex gap-2">
        <Button size="sm" disabled={!text.trim()} loading={save.isPending} onClick={() => save.mutate()}>
          Publish reply
        </Button>
        <Button size="sm" variant="ghost" onClick={() => setEditing(false)}>
          Cancel
        </Button>
      </div>
    </div>
  );
}

export function SellerReviewsPage() {
  useDocumentTitle("Reviews");
  const [productId, setProductId] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const products = useQuery({
    queryKey: ["manage-products", "seller", "all-names"],
    queryFn: () => productAdminApi.list("seller", { pageSize: 200, sortBy: "productName", sortOrder: "asc" }),
  });
  const selected = productId ?? products.data?.content[0]?.productId ?? null;
  const reviews = useReviews(selected ?? 0, { pageNumber: page, pageSize: 10, sortBy: "createdAt", sortOrder: "desc" });

  return (
    <div>
      <PageHeader title="Customer reviews" description="Reply publicly to reviews on your products." />
      {products.isLoading ? (
        <Skeleton className="h-10 w-80" />
      ) : !products.data?.content.length ? (
        <Card>
          <EmptyState icon={<MessageSquare />} title="No products yet" description="Reviews appear once you list products and customers rate them." />
        </Card>
      ) : (
        <>
          <Select
            value={selected ?? ""}
            onChange={(e) => (setProductId(Number(e.target.value)), setPage(0))}
            className="mb-5 max-w-md"
            aria-label="Product"
          >
            {products.data.content.map((p) => (
              <option key={p.productId} value={p.productId}>
                {p.productName}
              </option>
            ))}
          </Select>
          <Card>
            {reviews.isLoading ? (
              <div className="space-y-3 p-5">
                <Skeleton className="h-20" />
                <Skeleton className="h-20" />
              </div>
            ) : !reviews.data?.content.length ? (
              <EmptyState icon={<MessageSquare />} title="No reviews for this product yet" />
            ) : (
              <ul className="divide-y divide-slate-100">
                {reviews.data.content.map((r) => (
                  <li key={r.reviewId} className="p-5">
                    <div className="flex items-center gap-3">
                      <Stars value={r.rating} size="xs" />
                      <span className="text-sm font-semibold text-slate-900">{r.userName || "Customer"}</span>
                      <span className="text-xs text-slate-400">{formatDate(r.createdAt)}</span>
                    </div>
                    {r.comment && <p className="mt-2 text-sm text-slate-700">{r.comment}</p>}
                    <ReplyBox review={r} productId={selected!} />
                  </li>
                ))}
              </ul>
            )}
          </Card>
          {reviews.data && <Pagination className="mt-5" pageNumber={reviews.data.pageNumber} totalPages={reviews.data.totalPages} onChange={setPage} />}
        </>
      )}
    </div>
  );
}
