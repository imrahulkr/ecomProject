export function formatMoney(minorUnits: number, currency: string) {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(minorUnits / 100)
}
