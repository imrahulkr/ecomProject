import { useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { loadRazorpayScript } from '@/lib/loadRazorpayScript'

declare global {
  interface Window {
    Razorpay: new (options: Record<string, unknown>) => { open: () => void }
  }
}

type Props = {
  clientPayload: Record<string, unknown>
  email: string
  onSuccess: () => void
  onError: (message: string) => void
}

export function RazorpayCheckout({ clientPayload, email, onSuccess, onError }: Props) {
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let cancelled = false
    loadRazorpayScript().then((ok) => {
      if (cancelled) return
      if (!ok) onError('Could not load the Razorpay checkout script')
      setReady(ok)
    })
    return () => {
      cancelled = true
    }
  }, [onError])

  function open() {
    const razorpay = new window.Razorpay({
      key: clientPayload.keyId,
      order_id: clientPayload.razorpayOrderId,
      amount: clientPayload.amount,
      currency: clientPayload.currency,
      prefill: { email },
      handler: () => onSuccess(),
      modal: { ondismiss: () => onError('Payment was not completed') },
    })
    razorpay.open()
  }

  return (
    <Button size="lg" disabled={!ready} onClick={open}>
      Pay with Razorpay
    </Button>
  )
}
