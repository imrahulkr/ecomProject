import { cn } from '@/lib/utils'

export type ProviderName = 'STRIPE' | 'RAZORPAY'

const LABELS: Record<ProviderName, string> = {
  STRIPE: 'Card (Stripe)',
  RAZORPAY: 'Razorpay',
}

type Props = {
  value: ProviderName | undefined
  onChange: (value: ProviderName | undefined) => void
  /** Only the initial checkout allows omitting a provider (backend auto-picks the healthiest). */
  allowAuto?: boolean
  exclude?: ProviderName
}

export function ProviderPicker({ value, onChange, allowAuto, exclude }: Props) {
  const options: ProviderName[] = (['STRIPE', 'RAZORPAY'] as const).filter((p) => p !== exclude)

  return (
    <div className="flex flex-col gap-2" role="radiogroup" aria-label="Payment provider">
      {allowAuto && (
        <button
          type="button"
          role="radio"
          aria-checked={value === undefined}
          onClick={() => onChange(undefined)}
          className={cn(
            'rounded-lg border px-3 py-2 text-left text-sm transition-colors',
            value === undefined
              ? 'border-primary bg-primary/5 font-medium'
              : 'border-border hover:bg-muted'
          )}
        >
          Auto-select (recommended)
        </button>
      )}
      {options.map((provider) => (
        <button
          key={provider}
          type="button"
          role="radio"
          aria-checked={value === provider}
          onClick={() => onChange(provider)}
          className={cn(
            'rounded-lg border px-3 py-2 text-left text-sm transition-colors',
            value === provider
              ? 'border-primary bg-primary/5 font-medium'
              : 'border-border hover:bg-muted'
          )}
        >
          {LABELS[provider]}
        </button>
      ))}
    </div>
  )
}
