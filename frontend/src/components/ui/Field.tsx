import {
  forwardRef,
  useId,
  useState,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from "react";
import { Eye, EyeOff } from "lucide-react";
import { cn } from "@/lib/cn";

const control =
  "w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-900 placeholder:text-slate-400 shadow-xs transition-colors focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15 disabled:bg-slate-50 disabled:text-slate-500 aria-[invalid=true]:border-rose-400 aria-[invalid=true]:focus:ring-rose-500/15";

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }>(
  function Input({ className, invalid, ...rest }, ref) {
    return <input ref={ref} aria-invalid={invalid || undefined} className={cn(control, "h-10", className)} {...rest} />;
  },
);

export const PasswordInput = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }>(
  function PasswordInput({ className, invalid, ...rest }, ref) {
    const [visible, setVisible] = useState(false);
    return (
      <div className="relative">
        <input
          ref={ref}
          type={visible ? "text" : "password"}
          aria-invalid={invalid || undefined}
          className={cn(control, "h-10 pr-10", className)}
          {...rest}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          className="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-slate-400 hover:text-slate-600"
          aria-label={visible ? "Hide password" : "Show password"}
          tabIndex={-1}
        >
          {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>
    );
  },
);

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement> & { invalid?: boolean }>(
  function Textarea({ className, invalid, rows = 4, ...rest }, ref) {
    return (
      <textarea ref={ref} rows={rows} aria-invalid={invalid || undefined} className={cn(control, "py-2.5 leading-relaxed", className)} {...rest} />
    );
  },
);

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement> & { invalid?: boolean }>(
  function Select({ className, invalid, children, ...rest }, ref) {
    return (
      <select
        ref={ref}
        aria-invalid={invalid || undefined}
        className={cn(
          control,
          "h-10 cursor-pointer appearance-none bg-[url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2216%22 height=%2216%22 fill=%22none%22 stroke=%22%2364748b%22 stroke-width=%222%22 viewBox=%220 0 24 24%22><path d=%22m6 9 6 6 6-6%22/></svg>')] bg-[right_0.6rem_center] bg-no-repeat pr-9",
          className,
        )}
        {...rest}
      >
        {children}
      </select>
    );
  },
);

type FormFieldProps = {
  label?: ReactNode;
  error?: string;
  hint?: ReactNode;
  className?: string;
  children: (id: string) => ReactNode;
  action?: ReactNode;
};

export function FormField({ label, error, hint, className, children, action }: FormFieldProps) {
  const id = useId();
  return (
    <div className={cn("space-y-1.5", className)}>
      {(label || action) && (
        <div className="flex items-center justify-between">
          {label && (
            <label htmlFor={id} className="text-sm font-medium text-slate-700">
              {label}
            </label>
          )}
          {action}
        </div>
      )}
      {children(id)}
      {error ? (
        <p className="text-xs font-medium text-rose-600">{error}</p>
      ) : hint ? (
        <p className="text-xs text-slate-500">{hint}</p>
      ) : null}
    </div>
  );
}

export function Checkbox({ label, ...rest }: InputHTMLAttributes<HTMLInputElement> & { label: ReactNode }) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-2 text-sm text-slate-700">
      <input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-brand-600 accent-brand-600" {...rest} />
      {label}
    </label>
  );
}
