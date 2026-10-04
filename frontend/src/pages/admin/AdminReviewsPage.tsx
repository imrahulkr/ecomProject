import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { EyeOff, MessageSquare, RotateCcw, Search } from "lucide-react";
import { toast } from "sonner";
import { catalogApi, reviewApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { Product, Review } from "@/api/types";
import { cn } from "@/lib/cn";
import { formatDate } from "@/lib/format";
import { useReviews } from "@/hooks/queries";
import { useDebounced, useDocumentTitle } from "@/hooks/useUtils";
import { Card, CardBody, CardHeader, PageHeader } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { EmptyState, Skeleton } from "@/components/ui/Feedback";
import { FormField, Input, Textarea } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { Pagination } from "@/components/ui/Pagination";
import { ProductImage } from "@/components/ui/ProductImage";
import { Stars } from "@/components/ui/Stars";

export default function AdminReviewsPage() {
  useDocumentTitle("Admin · Reviews");
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const keyword = useDebounced(search.trim(), 300);
  const [product, setProduct] = useState<Product | null>(null);
  const [page, setPage] = useState(0);
  const [hiding, setHiding] = useState<Review | null>(null);
  const [reason, setReason] = useState("");
  const [restoreId, setRestoreId] = useState("");

  const products = useQuery({
    queryKey: ["admin-review-products", keyword],
    queryFn: () => catalogApi.products({ keyword: keyword || undefined, pageSize: 8, sortBy: "productName", sortOrder: "asc" }),
  });
  const reviews = useReviews(product?.productId ?? 0, { pageNumber: page, pageSize: 10, sortBy: "createdAt", sortOrder: "desc" });

  const moderate = useMutation({
    mutationFn: ({ id, hidden, why }: { id: number; hidden: boolean; why?: string }) => reviewApi.moderate(id, hidden, why),
    onSuccess: (r) => {
      toast.success(r.hidden ? "Review hidden from the store" : `Review #${r.reviewId} restored`);
      setHiding(null);
      setReason("");
      setRestoreId("");
      qc.invalidateQueries({ queryKey: ["reviews"] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div>
      <PageHeader title="Review moderation" description="Hide abusive or off-topic reviews. Hidden reviews don't count toward ratings." />
      <div className="grid gap-5 lg:grid-cols-[320px_1fr]">
        <div className="space-y-5">
          <Card>
            <CardHeader title="Find a product" />
            <CardBody className="space-y-3">
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search by name" className="pl-9" />
              </div>
              {products.isLoading ? (
                <Skeleton className="h-32" />
              ) : (
                <ul className="space-y-1">
                  {products.data?.content.map((p) => (
                    <li key={p.productId}>
                      <button
                        onClick={() => (setProduct(p), setPage(0))}
                        className={cn(
                          "flex w-full items-center gap-3 rounded-lg p-2 text-left transition-colors",
                          product?.productId === p.productId ? "bg-brand-50 ring-1 ring-brand-200" : "hover:bg-slate-50",
                        )}
                      >
                        <ProductImage src={p.image} alt={p.productName} className="h-9 w-9 shrink-0 rounded-md border border-slate-100 p-0.5" />
                        <span className="line-clamp-2 text-sm text-slate-800">{p.productName}</span>
                      </button>
                    </li>
                  ))}
                  {!products.data?.content.length && <p className="text-sm text-slate-500">No matching products.</p>}
                </ul>
              )}
            </CardBody>
          </Card>
          <Card>
            <CardHeader title="Restore a hidden review" description="Hidden reviews aren't listed publicly — restore by ID." />
            <CardBody>
              <form
                className="flex gap-2"
                onSubmit={(e) => {
                  e.preventDefault();
                  const id = Number(restoreId);
                  if (id > 0) moderate.mutate({ id, hidden: false });
                }}
              >
                <Input value={restoreId} onChange={(e) => setRestoreId(e.target.value.replace(/\D/g, ""))} placeholder="Review ID" inputMode="numeric" />
                <Button type="submit" variant="outline" disabled={!restoreId} loading={moderate.isPending && moderate.variables?.hidden === false}>
                  <RotateCcw className="h-4 w-4" /> Restore
                </Button>
              </form>
            </CardBody>
          </Card>
        </div>

        <Card>
          {!product ? (
            <EmptyState icon={<MessageSquare />} title="Pick a product" description="Its visible reviews will appear here." />
          ) : reviews.isLoading ? (
            <div className="space-y-3 p-5">
              <Skeleton className="h-20" />
              <Skeleton className="h-20" />
            </div>
          ) : !reviews.data?.content.length ? (
            <EmptyState icon={<MessageSquare />} title="No visible reviews" description={product.productName} />
          ) : (
            <>
              <CardHeader title={product.productName} description={`${reviews.data.totalElements} visible review(s)`} />
              <ul className="divide-y divide-slate-100">
                {reviews.data.content.map((r) => (
                  <li key={r.reviewId} className="flex items-start justify-between gap-4 p-5">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <Stars value={r.rating} size="xs" />
                        <span className="text-sm font-semibold text-slate-900">{r.userName || "Customer"}</span>
                        <span className="text-xs text-slate-400">
                          #{r.reviewId} · {formatDate(r.createdAt)}
                        </span>
                      </div>
                      {r.comment && <p className="mt-2 text-sm text-slate-700">{r.comment}</p>}
                    </div>
                    <Button size="sm" variant="outline" className="shrink-0 hover:border-rose-300 hover:text-rose-700" onClick={() => setHiding(r)}>
                      <EyeOff className="h-3.5 w-3.5" /> Hide
                    </Button>
                  </li>
                ))}
              </ul>
              <div className="p-4">
                <Pagination pageNumber={reviews.data.pageNumber} totalPages={reviews.data.totalPages} onChange={setPage} />
              </div>
            </>
          )}
        </Card>
      </div>

      <Modal
        open={!!hiding}
        onClose={() => setHiding(null)}
        title={`Hide review #${hiding?.reviewId}?`}
        description="The author still sees it, marked as hidden, along with your reason."
        footer={
          <>
            <Button variant="outline" onClick={() => setHiding(null)}>
              Cancel
            </Button>
            <Button variant="danger" loading={moderate.isPending} onClick={() => hiding && moderate.mutate({ id: hiding.reviewId, hidden: true, why: reason.trim() || undefined })}>
              Hide review
            </Button>
          </>
        }
      >
        <FormField label="Reason" hint="Optional, up to 500 characters">
          {(id) => <Textarea id={id} rows={3} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />}
        </FormField>
      </Modal>
    </div>
  );
}
