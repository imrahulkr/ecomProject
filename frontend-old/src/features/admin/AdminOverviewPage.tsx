import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useAnalytics } from '@/features/admin/analyticsHooks'

export function AdminOverviewPage() {
  const { data, isPending, isError } = useAnalytics()

  if (isPending) {
    return (
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-24 w-full" />
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return <p className="text-sm text-destructive">Couldn't load analytics. Please try again.</p>
  }

  const stats = [
    { label: 'Products', value: data.productCount, description: 'Total listed products' },
    { label: 'Orders', value: data.totalOrders, description: 'Total orders placed' },
    {
      label: 'Revenue',
      value: `₹${data.totalRevenue}`,
      description: 'Total revenue across all orders',
    },
  ]

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      {stats.map((stat) => (
        <Card key={stat.label}>
          <CardHeader>
            <CardTitle>{stat.label}</CardTitle>
            <CardDescription>{stat.description}</CardDescription>
          </CardHeader>
          <CardContent>
            <span className="text-2xl font-semibold">{stat.value}</span>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
