import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { useState } from 'react'

import { env } from '@/app/config/env'
import { createQueryClient } from '@/app/config/query-client'

export function QueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => createQueryClient())

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {env.isDev ? <ReactQueryDevtools buttonPosition="bottom-right" /> : null}
    </QueryClientProvider>
  )
}
