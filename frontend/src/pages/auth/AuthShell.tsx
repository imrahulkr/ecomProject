import type { ReactNode } from "react";
import { CircleCheck } from "lucide-react";
import { brand } from "@/config/brand";
import { env } from "@/config/env";
import { Logo } from "@/components/layout/Logo";

export function AuthShell({ title, subtitle, children, footer }: { title: string; subtitle?: ReactNode; children: ReactNode; footer?: ReactNode }) {
  return (
    <div className="mx-auto flex max-w-5xl items-center px-4 py-10 sm:px-6 lg:py-16">
      <div className="grid w-full overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-pop lg:grid-cols-[1fr_1.1fr]">
        <div className="relative hidden flex-col justify-between overflow-hidden bg-linear-to-br from-brand-700 to-brand-500 p-10 text-white lg:flex">
          <div className="absolute -right-20 -top-20 h-72 w-72 rounded-full bg-white/10 blur-2xl" />
          <Logo inverted className="relative" />
          <div className="relative">
            <h2 className="text-2xl font-bold leading-snug">{brand.tagline}.</h2>
            <ul className="mt-6 space-y-3 text-sm text-brand-100">
              {["Thousands of products from verified sellers", "Secure payments with Stripe & Razorpay", "Track every order, end to end"].map((t) => (
                <li key={t} className="flex items-center gap-2">
                  <CircleCheck className="h-4 w-4 text-amber-300" /> {t}
                </li>
              ))}
            </ul>
          </div>
          <p className="relative text-xs text-brand-200">© {new Date().getFullYear()} {brand.name}</p>
        </div>
        <div className="p-6 sm:p-10">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
          {subtitle && <p className="mt-1.5 text-sm text-slate-500">{subtitle}</p>}
          <div className="mt-7">{children}</div>
          {footer && <div className="mt-6 text-center text-sm text-slate-600">{footer}</div>}
        </div>
      </div>
    </div>
  );
}

function GoogleIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden>
      <path fill="#4285F4" d="M23.5 12.3c0-.8-.1-1.6-.2-2.3H12v4.4h6.5a5.6 5.6 0 0 1-2.4 3.6v3h3.9c2.3-2.1 3.5-5.2 3.5-8.7z" />
      <path fill="#34A853" d="M12 24c3.2 0 6-1.1 8-2.9l-3.9-3c-1.1.7-2.5 1.2-4.1 1.2-3.1 0-5.8-2.1-6.7-5H1.3v3.1A12 12 0 0 0 12 24z" />
      <path fill="#FBBC05" d="M5.3 14.3a7.2 7.2 0 0 1 0-4.6V6.6h-4a12 12 0 0 0 0 10.8l4-3.1z" />
      <path fill="#EA4335" d="M12 4.8c1.8 0 3.3.6 4.6 1.8l3.4-3.4A12 12 0 0 0 1.3 6.6l4 3.1c.9-2.9 3.6-4.9 6.7-4.9z" />
    </svg>
  );
}

function GithubIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor" aria-hidden>
      <path d="M12 .5a11.5 11.5 0 0 0-3.6 22.4c.6.1.8-.3.8-.6v-2c-3.2.7-3.9-1.5-3.9-1.5-.5-1.3-1.3-1.7-1.3-1.7-1-.7.1-.7.1-.7 1.2.1 1.8 1.2 1.8 1.2 1 1.8 2.8 1.3 3.5 1 .1-.8.4-1.3.7-1.6-2.6-.3-5.3-1.3-5.3-5.7 0-1.3.5-2.3 1.2-3.1-.1-.3-.5-1.5.1-3.1 0 0 1-.3 3.2 1.2a11 11 0 0 1 5.8 0c2.2-1.5 3.2-1.2 3.2-1.2.6 1.6.2 2.8.1 3.1.8.8 1.2 1.8 1.2 3.1 0 4.4-2.7 5.4-5.3 5.7.4.4.8 1.1.8 2.2v3.3c0 .3.2.7.8.6A11.5 11.5 0 0 0 12 .5z" />
    </svg>
  );
}

/** Full-page navigation to the backend - its OAuth redirect URI is registered against the backend origin. */
export function OAuthButtons() {
  const btn =
    "flex h-11 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50";
  return (
    <div className="grid grid-cols-2 gap-3">
      <a href={`${env.backendOrigin}/oauth2/authorization/google`} className={btn}>
        <GoogleIcon /> Google
      </a>
      <a href={`${env.backendOrigin}/oauth2/authorization/github`} className={btn}>
        <GithubIcon /> GitHub
      </a>
    </div>
  );
}

export function Divider({ label }: { label: string }) {
  return (
    <div className="my-6 flex items-center gap-3 text-xs font-medium uppercase tracking-wider text-slate-400">
      <span className="h-px flex-1 bg-slate-200" />
      {label}
      <span className="h-px flex-1 bg-slate-200" />
    </div>
  );
}
