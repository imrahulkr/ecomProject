import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { useQueryClient } from "@tanstack/react-query";
import { CircleCheck, CircleX } from "lucide-react";
import { toast } from "sonner";
import { authApi } from "@/api/endpoints";
import { refreshSession } from "@/api/client";
import { getErrorMessage, getStatus } from "@/api/errors";
import { useAuthStore } from "@/store/auth";
import { useDocumentTitle } from "@/hooks/useUtils";
import { ButtonLink } from "@/components/ui/Button";
import { Alert, PageLoader } from "@/components/ui/Feedback";
import { AuthShell } from "./AuthShell";

/** /oauth/callback?code=… - the code is single-use, so guard against StrictMode's double effect. */
export function OAuthCallbackPage() {
  useDocumentTitle("Signing you in");
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const setFromAccessToken = useAuthStore((s) => s.setFromAccessToken);
  const started = useRef(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const code = params.get("code");
    if (!code) {
      setError("The sign-in link is missing its code.");
      return;
    }
    authApi
      .exchangeOAuthCode(code)
      .then(async ({ accessToken }) => {
        setFromAccessToken(accessToken);
        // Pull the full profile (name, linked providers) that the exchange response omits.
        await refreshSession();
        qc.invalidateQueries();
        toast.success("Signed in");
        navigate("/", { replace: true });
      })
      .catch((err) => setError(getErrorMessage(err, "This sign-in link has expired. Please try again.")));
  }, [params, navigate, qc, setFromAccessToken]);

  if (!error) return <PageLoader label="Signing you in…" />;
  return (
    <AuthShell title="We couldn't sign you in">
      <Alert tone="danger">{error}</Alert>
      <ButtonLink to="/login" className="mt-6 w-full">
        Back to sign in
      </ButtonLink>
    </AuthShell>
  );
}

export function OAuthErrorPage() {
  useDocumentTitle("Sign-in problem");
  const [params] = useSearchParams();
  const linking = params.get("reason") === "account_linking_required";
  return (
    <AuthShell title={linking ? "Sign in with your password first" : "Social sign-in failed"}>
      <Alert tone="warning">
        {linking
          ? "An account with this email already exists. Sign in with your email and password; your social account will connect automatically once the provider verifies your email."
          : "Something went wrong while signing in with your provider. Please try again or use your email and password."}
      </Alert>
      <ButtonLink to="/login" className="mt-6 w-full">
        Go to sign in
      </ButtonLink>
    </AuthShell>
  );
}

/** /verify-email?token=… */
export function VerifyEmailPage() {
  useDocumentTitle("Verify email");
  const [params] = useSearchParams();
  const token = params.get("token");
  const started = useRef(false);
  const [state, setState] = useState<{ status: "loading" | "ok" | "error"; message?: string }>({ status: "loading" });

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    if (!token) {
      setState({ status: "error", message: "The verification link is missing its token." });
      return;
    }
    authApi
      .verifyEmail(token)
      .then(() => setState({ status: "ok" }))
      .catch((err) =>
        setState({
          status: "error",
          message:
            getStatus(err) === 408
              ? "This verification link has expired. Sign in to receive a new one."
              : getStatus(err) === 404
                ? "This link is invalid or was already used. If your account is verified, just sign in."
                : getErrorMessage(err),
        }),
      );
  }, [token]);

  if (state.status === "loading") return <PageLoader label="Verifying your email…" />;
  return (
    <AuthShell title={state.status === "ok" ? "Email verified" : "Verification failed"}>
      <div className="text-center">
        <div
          className={`mx-auto flex h-14 w-14 items-center justify-center rounded-2xl ${state.status === "ok" ? "bg-emerald-50 text-emerald-600" : "bg-rose-50 text-rose-600"}`}
        >
          {state.status === "ok" ? <CircleCheck className="h-7 w-7" /> : <CircleX className="h-7 w-7" />}
        </div>
        <p className="mt-4 text-sm text-slate-600">
          {state.status === "ok" ? "Your account is active. Sign in to start shopping." : state.message}
        </p>
        <ButtonLink to="/login" className="mt-6">
          Sign in
        </ButtonLink>
      </div>
    </AuthShell>
  );
}
