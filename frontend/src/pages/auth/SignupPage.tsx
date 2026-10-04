import { useState } from "react";
import { Link } from "react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { MailCheck } from "lucide-react";
import { authApi } from "@/api/endpoints";
import { getErrorBody, getErrorMessage, getFieldErrors } from "@/api/errors";
import { titleCase } from "@/lib/format";
import { useDocumentTitle } from "@/hooks/useUtils";
import { FormField, Input, PasswordInput } from "@/components/ui/Field";
import { Button, ButtonLink } from "@/components/ui/Button";
import { Alert } from "@/components/ui/Feedback";
import { AuthShell, Divider, OAuthButtons } from "./AuthShell";

// Mirrors SignupRequest: username 3–20, password 8–20.
const schema = z.object({
  name: z.string().trim().max(80).optional(),
  username: z
    .string()
    .trim()
    .min(3, "At least 3 characters")
    .max(20, "At most 20 characters")
    .regex(/^[a-zA-Z0-9._-]+$/, "Letters, numbers, dots, dashes and underscores only"),
  email: z.string().trim().email("Enter a valid email"),
  password: z.string().min(8, "At least 8 characters").max(20, "At most 20 characters"),
});
type Values = z.infer<typeof schema>;

export default function SignupPage() {
  useDocumentTitle("Create account");
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError: setFieldError } = useForm<Values>({ resolver: zodResolver(schema) });

  const signup = useMutation({
    mutationFn: (v: Values) => authApi.signup({ ...v, name: v.name || undefined }),
    onSuccess: (_, v) => setSentTo(v.email),
    onError: (err) => {
      const body = getErrorBody(err);
      const fields = getFieldErrors(err);
      Object.entries(fields).forEach(([k, msg]) => setFieldError(k as keyof Values, { message: msg }));
      if (body?.error === "PASSWORD_SIGNUP_BLOCKED" && body.existingProvider) {
        setError(`This email already uses ${titleCase(body.existingProvider)} sign-in. Continue with ${titleCase(body.existingProvider)} instead.`);
      } else if (!Object.keys(fields).length) {
        setError(getErrorMessage(err));
      }
    },
  });

  if (sentTo) {
    return (
      <AuthShell title="Check your inbox">
        <div className="text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600">
            <MailCheck className="h-7 w-7" />
          </div>
          <p className="mt-4 text-sm leading-relaxed text-slate-600">
            We sent a verification link to <span className="font-semibold text-slate-900">{sentTo}</span>. Open it to activate your account, then sign in.
          </p>
          <ButtonLink to="/login" className="mt-6">
            Go to sign in
          </ButtonLink>
        </div>
      </AuthShell>
    );
  }

  const e = formState.errors;
  return (
    <AuthShell
      title="Create your account"
      subtitle="Join thousands of shoppers and sellers."
      footer={
        <>
          Already have an account?{" "}
          <Link to="/login" className="font-semibold text-brand-700 hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <OAuthButtons />
      <Divider label="or with email" />
      <form onSubmit={handleSubmit((v) => (setError(null), signup.mutate(v)))} className="space-y-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Full name" error={e.name?.message}>
            {(id) => <Input id={id} autoComplete="name" placeholder="Priya Sharma" {...register("name")} />}
          </FormField>
          <FormField label="Username" error={e.username?.message}>
            {(id) => <Input id={id} autoComplete="username" placeholder="priya_s" invalid={!!e.username} {...register("username")} />}
          </FormField>
        </div>
        <FormField label="Email" error={e.email?.message}>
          {(id) => <Input id={id} type="email" autoComplete="email" placeholder="you@example.com" invalid={!!e.email} {...register("email")} />}
        </FormField>
        <FormField label="Password" error={e.password?.message} hint="8–20 characters">
          {(id) => <PasswordInput id={id} autoComplete="new-password" invalid={!!e.password} {...register("password")} />}
        </FormField>
        <Button type="submit" size="lg" className="w-full" loading={signup.isPending}>
          Create account
        </Button>
        <p className="text-center text-xs text-slate-500">By continuing you agree to our Terms and Privacy Policy.</p>
      </form>
    </AuthShell>
  );
}
