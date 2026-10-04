import { useState } from "react";
import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { EyeOff, MessageSquare, Store } from "lucide-react";
import { reviewApi } from "@/api/endpoints";
import { formatDate } from "@/lib/format";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Card, PageHeader } from "@/components/ui/Card";
import { Stars } from "@/components/ui/Stars";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/Feedback";
import { Pagination } from "@/components/ui/Pagination";
import { Badge } from "@/components/ui/Badge";

export default function MyReviewsPage() {
  useDocumentTitle("My reviews");
  const [page, setPage] = useState(0);
  const q = { pageNumber: page, pageSize: 10, sortBy: "reviewId", sortOrder: "desc" as const };
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ["my-reviews", q], queryFn: () => reviewApi.mine(q) });

  return (
    <div>
      <PageHeader title="My reviews" description="Everything you've rated. Edit or delete a review from its product page." />
      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-28 rounded-xl" />
        </div>
      ) : error ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : !data?.content.length ? (
        <Card>
          <EmptyState icon={<MessageSquare />} title="No reviews yet" description="Rate products you've bought to help other shoppers." />
        </Card>
      ) : (
        <div className="space-y-3">
          {data.content.map((r) => (
            <Card key={r.reviewId} className="p-5">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <Link to={`/products/${r.productId}#reviews`} className="text-sm font-semibold text-brand-700 hover:underline">
                    View product #{r.productId}
                  </Link>
                  <div className="mt-1 flex items-center gap-2">
                    <Stars value={r.rating} size="xs" />
                    <span className="text-xs text-slate-400">{formatDate(r.createdAt)}</span>
                  </div>
                </div>
                {r.hidden && (
                  <Badge tone="warning">
                    <EyeOff className="h-3 w-3" /> Hidden by moderator
                  </Badge>
                )}
              </div>
              {r.comment && <p className="mt-3 text-sm leading-relaxed text-slate-700">{r.comment}</p>}
              {r.hidden && r.hiddenReason && <p className="mt-2 text-xs text-amber-700">Reason: {r.hiddenReason}</p>}
              {r.sellerReply && (
                <div className="mt-3 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-600">
                  <p className="mb-1 flex items-center gap-1.5 text-xs font-semibold text-slate-700">
                    <Store className="h-3.5 w-3.5" /> Seller replied
                  </p>
                  {r.sellerReply}
                </div>
              )}
            </Card>
          ))}
          <Pagination className="pt-3" pageNumber={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
        </div>
      )}
    </div>
  );
}
