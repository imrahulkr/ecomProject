import { loadStripe, type Stripe } from "@stripe/stripe-js";
import { env } from "@/config/env";
import { brand } from "@/config/brand";
import type { CheckoutResponse, RazorpayClientPayload } from "@/api/types";

// ---- checkout response hand-off between the checkout page and the payment page ----
const key = (orderId: number) => `vendora:payment:${orderId}`;

export function savePaymentSession(res: CheckoutResponse) {
  try {
    sessionStorage.setItem(key(res.orderId), JSON.stringify(res));
  } catch {
    // storage unavailable - the payment page falls back to retry-payment
  }
}

export function loadPaymentSession(orderId: number): CheckoutResponse | null {
  try {
    const raw = sessionStorage.getItem(key(orderId));
    return raw ? (JSON.parse(raw) as CheckoutResponse) : null;
  } catch {
    return null;
  }
}

export function clearPaymentSession(orderId: number) {
  try {
    sessionStorage.removeItem(key(orderId));
  } catch {
    // ignore
  }
}

// ---- Stripe ----
let stripePromise: Promise<Stripe | null> | null = null;
export function getStripe() {
  if (!env.stripePublishableKey) return null;
  stripePromise ??= loadStripe(env.stripePublishableKey);
  return stripePromise;
}

// ---- Razorpay ----
type RazorpayInstance = { open: () => void; on: (event: string, cb: (resp: unknown) => void) => void };
declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => RazorpayInstance;
  }
}

let razorpayScript: Promise<void> | null = null;
export function loadRazorpay(): Promise<void> {
  if (window.Razorpay) return Promise.resolve();
  razorpayScript ??= new Promise<void>((resolve, reject) => {
    const s = document.createElement("script");
    s.src = "https://checkout.razorpay.com/v1/checkout.js";
    s.async = true;
    s.onload = () => resolve();
    s.onerror = () => {
      razorpayScript = null;
      reject(new Error("Couldn't load Razorpay. Check your connection and try again."));
    };
    document.body.appendChild(s);
  });
  return razorpayScript;
}

export async function openRazorpay(opts: {
  payload: Partial<RazorpayClientPayload>;
  orderId: number;
  email?: string;
  name?: string | null;
  onPaid: () => void;
  onDismiss: () => void;
  onFailed: (reason: string) => void;
}) {
  await loadRazorpay();
  if (!window.Razorpay) throw new Error("Razorpay is unavailable");
  const { keyId, razorpayOrderId, amount, currency } = opts.payload;
  if (!keyId || !razorpayOrderId) throw new Error("The payment session is incomplete. Please try again.");

  const rzp = new window.Razorpay({
    key: keyId,
    order_id: razorpayOrderId,
    amount,
    currency,
    name: brand.name,
    description: `Order #${opts.orderId}`,
    prefill: { email: opts.email, name: opts.name ?? undefined },
    theme: { color: "#2547e8" },
    // Confirmation happens server-side via webhook; the handler only means "payment submitted".
    handler: () => opts.onPaid(),
    modal: { ondismiss: () => opts.onDismiss() },
  });
  rzp.on("payment.failed", (resp) => {
    const reason = (resp as { error?: { description?: string } })?.error?.description;
    opts.onFailed(reason || "The payment was declined.");
  });
  rzp.open();
}
