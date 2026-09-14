import { Outlet } from '@tanstack/react-router'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'

import { env } from '@/app/config/env'

export function RootLayout() {
  return (
    <>
      <div id="root-content" className="min-h-dvh">
        <Outlet />
      </div>
      {env.isDev ? <TanStackRouterDevtools position="bottom-left" /> : null}
    </>
  )
}
