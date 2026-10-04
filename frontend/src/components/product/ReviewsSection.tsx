import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { MessageSquare, Pencil, Store, Trash } from "lucide-react";
import { toast } from "sonner";
import { reviewApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { Review } from "@/api/types";
import { formatDate } from "@/lib/format";
import { useAuthStore } from "@/store/auth";
import { useReviewSummary, useReviews } from "@/hooks/queries";
import { useLoginRedirect } from "@/hooks/useAddToCart";
import { Stars, StarInput } from "@/components/ui/Stars";
import { Button } from "@/components/ui/Button";
import { Textarea } from "@/components/ui/Field";
import { Pagination } from "@/components/ui/Pagination";
import { EmptyState, Skeleton } from "@/components/ui/Feedback";
import { ConfirmModal } from "@/components/ui/Modal";
import { Segmented } from "@/components/ui/Table";

function ReviewForm({
  productId,
  existing,
  onDone,
}: {
  productId: number;
  existing?: Review;
  onDone: () => void;
}) {
  const qc = useQueryClient();
  const [rating, setRating] = useState(existing?.rating ?? 0);
  const [comment, setComment] = useState(existing?.comment ?? "");

  const save = useMutation({
    mutationFn: () =>
      existing
        ? reviewApi.update(existing.reviewId, { rating, comment: comment.trim() || undefined })
        : reviewApi.create(productId, { rating, comment: comment.trim() || undefined }),
    onSuccess: () => {
      toast.success(existing ? "Review updated" : "Thanks for your review!");
      qc.invalidateQueries({ queryKey: ["reviews", productId] });
      qc.invalidateQueries({ queryKey: ["my-reviews"] });
      onDone();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <form
      className="space-y-4 rounded-xl border border-brand-100 bg-brand-50/40 p-5"
      onSubmit={(e) => {
        e.preventDefault();
        if (!rating) return toast.error("Please choose a star rating");
        save.mutate();
      }}
    >
      <div>
        <p className="mb-2 text-sm font-semibold text-slate-900">{existing ? "Edit your review" : "Rate this product"}</p>
        <StarInput value={rating} onChange={setRating} />
      </div>
      <Textarea
        value={comment}
        onChange={(e) => setComment(e.target.value)}
        maxLength={2000}
        placeholder="What did you like or dislike? How was the quality?"
        aria-label="Review"
      />
      <div className="flex items-center justify-between">
        <span className="text-xs text-slate-400">{comment.length}/2000</span>
        <div className="flex gap-2">
          <Button variant="ghost" onClick={onDone}>
            Cancel
          </Button>
          <Button type="submit" loading={save.isPending}>
            {existing ? "Save changes" : "Submit review"}
          </Button>
        </div>
      </div>
    </form>
  );
}

function ReviewItem({ review, mine, onEdit, onDelete }: { review: Review; mine: boolean; onEdit: () => void; onDelete: () => void }) {
  return (
    <article className="border-b border-slate-100 py-5 last:border-0">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="flex h-9 w-9 items-center justify-center rounded-full bg-slate-100 text-sm font-bold text-slate-600">
            {(review.userName || "U").charAt(0).toUpperCase()}
          </span>
          <div>
            <p className="text-sm font-semibold text-slate-900">
              {review.userName || "Customer"} {mine && <span className="ml-1 text-xs font-medium text-brand-600">(you)</span>}
            </p>
            <div className="flex items-center gap-2">
              <Stars value={review.rating} size="xs" />
              <span className="text-xs text-slate-400">{formatDate(review.createdAt)}</span>
            </div>
          </div>
        </div>
        {mine && (
          <div className="flex gap-1">
            <Button variant="ghost" size="icon-sm" onClick={onEdit} aria-label="Edit review">
              <Pencil className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon-sm" onClick={onDelete} aria-label="Delete review" className="hover:text-rose-600">
              <Trash className="h-3.5 w-3.5" />
            </Button>
          </div>
        )}
      </div>
      {review.comment && <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700">{review.comment}</p>}
      {review.sellerReply && (
        <div className="mt-3 rounded-lg border-l-2 border-brand-400 bg-slate-50 px-4 py-3">
          <p className="flex items-center gap-1.5 text-xs font-semibold text-slate-700">
            <Store className="h-3.5 w-3.5 text-brand-600" /> Response from the seller
            {review.sellerRepliedAt && <span className="font-normal text-slate-400">· {formatDate(review.sellerRepliedAt)}</span>}
          </p>
          <p className="mt-1 whitespace-pre-line text-sm text-slate-600">{review.sellerReply}</p>
        </div>
      )}
    </article>
  );
}

export function ReviewsSection({ productId }: { productId: number }) {
  const qc = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const authed = useAuthStore((s) => s.status === "authenticated");
  const goToLogin = useLoginRedirect();
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<"recent" | "top">("recent");
  const [mode, setMode] = useState<"idle" | "write" | "edit">("idle");
  const [deleting, setDeleting] = useState<Review | null>(null);

  const { data: summary, isLoading: summaryLoading } = useReviewSummary(productId);
  const { data, isLoading } = useReviews(productId, {
    pageNumber: page,
    pageSize: 5,
    sortBy: sort === "recent" ? "createdAt" : "rating",
    sortOrder: "desc",
  });

  // The public list hides moderated reviews, so look up the caller's own review via /users/reviews.
  const { data: mine } = useQuery({
    queryKey: ["my-reviews", { pageSize: 100 }],
    queryFn: () => reviewApi.mine({ pageSize: 100, sortBy: "reviewId", sortOrder: "desc" }),
    enabled: authed,
  });
  const myReview = mine?.content.find((r) => r.productId === productId);

  const remove = useMutation({
    mutationFn: (id: number) => reviewApi.remove(id),
    onSuccess: () => {
      toast.success("Review deleted");
      setDeleting(null);
      qc.invalidateQueries({ queryKey: ["reviews", productId] });
      qc.invalidateQueries({ queryKey: ["my-reviews"] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const avg = summary?.averageRating ?? 0;
  const count = summary?.reviewCount ?? 0;

  return (
    <section id="reviews" className="scroll-mt-40 rounded-xl border border-slate-200/80 bg-white shadow-card">
      <div className="grid gap-8 p-6 md:grid-cols-[240px_1fr]">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Ratings & reviews</h2>
          {summaryLoading ? (
            <Skeleton className="mt-4 h-20 w-40" />
          ) : (
            <div className="mt-4">
              <p className="text-5xl font-extrabold tracking-tight text-slate-900">{count ? avg.toFixed(1) : "–"}</p>
              <Stars value={avg} size="md" className="mt-2" />
              <p className="mt-1 text-sm text-slate-500">
                Based on {count.toLocaleString("en-IN")} review{count === 1 ? "" : "s"}
              </p>
            </div>
          )}
          <div className="mt-6">
            {myReview ? (
              <div className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
                You rated this {myReview.rating}★.
                {myReview.hidden && <span className="mt-1 block text-xs text-amber-700">Your review is hidden by a moderator.</span>}
                <Button variant="link" className="mt-1 block" onClick={() => setMode("edit")}>
                  Edit your review
                </Button>
              </div>
            ) : (
              <Button variant="outline" className="w-full" onClick={() => (authed ? setMode("write") : goToLogin())}>
                <MessageSquare className="h-4 w-4" /> Write a review
              </Button>
            )}
          </div>
        </div>

        <div className="min-w-0">
          {mode !== "idle" && (
            <ReviewForm productId={productId} existing={mode === "edit" ? myReview : undefined} onDone={() => setMode("idle")} />
          )}

          <div className="mt-2 flex items-center justify-between">
            <p className="text-sm font-semibold text-slate-700">Customer reviews</p>
            <Segmented
              value={sort}
              onChange={(v) => {
                setSort(v);
                setPage(0);
              }}
              options={[
                { value: "recent", label: "Most recent" },
                { value: "top", label: "Highest rated" },
              ]}
            />
          </div>

          {isLoading ? (
            <div className="space-y-4 py-5">
              {[0, 1].map((i) => (
                <Skeleton key={i} className="h-20 w-full" />
              ))}
            </div>
          ) : !data?.content.length ? (
            <EmptyState icon={<MessageSquare />} title="No reviews yet" description="Be the first to share your thoughts on this product." />
          ) : (
            <>
              {data.content.map((r) => (
                <ReviewItem
                  key={r.reviewId}
                  review={r}
                  mine={!!user && r.userId === user.userId}
                  onEdit={() => setMode("edit")}
                  onDelete={() => setDeleting(r)}
                />
              ))}
              <Pagination className="mt-4" pageNumber={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
            </>
          )}
        </div>
      </div>

      <ConfirmModal
        open={!!deleting}
        onClose={() => setDeleting(null)}
        onConfirm={() => deleting && remove.mutate(deleting.reviewId)}
        loading={remove.isPending}
        title="Delete your review?"
        description="This permanently removes your rating and comment."
        confirmLabel="Delete review"
      />
    </section>
  );
}
