import { useState } from "react";
import { Star } from "lucide-react";
import { cn } from "@/lib/cn";

export function Stars({ value, size = "sm", className }: { value: number; size?: "xs" | "sm" | "md"; className?: string }) {
  const px = { xs: "h-3 w-3", sm: "h-4 w-4", md: "h-5 w-5" }[size];
  return (
    <div className={cn("flex items-center gap-0.5", className)} aria-label={`${value.toFixed(1)} out of 5`}>
      {[1, 2, 3, 4, 5].map((i) => {
        const fill = Math.max(0, Math.min(1, value - (i - 1)));
        return (
          <span key={i} className={cn("relative inline-block", px)}>
            <Star className={cn("absolute inset-0 text-slate-300", px)} />
            <span className="absolute inset-0 overflow-hidden" style={{ width: `${fill * 100}%` }}>
              <Star className={cn("fill-amber-400 text-amber-400", px)} />
            </span>
          </span>
        );
      })}
    </div>
  );
}

export function RatingPill({ value, count }: { value: number; count?: number }) {
  return (
    <span className="inline-flex items-center gap-1 text-xs font-semibold">
      <span className="inline-flex items-center gap-0.5 rounded-md bg-emerald-600 px-1.5 py-0.5 text-white">
        {value.toFixed(1)}
        <Star className="h-3 w-3 fill-white" />
      </span>
      {count != null && <span className="font-medium text-slate-500">({count.toLocaleString("en-IN")})</span>}
    </span>
  );
}

const labels = ["", "Poor", "Fair", "Good", "Very good", "Excellent"];

export function StarInput({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  const [hover, setHover] = useState(0);
  const shown = hover || value;
  return (
    <div className="flex items-center gap-3">
      <div className="flex items-center gap-1" onMouseLeave={() => setHover(0)}>
        {[1, 2, 3, 4, 5].map((i) => (
          <button
            key={i}
            type="button"
            onMouseEnter={() => setHover(i)}
            onClick={() => onChange(i)}
            className="rounded transition-transform hover:scale-110"
            aria-label={`${i} star${i > 1 ? "s" : ""}`}
          >
            <Star className={cn("h-7 w-7", i <= shown ? "fill-amber-400 text-amber-400" : "text-slate-300")} />
          </button>
        ))}
      </div>
      <span className="text-sm font-medium text-slate-600">{labels[shown]}</span>
    </div>
  );
}
