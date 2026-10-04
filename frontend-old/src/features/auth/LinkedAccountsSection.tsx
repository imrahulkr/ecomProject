import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useLinkedAccounts, useUnlinkProvider } from '@/features/auth/hooks'

function providerLabel(provider: string) {
  return provider.charAt(0) + provider.slice(1).toLowerCase()
}

export function LinkedAccountsSection() {
  const { data, isPending, isError } = useLinkedAccounts()
  const unlinkProvider = useUnlinkProvider()

  if (isPending) return <Skeleton className="h-16 w-full" />
  if (isError || !data) {
    return <p className="text-sm text-destructive">Couldn't load linked accounts.</p>
  }

  const wouldLoseLastLoginMethod = !data.hasPassword && data.linkedProviders.length <= 1

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2 text-sm">
        <span className="text-muted-foreground">Password:</span>
        <Badge variant={data.hasPassword ? 'default' : 'outline'}>
          {data.hasPassword ? 'Set' : 'Not set'}
        </Badge>
      </div>

      {data.linkedProviders.length === 0 ? (
        <p className="text-sm text-muted-foreground">No social accounts linked.</p>
      ) : (
        <div className="flex flex-col gap-2">
          {data.linkedProviders.map((provider) => (
            <div
              key={provider}
              className="flex items-center justify-between rounded-xl border border-border p-3"
            >
              <span className="text-sm font-medium">{providerLabel(provider)}</span>
              <Button
                variant="ghost"
                size="sm"
                className="text-destructive"
                disabled={unlinkProvider.isPending || wouldLoseLastLoginMethod}
                title={
                  wouldLoseLastLoginMethod
                    ? 'Set a password first, or link another provider, before unlinking this one'
                    : undefined
                }
                onClick={() => unlinkProvider.mutate(provider)}
              >
                Unlink
              </Button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
