import { isAxiosError } from "axios";

type ErrorBody = {
  message?: string;
  error?: string;
  reason?: string;
  errors?: Record<string, string>;
  existingProvider?: string;
};

export function getErrorBody(err: unknown): ErrorBody | null {
  if (!isAxiosError(err)) return null;
  const data = err.response?.data;
  if (data && typeof data === "object") return data as ErrorBody;
  if (typeof data === "string" && data.trim()) return { message: data };
  return null;
}

export function getStatus(err: unknown): number | null {
  return isAxiosError(err) ? (err.response?.status ?? null) : null;
}

export function getErrorCode(err: unknown): string | null {
  return getErrorBody(err)?.error ?? null;
}

/** Both backend error envelopes (APIResponse and the auth map) carry `message`. */
export function getErrorMessage(err: unknown, fallback = "Something went wrong. Please try again.") {
  if (isAxiosError(err) && !err.response) return "Can't reach the server. Check your connection and try again.";
  // 502/503/504 come from the dev proxy or a gateway when the backend itself isn't running.
  const status = getStatus(err);
  if (status === 502 || status === 503 || status === 504) return "The store is temporarily unavailable. Please try again in a moment.";
  const body = getErrorBody(err);
  const fieldErrors = body?.errors ? Object.values(body.errors) : [];
  if (body?.error === "VALIDATION_ERROR" && fieldErrors.length) return fieldErrors.join(" · ");
  return body?.message || body?.reason || (err instanceof Error && !isAxiosError(err) ? err.message : fallback);
}

export function getFieldErrors(err: unknown): Record<string, string> {
  return getErrorBody(err)?.errors ?? {};
}
