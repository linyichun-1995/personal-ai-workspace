import type { UseQueryResult } from '@tanstack/react-query'
import type { ReactNode } from 'react'

import type { AsyncStateEmptyProps } from '@/shared/components/async-state'
import { AsyncState } from '@/shared/components/async-state'

export interface QueryStateProps<TData> {
  query: UseQueryResult<TData>
  isEmpty?: (data: TData) => boolean
  empty?: AsyncStateEmptyProps
  loading?: ReactNode
  children: (data: TData) => ReactNode
}

export function QueryState<TData>({
  query,
  isEmpty,
  empty,
  loading,
  children,
}: QueryStateProps<TData>) {
  if (query.isPending) {
    return (
      <AsyncState status="loading" loading={loading}>
        {null}
      </AsyncState>
    )
  }

  if (query.isError) {
    return (
      <AsyncState status="error" error={query.error} onRetry={() => void query.refetch()}>
        {null}
      </AsyncState>
    )
  }

  if (query.data === undefined || isEmpty?.(query.data)) {
    return (
      <AsyncState status="empty" empty={empty}>
        {null}
      </AsyncState>
    )
  }

  return children(query.data)
}
