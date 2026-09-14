import type { Session } from '@/features/auth/types'
import { api } from '@/shared/api'
import { queryKeys } from '@/shared/api/query-keys'

export const sessionQueryOptions = {
  queryKey: queryKeys.auth.session(),
  queryFn: getSession,
}

export async function getSession(): Promise<Session | null> {
  return api.get<Session | null>('/auth/session')
}
