const RAZORPAY_SRC = 'https://checkout.razorpay.com/v1/checkout.js'

let scriptPromise: Promise<boolean> | null = null

/** Razorpay has no npm package for the browser widget -- it's loaded via this script tag. */
export function loadRazorpayScript(): Promise<boolean> {
  if (document.querySelector(`script[src="${RAZORPAY_SRC}"]`)) {
    return Promise.resolve(true)
  }
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve) => {
      const script = document.createElement('script')
      script.src = RAZORPAY_SRC
      script.onload = () => resolve(true)
      script.onerror = () => resolve(false)
      document.body.appendChild(script)
    })
  }
  return scriptPromise
}
