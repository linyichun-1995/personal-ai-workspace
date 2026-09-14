import type { ReactNode } from 'react'
import type { AsyncStatus } from '@/shared/types/common'

import { AlertCircle, Inbox } from 'lucide-react'
import { toErrorMessage } from '@/shared/api/errors'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'

export interface AsyncStateEmptyProps {
  title: string
  description?: string
  action?: ReactNode
}

export interface AsyncStateProps {
  status: Exclude<AsyncStatus, 'idle'>
  error?: unknown
  onRetry?: () => void
  loading?: ReactNode
  empty?: AsyncStateEmptyProps
  children?: ReactNode
}

function DefaultLoading() {
  return (
    <div className="grid gap-3" role="status" aria-live="polite" aria-label="加载中">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-24 w-full" />
      <Skeleton className="h-24 w-full" />
    </div>
  )
}

function DefaultEmpty({ title, description, action }: AsyncStateEmptyProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed px-6 py-16 text-center">
      <Inbox className="size-8 text-muted-foreground" aria-hidden="true" />
      <div className="grid gap-1">
        <p className="font-medium">{title}</p>
        {description
          ? (
              <p className="max-w-md text-sm text-muted-foreground">{description}</p>
            )
          : null}
      </div>
      {action}
    </div>
  )
}

function DefaultError({
  error,
  onRetry,
}: {
  error?: unknown
  onRetry?: () => void
}) {
  return (
    <div
      className="flex flex-col items-center justify-center gap-3 rounded-xl border border-destructive/30 bg-destructive/5 px-6 py-16 text-center"
      role="alert"
    >
      <AlertCircle className="size-8 text-destructive" aria-hidden="true" />
      <div className="grid gap-1">
        <p className="font-medium">加载失败</p>
        <p className="max-w-md text-sm text-muted-foreground">{toErrorMessage(error)}</p>
      </div>
      {onRetry
        ? (
            <Button variant="outline" onClick={onRetry}>
              重试
            </Button>
          )
        : null}
    </div>
  )
}

export function AsyncState({
  status,
  error,
  onRetry,
  loading,
  empty,
  children,
}: AsyncStateProps) {
  if (status === 'loading') {
    return loading ?? <DefaultLoading />
  }

  if (status === 'error') {
    return <DefaultError error={error} onRetry={onRetry} />
  }

  if (status === 'empty') {
    return (
      <DefaultEmpty
        title={empty?.title ?? '暂无数据'}
        description={empty?.description}
        action={empty?.action}
      />
    )
  }

  return children
}
