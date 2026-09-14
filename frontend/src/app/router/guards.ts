import type { RouterContext } from '@/app/router/context'

import { redirect } from '@tanstack/react-router'
import { AUTH_REDIRECT_SEARCH_KEY } from '@/app/config/constants'
import { queryKeys } from '@/shared/api/query-keys'

export async function requireAuth(options: {
  context: RouterContext
  location: { href: string }
}): Promise<void> {
  void options.context
  void queryKeys
  void AUTH_REDIRECT_SEARCH_KEY
  void redirect

  // Session is a server state and must live in TanStack Query.
  // After /auth/session is available:
  // const session = await options.context.queryClient.ensureQueryData(sessionQueryOptions)
  // if (!session) throw redirect({ to: '/login', search: { [AUTH_REDIRECT_SEARCH_KEY]: options.location.href } })
}

export async function requireGuest(): Promise<void> {
  // After session exists, redirect authenticated users away from /login.
}
