import { useSearchParams } from "react-router";
import { SearchX, SlidersHorizontal } from "lucide-react";
import { cn } from "@/lib/cn";
import { useCategories, useProducts } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { ProductGrid } from "@/components/product/ProductCard";
import { Pagination } from "@/components/ui/Pagination";
import { Select } from "@/components/ui/Field";
import { EmptyState, ErrorState } from "@/components/ui/Feedback";
import { Button } from "@/components/ui/Button";
import type { ProductQuery } from "@/api/types";

const SORTS: Record<string, { label: string; sortBy: string; sortOrder: "asc" | "desc" }> = {
  relevance: { label: "Featured", sortBy: "productId", sortOrder: "asc" },
  newest: { label: "Newest first", sortBy: "createdAt", sortOrder: "desc" },
  "price-asc": { label: "Price: low to high", sortBy: "specialPriceMinorUnits", sortOrder: "asc" },
  "price-desc": { label: "Price: high to low", sortBy: "specialPriceMinorUnits", sortOrder: "desc" },
  discount: { label: "Biggest discount", sortBy: "discount", sortOrder: "desc" },
  name: { label: "Name A–Z", sortBy: "productName", sortOrder: "asc" },
};

const PAGE_SIZE = 20;

export default function ProductsPage() {
  const [params, setParams] = useSearchParams();
  const q = params.get("q") ?? "";
  const category = params.get("category") ?? "";
  const sortKey = params.get("sort") && SORTS[params.get("sort")!] ? params.get("sort")! : "relevance";
  const page = Math.max(0, Number(params.get("page") ?? 0) || 0);
  const sort = SORTS[sortKey];

  useDocumentTitle(q ? `Results for "${q}"` : category || "All products");

  const query: ProductQuery = {
    keyword: q || undefined,
    category: category || undefined,
    pageNumber: page,
    pageSize: PAGE_SIZE,
    sortBy: sort.sortBy,
    sortOrder: sort.sortOrder,
  };
  const { data, isLoading, isFetching, error, refetch } = useProducts(query);
  const { data: categories } = useCategories();

  const update = (patch: Record<string, string | null>) => {
    const next = new URLSearchParams(params);
    for (const [k, v] of Object.entries(patch)) {
      if (v) next.set(k, v);
      else next.delete(k);
    }
    if (!("page" in patch)) next.delete("page");
    setParams(next);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const heading = q ? `Results for “${q}”` : category || (sortKey === "discount" ? "Top deals" : sortKey === "newest" ? "New arrivals" : "All products");

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
      <div className="grid gap-6 lg:grid-cols-[230px_1fr]">
        <aside className="hidden lg:block">
          <div className="sticky top-40 rounded-xl border border-slate-200/80 bg-white p-4 shadow-card">
            <p className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-900">
              <SlidersHorizontal className="h-4 w-4" /> Categories
            </p>
            <div className="space-y-0.5">
              <button
                onClick={() => update({ category: null })}
                className={cn(
                  "block w-full rounded-lg px-3 py-2 text-left text-sm transition-colors",
                  !category ? "bg-brand-50 font-semibold text-brand-700" : "text-slate-600 hover:bg-slate-50",
                )}
              >
                All categories
              </button>
              {categories?.map((c) => (
                <button
                  key={c.categoryId}
                  onClick={() => update({ category: c.categoryName })}
                  className={cn(
                    "block w-full truncate rounded-lg px-3 py-2 text-left text-sm transition-colors",
                    category === c.categoryName ? "bg-brand-50 font-semibold text-brand-700" : "text-slate-600 hover:bg-slate-50",
                  )}
                >
                  {c.categoryName}
                </button>
              ))}
            </div>
          </div>
        </aside>

        <div className="min-w-0">
          <div className="mb-5 flex flex-col gap-3 rounded-xl border border-slate-200/80 bg-white p-4 shadow-card sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h1 className="text-lg font-bold text-slate-900">{heading}</h1>
              <p className="text-sm text-slate-500">
                {isLoading ? "Searching…" : `${(data?.totalElements ?? 0).toLocaleString("en-IN")} products`}
                {isFetching && !isLoading && " · updating"}
              </p>
            </div>
            <div className="flex gap-2">
              <Select
                className="lg:hidden"
                value={category}
                onChange={(e) => update({ category: e.target.value || null })}
                aria-label="Category"
              >
                <option value="">All categories</option>
                {categories?.map((c) => (
                  <option key={c.categoryId} value={c.categoryName}>
                    {c.categoryName}
                  </option>
                ))}
              </Select>
              <Select value={sortKey} onChange={(e) => update({ sort: e.target.value === "relevance" ? null : e.target.value })} aria-label="Sort by" className="sm:w-52">
                {Object.entries(SORTS).map(([k, s]) => (
                  <option key={k} value={k}>
                    Sort: {s.label}
                  </option>
                ))}
              </Select>
            </div>
          </div>

          {(q || category) && (
            <div className="mb-4 flex flex-wrap items-center gap-2">
              {q && (
                <button onClick={() => update({ q: null })} className="rounded-full bg-white px-3 py-1 text-xs font-medium text-slate-700 shadow-card ring-1 ring-slate-200 hover:ring-slate-300">
                  “{q}” ✕
                </button>
              )}
              {category && (
                <button onClick={() => update({ category: null })} className="rounded-full bg-white px-3 py-1 text-xs font-medium text-slate-700 shadow-card ring-1 ring-slate-200 hover:ring-slate-300">
                  {category} ✕
                </button>
              )}
            </div>
          )}

          {error ? (
            <ErrorState error={error} onRetry={() => refetch()} />
          ) : !isLoading && !data?.content.length ? (
            <div className="rounded-xl border border-slate-200/80 bg-white shadow-card">
              <EmptyState
                icon={<SearchX />}
                title="No products found"
                description="Try a different search term or browse another category."
                action={
                  <Button variant="outline" onClick={() => setParams(new URLSearchParams())}>
                    Clear filters
                  </Button>
                }
              />
            </div>
          ) : (
            <ProductGrid products={data?.content} loading={isLoading} className="xl:grid-cols-4" skeletonCount={8} />
          )}

          {data && (
            <Pagination
              className="mt-8"
              pageNumber={data.pageNumber}
              totalPages={data.totalPages}
              onChange={(p) => update({ page: p > 0 ? String(p) : null })}
            />
          )}
        </div>
      </div>
    </div>
  );
}
