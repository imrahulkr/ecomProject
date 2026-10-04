import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { authApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import { useAuthStore } from "@/store/auth";
import { useDocumentTitle } from "@/hooks/useUtils";
import { FormField, Input, PasswordInput } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { Alert } from "@/components/ui/Feedback";
import { safeRedirect } from "@/components/auth/Guards";
import { AuthShell, Divider, OAuthButtons } from "./AuthShell";

const schema = z.object({
  email: z.string().trim().email("Enter a valid email"),
  password: z.string().min(1, "Enter your password"),
});
type Values = z.infer<typeof schema>;

export default function LoginPage() {
  useDocumentTitle("Sign in");
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [params] = useSearchParams();
  const setSession = useAuthStore((s) => s.setFromAuthResponse);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<Values>({ resolver: zodResolver(schema) });

  const login = useMutation({
    mutationFn: authApi.login,
    onSuccess: (res) => {
      setSession(res);
      qc.invalidateQueries();
      toast.success(`Welcome back${res.user.name ? `, ${res.user.name.split(" ")[0]}` : ""}!`);
      navigate(safeRedirect(params.get("redirect")), { replace: true });
    },
    onError: (err) => setError(getErrorMessage(err, "Invalid email or password.")),
  });

  const e = formState.errors;
  const unverified = error?.toLowerCase().includes("not verified");

  return (
    <AuthShell
      title="Sign in to your account"
      subtitle="Welcome back! Please enter your details."
      footer={
        <>
          New to Vendora?{" "}
          <Link to={`/signup${params.get("redirect") ? `?redirect=${encodeURIComponent(params.get("redirect")!)}` : ""}`} className="font-semibold text-brand-700 hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <OAuthButtons />
      <Divider label="or with email" />
      <form onSubmit={handleSubmit((v) => (setError(null), login.mutate(v)))} className="space-y-4">
        {error && (
          <Alert tone={unverified ? "warning" : "danger"} title={unverified ? "Verify your email first" : undefined}>
            {unverified ? "We've sent you a fresh verification link. Open it, then sign in again." : error}
          </Alert>
        )}
        <FormField label="Email" error={e.email?.message}>
          {(id) => <Input id={id} type="email" autoComplete="email" placeholder="you@example.com" invalid={!!e.email} {...register("email")} />}
        </FormField>
        <FormField
          label="Password"
          error={e.password?.message}
          action={
            <Link to="/forgot-password" className="text-xs font-semibold text-brand-700 hover:underline">
              Forgot password?
            </Link>
          }
        >
          {(id) => <PasswordInput id={id} autoComplete="current-password" invalid={!!e.password} {...register("password")} />}
        </FormField>
        <Button type="submit" size="lg" className="w-full" loading={login.isPending}>
          Sign in
        </Button>
      </form>
    </AuthShell>
  );
}
