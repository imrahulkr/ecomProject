import type { ReactNode } from "react";
import { Navigate, Outlet, useLocation } from "react-router";
import { ShieldCheck } from "lucide-react";
import { isAdmin, isSeller, useAuthStore } from "@/store/auth";
import { PageLoader, EmptyState } from "@/components/ui/Feedback";
import { ButtonLink } from "@/components/ui/Button";

export function RequireAuth({ children }: { children?: ReactNode }) {
  const status = useAuthStore((s) => s.status);
  const location = useLocation();
  if (status === "loading") return <PageLoader />;
  if (status === "anonymous") {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  }
  return children ?? <Outlet />;
}

export function RequireRole({ role, children }: { role: "seller" | "admin"; children: ReactNode }) {
  const status = useAuthStore((s) => s.status);
  const user = useAuthStore((s) => s.user);
  const location = useLocation();

  if (status === "loading") return <PageLoader label="Checking your access…" />;
  if (status === "anonymous") {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  }
  const allowed = role === "admin" ? isAdmin(user) : isSeller(user);
  if (!allowed) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-canvas p-6">
        <EmptyState
          icon={<ShieldCheck />}
          title={role === "admin" ? "Admins only" : "Seller access required"}
          description={
            role === "admin"
              ? "Your account doesn't have permission to open the admin console."
              : "Apply to become a seller to list products and manage orders."
          }
          action={
            <div className="flex gap-2">
              <ButtonLink to="/" variant="outline">
                Back to store
              </ButtonLink>
              {role === "seller" && <ButtonLink to="/account/seller">Become a seller</ButtonLink>}
            </div>
          }
        />
      </div>
    );
  }
  return <>{children}</>;
}

/** Redirects already-signed-in users away from login/signup. */
export function GuestOnly({ children }: { children: ReactNode }) {
  const status = useAuthStore((s) => s.status);
  const location = useLocation();
  if (status === "loading") return <PageLoader />;
  if (status === "authenticated") {
    const redirect = new URLSearchParams(location.search).get("redirect");
    return <Navigate to={safeRedirect(redirect)} replace />;
  }
  return <>{children}</>;
}

/** Only allow same-app relative paths as post-login destinations. */
export function safeRedirect(target: string | null | undefined, fallback = "/") {
  if (!target || !target.startsWith("/") || target.startsWith("//")) return fallback;
  return target;
}
