import type { ErrorComponentProps } from '@tanstack/react-router'
import { Link } from '@tanstack/react-router'

import { AsyncState } from '@/shared/components/async-state'
import { Button } from '@/shared/components/ui/button'

export function RouterPending() {
  return (
    <div className="p-6">
      <AsyncState status="loading">{null}</AsyncState>
    </div>
  )
}

export function RouterError({ error, reset }: ErrorComponentProps) {
  return (
    <div className="p-6">
      <AsyncState status="error" error={error} onRetry={reset}>
        {null}
      </AsyncState>
    </div>
  )
}

export function RouterNotFound() {
  return (
    <div className="mx-auto grid min-h-dvh max-w-md place-content-center gap-4 p-6 text-center">
      <h1 className="text-2xl font-semibold">页面不存在</h1>
      <p className="text-sm text-muted-foreground">当前路径没有匹配的路由。</p>
      <Button asChild>
        <Link to="/app/dashboard">回到概览</Link>
      </Button>
    </div>
  )
}
