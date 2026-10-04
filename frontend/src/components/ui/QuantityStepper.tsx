import { Minus, Plus, Trash } from "lucide-react";
import { cn } from "@/lib/cn";

export function QuantityStepper({
  value,
  onIncrement,
  onDecrement,
  max,
  disabled,
  size = "md",
  className,
}: {
  value: number;
  onIncrement: () => void;
  onDecrement: () => void;
  max?: number;
  disabled?: boolean;
  size?: "sm" | "md";
  className?: string;
}) {
  const h = size === "sm" ? "h-8" : "h-10";
  const w = size === "sm" ? "w-8" : "w-10";
  return (
    <div className={cn("inline-flex items-center rounded-lg border border-slate-300 bg-white", h, className)}>
      <button
        type="button"
        className={cn("flex h-full items-center justify-center text-slate-600 hover:text-slate-900 disabled:opacity-40", w)}
        onClick={onDecrement}
        disabled={disabled}
        aria-label={value <= 1 ? "Remove" : "Decrease quantity"}
      >
        {value <= 1 ? <Trash className="h-3.5 w-3.5" /> : <Minus className="h-3.5 w-3.5" />}
      </button>
      <span className="min-w-8 text-center text-sm font-semibold tabular-nums text-slate-900">{value}</span>
      <button
        type="button"
        className={cn("flex h-full items-center justify-center text-slate-600 hover:text-slate-900 disabled:opacity-40", w)}
        onClick={onIncrement}
        disabled={disabled || (max != null && value >= max)}
        aria-label="Increase quantity"
      >
        <Plus className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}
