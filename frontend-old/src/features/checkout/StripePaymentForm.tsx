import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { getStripe } from '@/lib/stripe'

type Props = {
  clientSecret: string
  onSuccess: () => void
  onError: (message: string) => void
}

function InnerForm({ onSuccess, onError }: Omit<Props, 'clientSecret'>) {
  const stripe = useStripe()
  const elements = useElements()
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!stripe || !elements) return

    setSubmitting(true)
    const { error } = await stripe.confirmPayment({ elements, redirect: 'if_required' })
    setSubmitting(false)

    if (error) {
      onError(error.message ?? 'Payment could not be confirmed')
    } else {
      onSuccess()
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <PaymentElement />
      <Button type="submit" size="lg" disabled={!stripe || submitting}>
        {submitting ? 'Confirming...' : 'Pay now'}
      </Button>
    </form>
  )
}

export function StripePaymentForm({ clientSecret, onSuccess, onError }: Props) {
  return (
    <Elements stripe={getStripe()} options={{ clientSecret }}>
      <InnerForm onSuccess={onSuccess} onError={onError} />
    </Elements>
  )
}
