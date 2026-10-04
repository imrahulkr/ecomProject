import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/cn";

/** pageNumber is 0-based, matching the backend. */
export function Pagination({
  pageNumber,
  totalPages,
  onChange,
  className,
}: {
  pageNumber: number;
  totalPages: number;
  onChange: (page: number) => void;
  className?: string;
}) {
  if (totalPages <= 1) return null;

  const pages: (number | "…")[] = [];
  const add = (p: number) => pages.push(p);
  const window = 1;
  for (let p = 0; p < totalPages; p++) {
    if (p === 0 || p === totalPages - 1 || Math.abs(p - pageNumber) <= window) add(p);
    else if (pages[pages.length - 1] !== "…") pages.push("…");
  }

  const btn = "flex h-9 min-w-9 items-center justify-center rounded-lg px-2 text-sm font-medium transition-colors";

  return (
    <nav className={cn("flex items-center justify-center gap-1", className)} aria-label="Pagination">
      <button
        className={cn(btn, "text-slate-600 hover:bg-white disabled:opacity-40")}
        disabled={pageNumber === 0}
        onClick={() => onChange(pageNumber - 1)}
        aria-label="Previous page"
      >
        <ChevronLeft className="h-4 w-4" />
      </button>
      {pages.map((p, i) =>
        p === "…" ? (
          <span key={`gap-${i}`} className="px-1 text-slate-400">
            …
          </span>
        ) : (
          <button
            key={p}
            onClick={() => onChange(p)}
            aria-current={p === pageNumber ? "page" : undefined}
            className={cn(
              btn,
              p === pageNumber ? "bg-brand-600 text-white shadow-sm" : "text-slate-700 hover:bg-white hover:shadow-card",
            )}
          >
            {p + 1}
          </button>
        ),
      )}
      <button
        className={cn(btn, "text-slate-600 hover:bg-white disabled:opacity-40")}
        disabled={pageNumber >= totalPages - 1}
        onClick={() => onChange(pageNumber + 1)}
        aria-label="Next page"
      >
        <ChevronRight className="h-4 w-4" />
      </button>
    </nav>
  );
}
