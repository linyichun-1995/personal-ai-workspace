import { useQuery } from '@tanstack/react-query'

import { sessionQueryOptions } from '@/features/auth/api/session'

export function useSession() {
  return useQuery(sessionQueryOptions)
}
