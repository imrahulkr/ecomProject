export type JwtClaims = {
  sub?: string;
  email?: string;
  userId?: number;
  roles?: string[];
  providers?: string[];
  enabled?: boolean;
  exp?: number;
};

/** Reads claims for UI decisions only - the backend verifies the signature on every request. */
export function decodeJwt(token: string): JwtClaims {
  try {
    const payload = token.split(".")[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
    const json = decodeURIComponent(
      Array.from(atob(padded))
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join(""),
    );
    return JSON.parse(json) as JwtClaims;
  } catch {
    return {};
  }
}
