import type { ReactNode } from "react";
import { Link } from "react-router";
import { ArrowUpRight } from "lucide-react";
import { cn } from "@/lib/cn";
import { Skeleton } from "@/components/ui/Feedback";

export function StatCard({
  label,
  value,
  icon,
  hint,
  tone = "brand",
  loading,
  to,
}: {
  label: string;
  value: ReactNode;
  icon: ReactNode;
  hint?: ReactNode;
  tone?: "brand" | "emerald" | "amber" | "rose" | "violet" | "sky";
  loading?: boolean;
  /** Makes the whole card a link to the page behind the number. */
  to?: string;
}) {
  const tones = {
    brand: "bg-brand-50 text-brand-600",
    emerald: "bg-emerald-50 text-emerald-600",
    amber: "bg-amber-50 text-amber-600",
    rose: "bg-rose-50 text-rose-600",
    violet: "bg-violet-50 text-violet-600",
    sky: "bg-sky-50 text-sky-600",
  };
  const body = (
    <>
      <div className="flex items-start justify-between">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <span className={cn("flex h-9 w-9 items-center justify-center rounded-lg [&>svg]:h-5 [&>svg]:w-5", tones[tone])}>{icon}</span>
      </div>
      {loading ? <Skeleton className="mt-3 h-8 w-24" /> : <div className="mt-2 text-2xl font-bold tracking-tight text-slate-900">{value}</div>}
      {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
      {to && (
        <ArrowUpRight
          aria-hidden
          className="absolute bottom-4 right-4 h-4 w-4 text-slate-300 transition-colors group-hover:text-brand-600"
        />
      )}
    </>
  );
  const base = "relative rounded-xl border border-slate-200/80 bg-white p-5 shadow-card";
  if (!to) return <div className={base}>{body}</div>;
  return (
    <Link
      to={to}
      aria-label={`${label} - view details`}
      className={cn(
        base,
        "group block transition-all hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-md",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2",
      )}
    >
      {body}
    </Link>
  );
}
