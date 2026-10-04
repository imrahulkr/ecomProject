import { cn } from "@/lib/cn";
import { formatMoney } from "@/lib/format";

export function Price({
  price,
  special,
  discount,
  currency,
  size = "md",
  className,
}: {
  price: number;
  special: number;
  discount?: number;
  currency: string;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const hasDiscount = special < price && (discount ?? 0) > 0;
  const main = { sm: "text-base", md: "text-lg", lg: "text-3xl" }[size];
  const sub = { sm: "text-xs", md: "text-sm", lg: "text-base" }[size];
  return (
    <div className={cn("flex flex-wrap items-baseline gap-x-2 gap-y-0.5", className)}>
      <span className={cn("font-bold tracking-tight text-slate-900", main)}>{formatMoney(special, currency)}</span>
      {hasDiscount && (
        <>
          <span className={cn("text-slate-400 line-through", sub)}>{formatMoney(price, currency)}</span>
          <span className={cn("font-semibold text-emerald-600", sub)}>{Math.round(discount!)}% off</span>
        </>
      )}
    </div>
  );
}
