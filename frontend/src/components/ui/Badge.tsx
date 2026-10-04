import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import type { FulfillmentStatus, OrderStatus, SellerApplicationStatus } from "@/api/types";

type Tone = "neutral" | "brand" | "success" | "warning" | "danger" | "info";

const tones: Record<Tone, string> = {
  neutral: "bg-slate-100 text-slate-700 ring-slate-200",
  brand: "bg-brand-50 text-brand-700 ring-brand-200",
  success: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  warning: "bg-amber-50 text-amber-800 ring-amber-200",
  danger: "bg-rose-50 text-rose-700 ring-rose-200",
  info: "bg-sky-50 text-sky-700 ring-sky-200",
};

export function Badge({ tone = "neutral", className, children, dot }: { tone?: Tone; className?: string; children: ReactNode; dot?: boolean }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset",
        tones[tone],
        className,
      )}
    >
      {dot && <span className="h-1.5 w-1.5 rounded-full bg-current" />}
      {children}
    </span>
  );
}

const orderTone: Record<OrderStatus, [Tone, string]> = {
  PENDING_PAYMENT: ["warning", "Awaiting payment"],
  PAID: ["success", "Paid"],
  PAYMENT_FAILED: ["danger", "Payment failed"],
  CANCELLED: ["neutral", "Cancelled"],
};

export function OrderStatusBadge({ status }: { status: OrderStatus | string }) {
  const [tone, label] = orderTone[status as OrderStatus] ?? ["neutral", status];
  return (
    <Badge tone={tone} dot>
      {label}
    </Badge>
  );
}

const fulfillmentTone: Record<FulfillmentStatus, [Tone, string]> = {
  PENDING: ["info", "Processing"],
  SHIPPED: ["brand", "Shipped"],
  DELIVERED: ["success", "Delivered"],
  CANCELLED: ["neutral", "Cancelled"],
  RETURN_REQUESTED: ["warning", "Return requested"],
  RETURNED: ["neutral", "Returned"],
  RETURN_REJECTED: ["danger", "Return rejected"],
};

export function FulfillmentBadge({ status }: { status: FulfillmentStatus }) {
  const [tone, label] = fulfillmentTone[status] ?? ["neutral", status];
  return <Badge tone={tone}>{label}</Badge>;
}

const applicationTone: Record<SellerApplicationStatus, Tone> = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "danger",
};

export function ApplicationStatusBadge({ status }: { status: SellerApplicationStatus }) {
  return <Badge tone={applicationTone[status]}>{status.charAt(0) + status.slice(1).toLowerCase()}</Badge>;
}
