import type { RouterContext } from '@/app/router/context'

import { redirect } from '@tanstack/react-router'
import { AUTH_REDIRECT_SEARCH_KEY } from '@/app/config/constants'
import { sessionQueryOptions } from '@/features/auth/api/session'

export async function requireAuth(options: {
  context: RouterContext
  location: { href: string }
}): Promise<void> {
  const session = await options.context.queryClient.ensureQueryData(sessionQueryOptions)
  if (!session) {
    throw redirect({
      to: '/login',
      search: { [AUTH_REDIRECT_SEARCH_KEY]: options.location.href },
    })
  }
}

export async function requireGuest(options: {
  context: RouterContext
}): Promise<void> {
  const session = await options.context.queryClient.ensureQueryData(sessionQueryOptions)
  if (session) {
    throw redirect({ to: '/app/dashboard' })
  }
}
