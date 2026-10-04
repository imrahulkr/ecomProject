import { env } from "@/config/env";

const moneyFormatters = new Map<string, Intl.NumberFormat>();

/**
 * Backend amounts are integer minor units (paise for INR); assumes a 2-decimal currency.
 * Whole amounts drop the decimals (₹1,299); anything else always shows two (₹2,099.40).
 */
export function formatMoney(minorUnits: number | null | undefined, currency = "INR") {
  const code = (currency || "INR").toUpperCase();
  const minor = Math.round(minorUnits ?? 0);
  const digits = minor % 100 === 0 ? 0 : 2;
  const cacheKey = `${code}:${digits}`;
  let fmt = moneyFormatters.get(cacheKey);
  if (!fmt) {
    fmt = new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: code,
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    });
    moneyFormatters.set(cacheKey, fmt);
  }
  return fmt.format(minor / 100);
}

export function toMinorUnits(major: number | string) {
  const n = typeof major === "string" ? Number.parseFloat(major) : major;
  return Number.isFinite(n) ? Math.round(n * 100) : 0;
}

export function toMajorUnits(minor: number | null | undefined) {
  return (minor ?? 0) / 100;
}

const dateFmt = new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", year: "numeric" });
const dateTimeFmt = new Intl.DateTimeFormat("en-IN", {
  day: "numeric",
  month: "short",
  year: "numeric",
  hour: "numeric",
  minute: "2-digit",
});

function parseDate(value: string | null | undefined) {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

export function formatDate(value: string | null | undefined) {
  const d = parseDate(value);
  return d ? dateFmt.format(d) : "—";
}

export function formatDateTime(value: string | null | undefined) {
  const d = parseDate(value);
  return d ? dateTimeFmt.format(d) : "—";
}

/** Product/cart/order images arrive either as full URLs or bare filenames depending on endpoint. */
export function resolveImageUrl(image: string | null | undefined) {
  if (!image) return null;
  if (/^(https?:)?\/\//.test(image) || image.startsWith("data:") || image.startsWith("blob:")) return image;
  return `${env.imageBaseUrl}/${image.replace(/^\/+/, "")}`;
}

export function pluralize(count: number, singular: string, plural = `${singular}s`) {
  return `${count} ${count === 1 ? singular : plural}`;
}

export function titleCase(value: string | null | undefined) {
  if (!value) return "";
  return value
    .toLowerCase()
    .split(/[_\s]+/)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}
