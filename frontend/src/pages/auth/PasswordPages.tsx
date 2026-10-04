import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { MailCheck } from "lucide-react";
import { toast } from "sonner";
import { authApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import { useDocumentTitle } from "@/hooks/useUtils";
import { FormField, Input, PasswordInput } from "@/components/ui/Field";
import { Button, ButtonLink } from "@/components/ui/Button";
import { Alert, PageLoader } from "@/components/ui/Feedback";
import { AuthShell } from "./AuthShell";

export function ForgotPasswordPage() {
  useDocumentTitle("Reset password");
  const [sent, setSent] = useState(false);
  const { register, handleSubmit, formState } = useForm<{ email: string }>({
    resolver: zodResolver(z.object({ email: z.string().trim().email("Enter a valid email") })),
  });
  const send = useMutation({
    mutationFn: (v: { email: string }) => authApi.forgotPassword(v.email),
    onSuccess: () => setSent(true),
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <AuthShell
      title={sent ? "Check your email" : "Forgot your password?"}
      subtitle={sent ? undefined : "Enter your email and we'll send you a reset link."}
      footer={
        <Link to="/login" className="font-semibold text-brand-700 hover:underline">
          Back to sign in
        </Link>
      }
    >
      {sent ? (
        <div className="text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600">
            <MailCheck className="h-7 w-7" />
          </div>
          <p className="mt-4 text-sm text-slate-600">If an account exists for that email, a reset link is on its way. It expires in 30 minutes.</p>
        </div>
      ) : (
        <form onSubmit={handleSubmit((v) => send.mutate(v))} className="space-y-4">
          <FormField label="Email" error={formState.errors.email?.message}>
            {(id) => <Input id={id} type="email" autoComplete="email" invalid={!!formState.errors.email} {...register("email")} />}
          </FormField>
          <Button type="submit" size="lg" className="w-full" loading={send.isPending}>
            Send reset link
          </Button>
        </form>
      )}
    </AuthShell>
  );
}

const resetSchema = z
  .object({ password: z.string().min(8, "At least 8 characters").max(20, "At most 20 characters"), confirm: z.string() })
  .refine((v) => v.password === v.confirm, { path: ["confirm"], message: "Passwords don't match" });

export function ResetPasswordPage() {
  useDocumentTitle("Choose a new password");
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const navigate = useNavigate();
  const { data: validity, isLoading } = useQuery({
    queryKey: ["reset-token", token],
    queryFn: () => authApi.validateResetToken(token),
    enabled: !!token,
    retry: false,
  });
  const { register, handleSubmit, formState } = useForm<z.infer<typeof resetSchema>>({ resolver: zodResolver(resetSchema) });
  const reset = useMutation({
    mutationFn: (v: z.infer<typeof resetSchema>) => authApi.resetPassword(token, v.password),
    onSuccess: (res) => {
      toast.success(res.message || "Password reset. You can sign in now.");
      navigate("/login", { replace: true });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (token && isLoading) return <PageLoader label="Checking your reset link…" />;
  if (!token || !validity?.valid) {
    return (
      <AuthShell title="This link can't be used">
        <Alert tone="danger">{validity?.reason ?? "The reset link is missing or invalid."}</Alert>
        <ButtonLink to="/forgot-password" className="mt-6 w-full">
          Request a new link
        </ButtonLink>
      </AuthShell>
    );
  }

  const e = formState.errors;
  return (
    <AuthShell title="Choose a new password" subtitle="Make it something you haven't used before.">
      <form onSubmit={handleSubmit((v) => reset.mutate(v))} className="space-y-4">
        <FormField label="New password" error={e.password?.message} hint="8–20 characters">
          {(id) => <PasswordInput id={id} autoComplete="new-password" invalid={!!e.password} {...register("password")} />}
        </FormField>
        <FormField label="Confirm password" error={e.confirm?.message}>
          {(id) => <PasswordInput id={id} autoComplete="new-password" invalid={!!e.confirm} {...register("confirm")} />}
        </FormField>
        <Button type="submit" size="lg" className="w-full" loading={reset.isPending}>
          Reset password
        </Button>
      </form>
    </AuthShell>
  );
}
