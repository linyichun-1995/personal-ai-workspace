import { useQueryClient } from '@tanstack/react-query'
import { createRouter, RouterProvider } from '@tanstack/react-router'
import { useEffect } from 'react'

import { routeTree } from '@/app/router/route-tree'
import { clearAccessToken } from '@/features/auth/api/token-store'
import { setAuthExpiredHandler } from '@/shared/api'
import { queryKeys } from '@/shared/api/query-keys'

const router = createRouter({
  routeTree,
  context: {
    queryClient: undefined!,
  },
  defaultPreload: 'intent',
  scrollRestoration: true,
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

export function AppRouter() {
  const queryClient = useQueryClient()

  useEffect(() => {
    setAuthExpiredHandler(() => {
      clearAccessToken()
      queryClient.setQueryData(queryKeys.auth.session(), null)
      queryClient.removeQueries({ queryKey: queryKeys.auth.all() })
      void router.navigate({ to: '/login' })
    })

    return () => {
      setAuthExpiredHandler(null)
    }
  }, [queryClient])

  return <RouterProvider router={router} context={{ queryClient }} />
}
