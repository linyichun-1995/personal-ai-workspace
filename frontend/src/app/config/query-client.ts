import { MutationCache, QueryClient } from '@tanstack/react-query'

import { DEFAULT_GC_TIME_MS, DEFAULT_STALE_TIME_MS } from '@/app/config/constants'
import { isApiError } from '@/shared/api/errors'
import { handleClientError } from '@/shared/lib/error-handler'

export function createQueryClient(): QueryClient {
  return new QueryClient({
    mutationCache: new MutationCache({
      onError: handleClientError,
    }),
    defaultOptions: {
      queries: {
        staleTime: DEFAULT_STALE_TIME_MS,
        gcTime: DEFAULT_GC_TIME_MS,
        retry: (failureCount, error) => {
          if (isApiError(error) && error.status >= 400 && error.status < 500) {
            return false
          }

          return failureCount < 2
        },
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  })
}
