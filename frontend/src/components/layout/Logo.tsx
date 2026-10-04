import { Link } from "react-router";
import { cn } from "@/lib/cn";
import { brand } from "@/config/brand";

export function Logo({ className, inverted, to = "/" }: { className?: string; inverted?: boolean; to?: string }) {
  return (
    <Link to={to} className={cn("inline-flex items-center gap-2", className)} aria-label={`${brand.name} home`}>
      <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 shadow-sm">
        <svg viewBox="0 0 32 32" className="h-5 w-5" aria-hidden>
          <path d="M9 10l7 13 7-13" fill="none" stroke="#fff" strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </span>
      <span className={cn("text-xl font-extrabold tracking-tight", inverted ? "text-white" : "text-slate-900")}>
        {brand.name.toLowerCase()}
        <span className="text-amber-400">.</span>
      </span>
    </Link>
  );
}
