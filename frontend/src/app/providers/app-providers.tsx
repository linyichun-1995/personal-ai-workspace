import type { ReactNode } from 'react'

import { QueryProvider } from '@/app/providers/query-provider'
import { ThemeProvider } from '@/app/providers/theme-provider'
import { Toaster } from '@/shared/components/ui/sonner'
import { TooltipProvider } from '@/shared/components/ui/tooltip'

export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider>
      <QueryProvider>
        <TooltipProvider>
          {children}
          <Toaster position="top-right" />
        </TooltipProvider>
      </QueryProvider>
    </ThemeProvider>
  )
}
