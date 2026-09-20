import { isApiError, toErrorMessage } from '@/shared/api/errors'
export function OperationError({ error }: { error: unknown }) {
  if (!error) return null
  return (
    <div
      role="alert"
      className="rounded-md border border-destructive/25 bg-destructive/5 p-3 text-sm"
    >
      <p>{toErrorMessage(error)}</p>
      {isApiError(error) && error.traceId && (
        <details className="mt-2 text-xs text-muted-foreground">
          <summary>错误详情</summary>请求编号：{error.traceId}
        </details>
      )}
    </div>
  )
}
